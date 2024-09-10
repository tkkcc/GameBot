#![feature(trait_upcasting)]
mod asset;
mod find_trait;
mod mail;
mod node;
mod t1;
mod ui;

use core::{error, f64, panic};
use std::backtrace::Backtrace;
use std::cell::Cell;
use std::i32;
use std::sync::atomic::AtomicU32;
use std::sync::{Arc, RwLock};
use std::{
    sync::{
        atomic::AtomicBool,
        mpsc::{channel, Receiver, Sender, SyncSender},
        LazyLock, OnceLock,
    },
    time::Duration,
};

use anyhow::{Error, Result};

use image::{GenericImageView, ImageReader};
use jni::objects::JByteBuffer;
// use erased_serde::Serialize;
// use git2::{CertificateCheckStatus, RemoteCallbacks};
use jni::{
    objects::{GlobalRef, JByteArray, JClass, JObject, JObjectArray},
    JNIEnv, JavaVM,
};
use linux_futex::{Futex, Private};
use mail::{
    ColorPoint, ColorPointGroup, ColorPointGroupIn, ColorPointIn, ImageIn, IntoMilliseconds,
    IntoSeconds, Point, Tolerance,
};
use node::Node;
use serde::{Deserialize, Serialize};
use ui::CallbackValue;
use ui::Element;

pub use log::error;

pub fn add(left: usize, right: usize) -> usize {
    left + right
}

#[macro_export]
macro_rules! log {
    // NOTE: We cannot use `concat!` to make a static string as a format argument
    // of `eprintln!` because `file!` could contain a `{` or
    // `$val` expression could be a block (`{ .. }`), in which case the `eprintln!`
    // will be malformed.
    () => {
        $crate::error!("[{}:{}:{}]", $crate::file!(), $crate::line!(), $crate::column!())
    };
    ($val:expr $(,)?) => {
        // Use of `match` here is intentional because it affects the lifetimes
        // of temporaries - https://stackoverflow.com/a/48732525/1063961
        match $val {
            tmp => {
                $crate::error!("[{}:{}:{}] {} = {:#?}",
                    file!(), line!(), column!(), stringify!($val), &tmp);
                tmp
            }
        }
    };
    ($($val:expr),+ $(,)?) => {
        ($($crate::log!($val)),+,)
    };
}

extern crate android_logger;

#[derive(Serialize, Deserialize, Debug)]
struct CallbackMsg {
    id: usize,
    value: Box<dyn CallbackValue>,
}

static CELL: OnceLock<GlobalRef> = OnceLock::new();
fn get_object() -> &'static JObject<'static> {
    CELL.get().unwrap().as_obj()
}

static CELL2: OnceLock<JavaVM> = OnceLock::new();
fn get_env() -> JNIEnv<'static> {
    let vm = CELL2.get().unwrap();
    vm.attach_current_thread_permanently().unwrap()
}

#[derive(Default)]
struct ScreenNode {
    info: Vec<Node>,
    info_ref: Option<GlobalRef>,
}

#[derive(Default, Debug)]
pub struct Screenshot<'a> {
    pub width: u32,
    pub height: u32,
    // pub pixel_stride: i32,
    // pub row_stride: i32,
    pub data: &'a [u8],
    // pub data: Vec<u8>,
}

impl<'a> Screenshot<'a> {
    fn find_color_point(
        &self,
        &ColorPoint {
            x,
            y,
            red,
            blue,
            green,
        }: &ColorPoint,
    ) -> Option<Point> {
        if x >= self.width || y >= self.height {
            return None;
        }
        let i = ((y * self.width + x) * 4) as usize;
        if self.data[i].abs_diff(red) > 0
            || self.data[i + 1].abs_diff(green) > 0
            || self.data[i + 2].abs_diff(blue) > 0
        {
            return None;
        }
        Some(Point { x, y })
    }

