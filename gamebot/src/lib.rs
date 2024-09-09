#![feature(trait_upcasting)]
mod asset;
mod find_trait;
mod mail;
mod node;
mod t1;
mod ui;

use core::{error, panic};
use std::backtrace::Backtrace;
use std::cell::Cell;
use std::sync::atomic::AtomicU32;
use std::sync::{Arc, RwLock};
use std::{i32, thread};
use std::{
    sync::{
        atomic::AtomicBool,
        mpsc::{channel, Receiver, Sender, SyncSender},
        LazyLock, OnceLock,
    },
    time::Duration,
};

use anyhow::{Error, Result};

use jni::objects::JByteBuffer;
// use erased_serde::Serialize;
// use git2::{CertificateCheckStatus, RemoteCallbacks};
use jni::{
    objects::{GlobalRef, JByteArray, JClass, JObject, JObjectArray},
    JNIEnv, JavaVM,
};
use linux_futex::{Futex, Private};
use mail::{
    ColorPoint, ColorPointGroup, ColorPointGroupIn, ColorPointIn, IntoMilliseconds, IntoSeconds,
    Point,
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
        let i = (y * self.width * 4 + x) as usize;
        if self.data[i].abs_diff(red) > 0
            || self.data[i + 1].abs_diff(green) > 0
            || self.data[i + 2].abs_diff(blue) > 0
        {
            return None;
        }
        Some(Point { x, y })
    }
    fn find_color_point_group(&self, cgp: &ColorPointGroup) -> Option<Point> {
        for cp in &cgp.group {
            if cp.x >= self.width || cp.y >= self.height {
                return None;
            }
            let i = (cp.y * self.width * 4 + cp.x) as usize;
            if self.data[i].abs_diff(cp.red) > cgp.color_tolerance
                || self.data[i + 1].abs_diff(cp.green) > cgp.color_tolerance
                || self.data[i + 2].abs_diff(cp.blue) > cgp.color_tolerance
            {
                return None;
            }
        }
        cgp.group.get(0).map(Into::into)
    }
    fn find_color_point_group_in(&self, cgpi: &ColorPointGroupIn) -> Option<Point> {
        // we visit all possible localtion: iter on delta x and y, use color point x + delta x

        let mut t = u32::MAX;
        let mut l = u32::MAX;
        let mut b = 0;
        let mut r = 0;
        for cp in &cgpi.group {
            l = l.min(cp.x);
            r = r.max(cp.x);
            t = t.min(cp.y);
            b = b.max(cp.y);
        }

        None

        // for cp in &cgp.group {
        //     if cp.x >= self.width || cp.y >= self.height {
        //         return None;
        //     }
        //     let i = (cp.y * self.width * 4 + cp.x) as usize;
        //     if self.data[i].abs_diff(cp.red) > cgp.color_tolerance
        //         || self.data[i + 1].abs_diff(cp.green) > cgp.color_tolerance
        //         || self.data[i + 2].abs_diff(cp.blue) > cgp.color_tolerance
        //     {
        //         return None;
        //     }
        // }
        // cgp.group.get(0).map(Into::into)
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
    fn click(&mut self, x: i32, y: i32) {
        self.env
            .call_method(&self.host, "click", "(II)V", &[x.into(), y.into()])
            .unwrap();
    }
    fn touch_down(&mut self, x: i32, y: i32) {
        self.env
            .call_method(&self.host, "touchDown", "(II)V", &[x.into(), y.into()])
            .unwrap();
    }
    fn touch_up(&mut self, x: i32, y: i32) {
        self.env
            .call_method(&self.host, "touchUp", "(II)V", &[x.into(), y.into()])
            .unwrap();
    }
    fn touch_move(&mut self, x: i32, y: i32) {
        self.env
            .call_method(&self.host, "touchMove", "(II)V", &[x.into(), y.into()])
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
pub fn click(x: i32, y: i32) {
    Store::proxy().click(x, y);
}
pub fn touch_down(x: i32, y: i32) {
    Store::proxy().touch_down(x, y);
}
pub fn touch_up(x: i32, y: i32) {
    Store::proxy().touch_up(x, y);
}
pub fn touch_move(x: i32, y: i32) {
    Store::proxy().touch_move(x, y);
}
pub fn click_recent() {
    Store::proxy().click_recent();
}

pub fn ssleep(s: impl IntoSeconds) {
    let _ = STATUS_TOKEN.wait_for(Status::Running as u32, s.into_seconds());
    is_running_status();
}
pub fn msleep(s: impl IntoMilliseconds) {
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
