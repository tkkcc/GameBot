#![feature(trait_upcasting)]
mod asset;
mod find_trait;
mod mail;
mod node;
mod t1;
mod ui;

use std::i32;
use std::ops::Range;
use std::sync::Arc;
use std::time::Instant;
use std::{
    sync::{LazyLock, OnceLock},
    time::Duration,
};

use anyhow::{Error, Result};

use ease::cubic_in_out;
use jni::objects::JByteBuffer;
// use erased_serde::Serialize;
// use git2::{CertificateCheckStatus, RemoteCallbacks};
use jni::{
    objects::{GlobalRef, JByteArray, JObject, JObjectArray},
    JNIEnv, JavaVM,
};
use linux_futex::{Futex, Private};
pub use mail::Region;
pub use mail::{
    ColorPoint, ColorPointGroup, ColorPointGroupIn, ColorPointIn, ImageIn, IntoMilliseconds,
    IntoSeconds, Point, Rect, Tolerance,
};
use node::ANode;
pub use node::Node;
pub use node::NodeSelector;
use serde::{Deserialize, Serialize};
pub use ui::CallbackValue;
pub use ui::Element;

pub use find_trait::Find;

pub use log::error;

pub fn add(left: usize, right: usize) -> usize {
    left + right
}
pub use ui::*;