    fn find_color_point_group(&self, cpg: &ColorPointGroup) -> Option<Point> {
        if cpg.group.is_empty() {
            return None;
        }
        let tolerance = (cpg.tolerance * 255.0) as u8;
        for cp in &cpg.group {
            if cp.x >= self.width || cp.y >= self.height {
                return None;
            }
            let i = ((cp.y * self.width + cp.x) * 4) as usize;
            if self.data[i].abs_diff(cp.red) > tolerance
                || self.data[i + 1].abs_diff(cp.green) > tolerance
                || self.data[i + 2].abs_diff(cp.blue) > tolerance
            {
                return None;
            }
        }
        Some((&cpg.group[0]).into())
    }

    fn find_color_point_group_in(&self, cpg: &ColorPointGroupIn) -> Option<Point> {
        self.find_all_color_point_group_in(cpg, 1).first().cloned()
    }

    fn find_all_color_point_group_in(&self, cpg: &ColorPointGroupIn, max_num: usize) -> Vec<Point> {
        // we visit all valid localtion: iter on delta x and y, move via color point x + delta x
        let mut ans = vec![];
        if cpg.group.is_empty() {
            return ans;
        }
        let region = &cpg.region;

        // region is not in screenshot
        // or region's area is zero
        if region.right >= self.width
            || region.bottom >= self.height
            || region.left >= region.right
            || region.top >= region.bottom
        {
            return ans;
        }

        let mut t = u32::MAX;
        let mut l = u32::MAX;
        let mut b = 0;
        let mut r = 0;
        for cp in &cpg.group {
            l = l.min(cp.x);
            r = r.max(cp.x);
            t = t.min(cp.y);
            b = b.max(cp.y);
        }

        // cpg cant be in region
        if region.width() < r - l || region.height() < b - t {
            return ans;
        }

        let tolerance = (cpg.tolerance * 255.0) as u8;

        for dy in (region.top as i32 - t as i32)..(region.bottom as i32 - b as i32) {
            'outer: for dx in (region.left as i32 - l as i32)..(region.right as i32 - r as i32) {
                for cp in &cpg.group {
                    let x = (cp.x as i32 + dx) as u32;
                    let y = (cp.y as i32 + dy) as u32;
                    let i = ((y * self.width + x) * 4) as usize;
                    if self.data[i].abs_diff(cp.red) > tolerance
                        || self.data[i + 1].abs_diff(cp.green) > tolerance
                        || self.data[i + 2].abs_diff(cp.blue) > tolerance
                    {
                        continue 'outer;
                    }
                }

                ans.push(Point {
                    x: (cpg.group[0].x as i32 + dx) as u32,
                    y: (cpg.group[0].y as i32 + dy) as u32,
                });

                if ans.len() >= max_num {
                    return ans;
                }
            }
        }
        ans
    }

    fn find_image_in(&self, img: &ImageIn) -> Option<Point> {
        self.find_all_image_in(img, 1).first().cloned()
    }

    fn find_all_image_in(
        &self,
        ImageIn {
            img,
            region,
            tolerance,
        }: &ImageIn,
        max_num: usize,
    ) -> Vec<Point> {
        let mut ans = vec![];

        // img cant be in region
        if img.width() < region.width() || img.height() < region.height() {
            return ans;
        }

        // region is not in screenshot
        // or region's area is zero
        if region.right >= self.width
            || region.bottom >= self.height
            || region.left >= region.right
            || region.top >= region.bottom
        {
            return ans;
        }
        let ih = img.height();
        let iw = img.width();
        let img = img.as_raw().as_slice();
        let base = (ih * iw) as f64 * 3.0;

        for y in region.top..region.bottom - ih {
            'outer: for x in region.left..region.right - iw {
                let mut loss = 0f64;
                for iy in 0..ih {
                    for ix in 0..iw {
                        let i = ((iy * iw + ix) * 4) as usize;
                        let j = ((y * self.width + x) * 4) as usize;

                        let r = img[i].abs_diff(self.data[j]) as f64;
                        let g = img[i + 1].abs_diff(self.data[j + 1]) as f64;
                        let b = img[i + 2].abs_diff(self.data[j + 2]) as f64;
                        let a = img[i + 3] as f64 / 255.0;

                        match tolerance {
                            Tolerance::MAE(limit) => {
                                loss += a * (r + g + b) / base;
                                if loss > (*limit as f64) {
                                    continue 'outer;
                                }
                            }
                            Tolerance::MSE(limit) => {
                                loss += a * (r.powi(2) + g.powi(2) + b.powi(2)) / base;
                                if loss > (*limit as f64) {
                                    continue 'outer;
                                }
                            }
                            Tolerance::MAX(limit) => {
                                loss = loss.max(a * r.max(g).max(b));
                                if loss > (*limit) as f64 {
                                    continue 'outer;
                                }
                            }
                        }
                    }
                }
                ans.push(Point { x, y });
                if ans.len() >= max_num {
                    return ans;
                }
            }
        }

        ans
    }
}

static STATUS_TOKEN: LazyLock<Futex<Private>> =
    LazyLock::new(|| Futex::new(Status::Stopped as u32));

struct Store {
    vm: JavaVM,
    host_ref: GlobalRef,
    screen_node: ScreenNode,
}
pub static STORE: OnceLock<Store> = OnceLock::new();
impl Store {
    pub fn store() -> &'static Store {
        STORE.get().unwrap()
    }
    fn init(env: &JNIEnv, obj: &JObject) -> Result<()> {
        if STORE.get().is_some() {
            return Ok(());
        }
        let vm = env.get_java_vm()?;
        let obj_ref = env.new_global_ref(obj)?;
        let _ = STORE
            .set(Store {
                vm,
                host_ref: obj_ref,
                screen_node: ScreenNode::default(),
            })
            .map_err(|_| Error::msg("Store set fail"))?;
        Ok(())
    }

    fn proxy() -> Proxy<'static> {
        let store = STORE.get().ok_or(Error::msg("Store get fail")).unwrap();
        let env = store.vm.attach_current_thread_permanently().unwrap();
        let host = store.host_ref.as_obj();

        Proxy { env, host }
    }
}

struct Proxy<'a> {
    env: JNIEnv<'a>,
    host: &'a JObject<'a>,
}

impl<'a> Proxy<'a> {
    fn fetch_screen_node(&mut self) {
        let ans = self
            .env
            // .call_method(&self.host, "takeScreenNode", "()Ljava/lang/Object;", &[])
            .call_method(&self.host, "takeScreenNode", "()LScreenNode;", &[])
            .unwrap();
        let obj = ans.l().unwrap();
        let first = self.env.get_field(&obj, "first", "[B").unwrap();
        let second = self
            .env
            .get_field(
                &obj,
                "second",
                "[Landroid/view/accessibility/AccessibilityNodeInfo;",
            )
            // /get_field(&obj, "second", "Ljava/util/List;")
            .unwrap();

        let first: JByteArray = first.l().unwrap().into();
        let first = self.env.convert_byte_array(first).unwrap();
        error!("114: {first:?}");
        let first: Vec<Node> = serde_json::from_slice(&first).unwrap();

        // let second = self.env.new_global_ref(second.l().unwrap()).unwrap();
        // let second: &JObjectArray = second.as_obj().into();
        let second: JObjectArray = second.l().unwrap().into();

        for node in first {
            if node.text.contains("mail") {
                let i = node.index.try_into().unwrap();
                let s = self.env.get_array_length(&second).unwrap();

                error!("get array element {} / {}", 1, s);
                let e0 = self.env.get_object_array_element(&second, i).unwrap();

                const ACTION_CLICK: i32 = 0x00000010;
                self.env
                    .call_method(&e0, "performAction", "(I)Z", &[ACTION_CLICK.into()])
                    .unwrap();
                // self.env
                //     .call_method(
                //         &self.host,
                //         "clickNode",
                //         "(Landroid/view/accessibility/AccessibilityNodeInfo;)Z",
                //         &[(&e0).into()],
                //     )
                //     .unwrap();

                error!("141");
            }
            error!("142");
        }
    }