#[macro_export]
macro_rules! d {
    // NOTE: We cannot use `concat!` to make a static string as a format argument
    // of `eprintln!` because `file!` could contain a `{` or
    // `$val` expression could be a block (`{ .. }`), in which case the `eprintln!`
    // will be malformed.
    () => {
        $crate::error!("[{}:{}:{}]", file!(), line!(), column!())
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
        ($($crate::d!($val)),+,)
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
pub struct Screenshot {
    pub width: u32,
    pub height: u32,
    // pub pixel_stride: i32,
    // pub row_stride: i32,
    pub data: &'static [u8],
    // pub data: Vec<u8>,
}

impl Screenshot {
    pub fn find_color_point(
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
        Some((x, y).into())
    }

    pub fn find_color_point_group(&self, cpg: &ColorPointGroup) -> Option<Point> {
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

    pub fn find_color_point_group_in(&self, cpg: &ColorPointGroupIn) -> Option<Point> {
        self.find_all_color_point_group_in(cpg, 1).first().cloned()
    }

    pub fn region(&self) -> Region {
        Region {
            left: 0,
            width: self.width,
            top: 0,
            height: self.height,
        }
    }

    pub fn find_all_color_point_group_in(
        &self,
        cpg: &ColorPointGroupIn,
        max_num: usize,
    ) -> Vec<Point> {
        // we visit all valid localtion: iter on delta x and y, move via color point x + delta x
        let mut ans = vec![];
        if cpg.group.is_empty() {
            return ans;
        }
        let region = &cpg.region;

        if !self.region().contains(region) {
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
        if region.width < r - l || region.height < b - t {
            return ans;
        }

        let tolerance = (cpg.tolerance * 255.0) as u8;

        for dy in (region.top as i32 - t as i32)..(region.bottom() as i32 - b as i32) {
            'outer: for dx in (region.left as i32 - l as i32)..(region.right() as i32 - r as i32) {
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
                    x: (cpg.group[0].x as i32 + dx),
                    y: (cpg.group[0].y as i32 + dy),
                });

                if ans.len() >= max_num {
                    return ans;
                }
            }
        }
        ans
    }

    pub fn find_image_in(&self, img: &ImageIn) -> Option<Point> {
        self.find_all_image_in(img, 1).first().cloned()
    }

    pub fn find_all_image_in(
        &self,
        ImageIn {
            img,
            region,
            tolerance,
        }: &ImageIn,
        max_num: usize,
    ) -> Vec<Point> {
        let mut ans = vec![];

        if !self.region().contains(region)
            || img.width() > region.width
            || img.height() > region.height
        {
            return ans;
        }

        let ih = img.height();
        let iw = img.width();
        let img = img.as_raw().as_slice();
        let base = (ih * iw) as f64 * 3.0;

        for y in region.top..region.bottom() - ih {
            'outer: for x in region.left..region.right() - iw {
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
                ans.push((x, y).into());
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

pub struct Store {
    vm: JavaVM,
    host_ref: GlobalRef,
}
pub static STORE: OnceLock<Store> = OnceLock::new();
impl Store {
    pub fn store() -> &'static Store {
        STORE.get().unwrap()
    }
    pub fn init(env: &mut JNIEnv, obj: &JObject) -> Result<()> {
        if STORE.get().is_some() {
            return Ok(());
        }

        let c = env.find_class("java/lang/String").unwrap();
        let c = env.new_global_ref(c).unwrap();
        // let c2: &JObject = c.as_ref();
        let _ = STRING_CLASS.set(c);

        let c = env
            .find_class("android/view/accessibility/AccessibilityNodeInfo")
            .unwrap();
        let c = env.new_global_ref(c).unwrap();
        let _ = NODE_CLASS.set(c);

        let vm = env.get_java_vm()?;
        let obj_ref = env.new_global_ref(obj)?;
        let _ = STORE
            .set(Store {
                vm,
                host_ref: obj_ref,
            })
            .map_err(|_| Error::msg("Store set fail"))?;
        Ok(())
    }

    pub fn proxy() -> Proxy {
        let store = STORE.get().ok_or(Error::msg("Store get fail")).unwrap();
        let env = store.vm.attach_current_thread_permanently().unwrap();
        let host = store.host_ref.as_obj();

        Proxy { env, host }
    }
}

pub struct Proxy {
    env: JNIEnv<'static>,
    host: &'static JObject<'static>,
}

static STRING_CLASS: OnceLock<GlobalRef> = OnceLock::new();
static NODE_CLASS: OnceLock<GlobalRef> = OnceLock::new();

impl Proxy {
    fn take_nodeshot_serde(&mut self) -> Vec<ANode> {
        self.env
            .with_local_frame(32, |env| -> std::result::Result<_, Error> {
                let ans = env
                    .call_method(&self.host, "takeNodeshotSerde", "()LNodeshot;", &[])
                    .unwrap();
                let obj = ans.l().unwrap();
                let data: JByteBuffer = env
                    .get_field(&obj, "data", "Ljava/nio/ByteBuffer;")
                    .unwrap()
                    .l()
                    .unwrap()
                    .into();
                // let data_raw: JObjectArray = env
                //     .get_field(&obj, "data_raw", "[LNodeInfo;")
                //     .unwrap()
                //     .l()
                //     .unwrap()
                //     .into();
                let reference: JObjectArray = env
                    .get_field(
                        &obj,
                        "reference",
                        "[Landroid/view/accessibility/AccessibilityNodeInfo;",
                    )
                    .unwrap()
                    .l()
                    .unwrap()
                    .into();

                let addr = env.get_direct_buffer_address(&data).unwrap();
                let capacity = env.get_direct_buffer_capacity(&data).unwrap();
                let data = unsafe { std::slice::from_raw_parts(addr, capacity) };

                // most time consuming part, but cbor or get_field(unchecked) not help
                let data: Vec<Arc<Node>> = serde_json::from_slice(&data).unwrap();
                let data: Vec<ANode> = data.into_iter().map(ANode).collect();
                for (i, x) in data.iter().enumerate() {
                    if i != 0 {
                        *x.parent.borrow_mut() = Arc::downgrade(&data[x.parent_idx]);
                    }

                    *x.children.borrow_mut() =
                        x.children_idx.iter().map(|&i| data[i].clone()).collect();

                    x.obj.borrow_mut().replace({
                        let o = env.get_object_array_element(&reference, i as _).unwrap();
                        let o = env.new_global_ref(o).unwrap();
                        o
                    });
                }

                Ok(data)
            })
            .unwrap()
    }

    fn update_config_ui<State: Serialize + 'static>(
        &mut self,
        state: &mut State,
        view: impl Fn(&State) -> Element<State>,
    ) {
        check_running_status();

        // generate element tree
        let mut element = view(state);

        // take out callback and assign callback_id
        let callback = element.collect_callback();

        // serialize element
        let byte = serde_json::to_vec(&element).unwrap();

        // debug!("{:?}", serde_json::to_string(&element));

        // TODO may be leak
        let value = self.env.byte_array_from_slice(&byte).unwrap();

        d!(&value);

        self.env
            .call_method(&self.host, "updateConfigUI", "([B)V", &[(&value).into()])
            .unwrap();

        d!(440);

        let event = self
            .env
            .call_method(&self.host, "waitConfigUIEvent", "()[B", &[])
            .unwrap();

        d!(&event);

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
    pub fn toast2(&mut self, msg: &str) {
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
    fn take_screenshot(&mut self) -> Screenshot {
        self.env
            .with_local_frame(
                4,
                |mut env| -> Result<Screenshot, Box<dyn std::error::Error>> {
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
                    // let pixel_stride = env
                    //     .get_field(&screenshot, "pixelStride", "I")
                    //     .unwrap()
                    //     .i()
                    //     .unwrap();
                    // let row_stride = env
                    //     .get_field(&screenshot, "rowStride", "I")
                    //     .unwrap()
                    //     .i()
                    //     .unwrap();

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
                },
            )
            .unwrap()
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

    fn node_action(&mut self, obj: &JObject, i: i32) {
        self.env
            .call_method(obj, "performAction", "(I)Z", &[i.into()])
            .unwrap();
    }
    fn gesture(&mut self) {
        self.env
            .call_method(self.host, "gesture", "()V", &[])
            .unwrap();
    }
}

static NODE_ACTION_CLICK: i32 = 0x00000010;

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

pub fn take_screenshot() -> Screenshot {
    Store::proxy().take_screenshot()
}
pub fn click(x: f32, y: f32) {
    Store::proxy().click(x, y);
}
pub fn touch_down(x: f32, y: f32, id: i32) {
    d!("touch down ", x, y, id);
    Store::proxy().touch_down(x, y, id);
}
pub fn touch_up(x: f32, y: f32, id: i32) {
    d!("touch up ", x, y, id);
    Store::proxy().touch_up(x, y, id);
}
pub fn touch_move(x: f32, y: f32, id: i32) {
    d!("touch move ", x, y, id);
    Store::proxy().touch_move(x, y, id);
}
pub fn click_recent() {
    Store::proxy().click_recent();
}

pub fn take_nodeshot() -> Vec<ANode> {
    // Store::proxy().take_nodeshot()
    Store::proxy().take_nodeshot_serde()
    // root_node().map_or(vec![], |n| n.find_all(|_| true))
}

pub fn update_config_ui<State: Serialize + 'static>(
    state: &mut State,
    view: impl Fn(&State) -> Element<State>,
) {
    Store::proxy().update_config_ui(state, view)
}

pub fn get_string_test() -> String {
    // Store::proxy().get_string_test();
    // Store::proxy().fetch_screen_node();
    String::from("")
    // root_node().map_or(vec![], |n| n.find_all(|_| true))
}

pub fn wait_secs(s: impl IntoSeconds) {
    let _ = STATUS_TOKEN.wait_for(Status::Running as u32, s.into_seconds());
    check_running_status();
}
pub fn wait_millis(s: impl IntoMilliseconds) {
    let _ = STATUS_TOKEN.wait_for(Status::Running as u32, s.into_milliseconds());
    check_running_status();
}

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

pub fn check_running_status() {
    if !matches!(status(), Status::Running) {
        panic!();
    }
}

pub mod ease {
    pub type EaseFunc = fn(f32) -> f32;

    pub fn linear(t: f32) -> f32 {
        t
    }

    // https://docs.rs/simple-easing/latest/src/simple_easing/cubic.rs.html#12-18
    pub fn cubic_in_out(t: f32) -> f32 {
        if t < 0.5 {
            4.0 * t * t * t
        } else {
            1.0 - (-2.0 * t + 2.0).powi(3) / 2.0
        }
    }
}

pub fn test_gesture() {
    // multi finger
    gesture(&[
        vec![],
        vec![(0, (600, 500)), (500, (1000, 0)), (100, (1000, 0))],
        vec![],
        vec![(100, (600, 500)), (500, (1000, 0)), (100, (1000, 0))],
        vec![],
        vec![],
    ]);
}

pub fn gesture(path: &[Vec<(u64, (i32, i32))>]) {
    #[derive(Debug)]
    enum Action {
        Down,
        Up,
        Move,
    }

    #[derive(Debug)]
    struct PointEvent {
        x: f32,
        y: f32,
        id: i32,
        action: Action,
        time: Duration,
    }
    let mut point_list: Vec<_> = path
        .iter()
        .enumerate()
        .flat_map(|(i, path)| {
            let mut ans = vec![];
            let mut time = Duration::ZERO;
            for (j, &(delay, (x, y))) in path.iter().enumerate() {
                time += Duration::from_millis(delay);
                let point = move |action: Action| PointEvent {
                    x: x as _,
                    y: y as _,
                    id: i as i32,
                    action,
                    time,
                };
                if j == 0 {
                    ans.push(point(Action::Down));
                }
                if j > 0 {
                    ans.push(point(Action::Move));
                }

                if j + 1 == path.len() {
                    ans.push(point(Action::Up));
                }
            }
            ans
        })
        .collect();

    point_list.sort_by_key(|e| e.time);

    let start = Instant::now();
    for PointEvent {
        x,
        y,
        id,
        action,
        time,
    } in point_list
    {
        wait_millis(time.saturating_sub(start.elapsed()));

        // d!(start.elapsed(), x, y, id, &action);

        match action {
            Action::Down => touch_down(x, y, id),
            Action::Up => touch_up(x, y, id),
            Action::Move => touch_move(x, y, id),
        }
    }
}

pub fn gesture_smooth(path: &[Vec<(u64, (i32, i32))>]) {
    gesture_interpolated(path, cubic_in_out, Duration::from_millis(33))
}

pub fn gesture_interpolated(
    path: &[Vec<(u64, (i32, i32))>],
    ease_func: ease::EaseFunc,
    sample_interval: Duration,
) {
    #[derive(Debug)]
    struct MoveEvent {
        x1: f32,
        y1: f32,
        x2: f32,
        y2: f32,
        id: i32,
        time: Range<Duration>,
    }

    #[derive(Debug)]
    enum Action {
        Down,
        Up,
    }

    #[derive(Debug)]
    struct UpDownEvent {
        x: f32,
        y: f32,
        id: i32,
        action: Action,
        time: Duration,
    }

    let mut updown_event = vec![];
    let mut move_event = vec![];

    for (i, path) in path.iter().enumerate() {
        let mut time = Duration::ZERO;
        for (j, &(delay, (x, y))) in path.iter().enumerate() {
            time += Duration::from_millis(delay);

            if j > 0 {
                move_event.push(MoveEvent {
                    x1: path[j - 1].1 .0 as f32,
                    y1: path[j - 1].1 .1 as f32,
                    x2: x as f32,
                    y2: y as f32,
                    id: i as _,
                    time: time - Duration::from_millis(delay)..time,
                });
            }

            let updown = move |action: Action| UpDownEvent {
                x: x as _,
                y: y as _,
                id: i as i32,
                action,
                time,
            };
            if j == 0 {
                updown_event.push(updown(Action::Down));
            }
            if j + 1 == path.len() {
                updown_event.push(updown(Action::Up));
            }
        }
    }

    updown_event.sort_by_key(|e| e.time);
    move_event.sort_by_key(|e| e.time.end);
    let mut updown_event = updown_event.as_slice();
    let mut move_event = move_event.as_slice();
    let Some(mut next_wake_time) = updown_event.first().map(|x| x.time) else {
        return;
    };
    let start = Instant::now();

    loop {
        // d!(move_event.len(), move_event);
        // break;
        // d!(next_wake_time, start.elapsed());
        wait_millis(next_wake_time.saturating_sub(start.elapsed()));
        let i = updown_event
            .iter()
            .position(|e| e.time > start.elapsed())
            .unwrap_or(updown_event.len());

        for &UpDownEvent {
            x,
            y,
            id,
            ref action,
            ..
        } in &updown_event[..i]
        {
            // d!("updown", x, y, id, action);
            match action {
                Action::Down => touch_down(x, y, id),
                Action::Up => {
                    // touch_move(x, y, id);
                    touch_up(x, y, id)
                }
            }
        }
        updown_event = &updown_event[i..];

        let mut i = 0;
        for (
            j,
            &MoveEvent {
                x1,
                y1,
                x2,
                y2,
                id,
                ref time,
            },
        ) in move_event.iter().enumerate()
        {
            let now = start.elapsed();
            if time.end <= now {
                i = j;
            }
            if time.contains(&now) {
                let t = (now - time.start).as_secs_f32()
                    / ((time.end - time.start).as_secs_f32() + 1e-6);
                let t = ease_func(t);
                let x = x1 + (x2 - x1) * t;
                let y = y1 + (y2 - y1) * t;
                // d!("move", x, y, id);
                touch_move(x, y, id);
            }
        }
        move_event = &move_event[i..];

        if move_event.is_empty() && updown_event.is_empty() {
            break;
        } else if move_event.is_empty() {
            next_wake_time = updown_event.first().unwrap().time;
        } else if updown_event.is_empty() {
            next_wake_time += sample_interval;
        } else {
            next_wake_time = updown_event
                .first()
                .unwrap()
                .time
                .min(next_wake_time + sample_interval);
        }
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

    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("gamebot"),
    );

    // running before previous stopped? unexpected!
    if matches!(status(), Status::Running) {
        stop(env, host);
        return;
    }

    set_status(Status::Running);

    Store::init(&mut env, &host).unwrap();

    if let Some(f) = USER_START.get() {
        let _ = std::panic::catch_unwind(|| {
            f();
        });
    }

    stop(env, host);
}

#[no_mangle]
extern "C" fn stop(mut env: JNIEnv, host: JObject) {
    set_status(Status::Stopped);
    STATUS_TOKEN.wake(i32::MAX);

    // stop callback / channel
    let _ = env.call_method(&host, "stopConfigUIEvent", "()V", &[]);
}