    fn update_config_ui<State: Serialize + 'static>(
        &mut self,
        state: &mut State,
        view: impl Fn(&State) -> Element<State>,
    ) {
        // generate element tree
        let mut element = view(state);

        // take out callback and assign callback_id
        let callback = element.collect_callback();

        // serialize element
        let byte = serde_json::to_vec(&element).unwrap();

        // debug!("{:?}", serde_json::to_string(&element));

        // TODO may be leak
        let value = self.env.byte_array_from_slice(&byte).unwrap();

        let _ = self
            .env
            .call_method(&self.host, "updateConfigUI", "([B)V", &[(&value).into()]);

        let event = self
            .env
            .call_method(&self.host, "waitConfigUIEvent", "()[B", &[])
            .unwrap();
        // error!("64");

        // TODO may be leak
        let event: JByteArray = event.l().unwrap().into();
        // error!("65, {:?}", serde_json::to_string(&CallbackMsg {
        //     id:0,
        //     value: Box::new("abc".to_string())
        // }));
        // let x : CallbackMsg = serde_json::from_slice(&serde_json::to_vec(value))

        let event = self.env.convert_byte_array(event).unwrap();

        // error!("67 {:?}", event);
        let event: Vec<CallbackMsg> = serde_json::from_slice(&event).unwrap();

        error!("{:?}", event);

        for event in event {
            callback[event.id](state, event.value)
        }
    }

    fn start_http_server() {}

    fn handle_config_ui_event<State>(&mut self, element: Element<State>) {}

    fn update_float_ui() {}

    fn toast(&mut self, msg: &str) {
        let msg: JObject = self.env.new_string(&msg).unwrap().into();
        let msg = self.env.auto_local(msg);
        self.env
            .call_method(
                self.host,
                "toast",
                "(Ljava/lang/String;)V",
                &[msg.as_ref().into()],
            )
            .unwrap();
    }
    fn toast2(&mut self, msg: &str) {
        let msg: JObject = self.env.new_string(&msg).unwrap().into();
        let msg = self.env.auto_local(msg);
        self.env
            .call_method(
                self.host,
                "toast2",
                "(Ljava/lang/String;)V",
                &[msg.as_ref().into()],
            )
            .unwrap();
    }
    fn take_screenshot(&mut self) -> Screenshot<'static> {
        let screenshot: Result<Screenshot, Box<dyn std::error::Error>> =
            self.env.with_local_frame(32, |mut env| {
                let screenshot = env
                    .call_method(self.host, "takeScreenshot", "()LScreenshot;", &[])
                    .unwrap()
                    .l()
                    .unwrap();
                let width = env
                    .get_field(&screenshot, "width", "I")
                    .unwrap()
                    .i()
                    .unwrap()
                    .try_into()
                    .unwrap();
                let height = env
                    .get_field(&screenshot, "height", "I")
                    .unwrap()
                    .i()
                    .unwrap()
                    .try_into()
                    .unwrap();
                let pixel_stride = env
                    .get_field(&screenshot, "pixelStride", "I")
                    .unwrap()
                    .i()
                    .unwrap();
                let row_stride = env
                    .get_field(&screenshot, "rowStride", "I")
                    .unwrap()
                    .i()
                    .unwrap();

                let data: JByteBuffer = env
                    .get_field(&screenshot, "data", "Ljava/nio/ByteBuffer;")
                    .unwrap()
                    .l()
                    .unwrap()
                    .into();

                let addr = env.get_direct_buffer_address(&data).unwrap();
                let capacity = env.get_direct_buffer_capacity(&data).unwrap();
                let data = unsafe { std::slice::from_raw_parts(addr, capacity) };
                Ok(Screenshot {
                    width,
                    height,
                    // pixel_stride,
                    // row_stride,
                    data,
                })
            });
        screenshot.unwrap()
        // TODO memory leak ?
    }
    fn click(&mut self, x: f32, y: f32) {
        self.env
            .call_method(&self.host, "click", "(FF)V", &[x.into(), y.into()])
            .unwrap();
    }
    fn touch_down(&mut self, x: f32, y: f32, id: i32) {
        self.env
            .call_method(
                &self.host,
                "touchDown",
                "(FFI)V",
                &[x.into(), y.into(), id.into()],
            )
            .unwrap();
    }
    fn touch_up(&mut self, x: f32, y: f32, id: i32) {
        self.env
            .call_method(
                &self.host,
                "touchUp",
                "(FFI)V",
                &[x.into(), y.into(), id.into()],
            )
            .unwrap();
    }
    fn touch_move(&mut self, x: f32, y: f32, id: i32) {
        self.env
            .call_method(
                &self.host,
                "touchMove",
                "(FFI)V",
                &[x.into(), y.into(), id.into()],
            )
            .unwrap();
    }

    fn click_recent(&mut self) {
        self.env
            .call_method(&self.host, "clickRecent", "()V", &[])
            .unwrap();
    }
}

// static CELL3: OnceLock<JObject> = OnceLock::new();
// fn get_object() -> &'static JObject<'static> {
//     CELL3.get().unwrap()
// }

pub fn toast(msg: &str) {
    Store::proxy().toast(msg);
}
pub fn toast2(msg: &str) {
    Store::proxy().toast2(msg);
}

pub fn take_screenshot() -> Screenshot<'static> {
    Store::proxy().take_screenshot()
}
pub fn click(x: f32, y: f32) {
    Store::proxy().click(x, y);
}
pub fn touch_down(x: f32, y: f32, id: i32) {
    Store::proxy().touch_down(x, y, id);
}
pub fn touch_up(x: f32, y: f32, id: i32) {
    Store::proxy().touch_up(x, y, id);
}
pub fn touch_move(x: f32, y: f32, id: i32) {
    Store::proxy().touch_move(x, y, id);
}
pub fn click_recent() {
    Store::proxy().click_recent();
}

pub fn wait_secs(s: impl IntoSeconds) {
    let _ = STATUS_TOKEN.wait_for(Status::Running as u32, s.into_seconds());
    is_running_status();
}
pub fn wait_millis(s: impl IntoMilliseconds) {
    let _ = STATUS_TOKEN.wait_for(Status::Running as u32, s.into_milliseconds());
    is_running_status();
}

pub fn update_screen_shot() {}

pub enum Status {
    Stopped = 0,
    Running = 1,
}

pub fn status() -> Status {
    let i = STATUS_TOKEN
        .value
        .load(std::sync::atomic::Ordering::Relaxed);

    match i {
        0 => Status::Stopped,
        1 => Status::Running,
        _ => panic!(),
    }
}

pub fn set_status(status: Status) {
    STATUS_TOKEN
        .value
        .store(status as u32, std::sync::atomic::Ordering::Relaxed);
}

pub fn is_running_status() {
    if !matches!(status(), Status::Running) {
        panic!();
    }
}

pub static USER_START: OnceLock<fn()> = OnceLock::new();

#[macro_export]
macro_rules! entry {
    ($f:expr) => {
        #[no_mangle]
        extern "C" fn before_start() {
            let _ = $crate::USER_START.set($f);
        }
    };
}

// static BACKSTRACE: RwLock<Option<Backtrace>> = RwLock::new(None);

#[no_mangle]
extern "C" fn start(mut env: JNIEnv, host: JObject) {
    log_panics::init();

    let _ = std::panic::catch_unwind(move || {
        // running before previous stopped? unexpected!
        if matches!(status(), Status::Running) {
            panic!();
        }

        Store::init(&env, &host).unwrap();

        set_status(Status::Running);

        android_logger::init_once(
            android_logger::Config::default().with_max_level(log::LevelFilter::Info),
        );

        // trace!("trace log after init");

        // change log level at anytime
        // log::set_max_level(log::LevelFilter::Warn);

        // trace!("trace log after init?");

        if let Some(f) = USER_START.get() {
            f();
        }

        // let msg: JObject = env.new_string("1from").unwrap().into();
        // thread::spawn(|| {
        //     sleep(1);
        //     error!("259");
        // });
        //
        // sleep(3);
        //
        // let _ = env.call_method(&host, "toast", "(Ljava/lang/String;)V", &[(&msg).into()]);
    });
    stop();
}

#[no_mangle]
extern "C" fn stop() {
    set_status(Status::Stopped);
    STATUS_TOKEN.wake(i32::MAX);
}
