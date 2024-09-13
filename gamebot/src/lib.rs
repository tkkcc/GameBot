#![feature(trait_upcasting)]
mod asset;
mod find_trait;
mod mail;
mod node;
mod t1;
mod ui;

use core::{error, f64, panic};
use std::backtrace::Backtrace;
use std::cell::{Cell, RefCell};
use std::i32;
use std::sync::atomic::AtomicU32;
use std::sync::{Arc, RwLock};
use std::time::Instant;
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
use jni::descriptors::Desc;
use jni::objects::{AutoLocal, JByteBuffer, JFieldID, JString};
use jni::signature::ReturnType;
// use erased_serde::Serialize;
// use git2::{CertificateCheckStatus, RemoteCallbacks};
use jni::{
    objects::{GlobalRef, JByteArray, JClass, JObject, JObjectArray},
    JNIEnv, JavaVM,
};
use linux_futex::{Futex, Private};
pub use mail::{
    ColorPoint, ColorPointGroup, ColorPointGroupIn, ColorPointIn, ImageIn, IntoMilliseconds,
    IntoSeconds, Point, Rect, Tolerance,
};
pub use node::NodeSelector;
pub use node::{Node, Node2, Node4};
use serde::{Deserialize, Serialize};
use ui::CallbackValue;
use ui::Element;

pub use find_trait::Find;

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
        Some(Point { x, y })
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

pub struct Store {
    vm: JavaVM,
    host_ref: GlobalRef,
    screen_node: ScreenNode,
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
                screen_node: ScreenNode::default(),
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
    fn take_screen_node(&mut self) -> Vec<Arc<RefCell<Node4>>> {
        self.env
            .with_local_frame(32, |env| -> std::result::Result<_, Error> {
                let ans = env
                    .call_method(&self.host, "takeScreenNode", "()LScreenNode;", &[])
                    .unwrap();
                let obj = ans.l().unwrap();
                let data: JByteBuffer = env
                    .get_field(&obj, "data", "Ljava/nio/ByteBuffer;")
                    .unwrap()
                    .l()
                    .unwrap()
                    .into();
                let data_raw: JObjectArray = env
                    .get_field(&obj, "data_raw", "[LNodeInfo;")
                    .unwrap()
                    .l()
                    .unwrap()
                    .into();
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

                // most time consuming part
                // let data: Vec<Node> = serde_json::from_slice(&data).unwrap();

                // how about do without Deserialize?
                let count = env.get_array_length(&data_raw).unwrap();

                // let obj = obj.as_ref();
                let class = env.find_class("NodeInfo").unwrap();
                //
                // let field_id: JFieldID =
                //     Desc::<JFieldID>::lookup((&class, "visible", "Z"), env).unwrap();

                // let class2 = env.find_class("java/lang/String").unwrap();
                let field_id2: JFieldID =
                    Desc::<JFieldID>::lookup((&class, "id", "Ljava/lang/String;"), env).unwrap();

                let data: Vec<Node> = (0..count)
                    .map(|i| {
                        let x = env.get_object_array_element(&data_raw, i).unwrap();
                        for j in 0..5 {
                            // let parsed = ReturnType::Primitive(jni::signature::Primitive::Boolean);
                            // env.get_field_unchecked(&data_raw, &field_id, parsed)
                            //     .unwrap()
                            //     .z()
                            //     .unwrap();
                            // let id: JString = env
                            //     .get_field(&x, "id", "Ljava/lang/String;")
                            //     .unwrap()
                            //     .l()
                            //     .unwrap()
                            //     .into();
                            // let id: String = env.get_string(&id).unwrap().into();
                            let id: JString = env
                                // .get_field(&x, "id", "Ljava/lang/String;")
                                .get_field_unchecked(&x, &field_id2, ReturnType::Object)
                                .unwrap()
                                .l()
                                .unwrap()
                                .into();
                            let id: String =
                                unsafe { env.get_string_unchecked(&id) }.unwrap().into();
                        }
                        let id: JString = env
                            // .get_field(&x, "id", "Ljava/lang/String;")
                            .get_field_unchecked(&x, &field_id2, ReturnType::Object)
                            .unwrap()
                            .l()
                            .unwrap()
                            .into();
                        let id: String = unsafe { env.get_string_unchecked(&id) }.unwrap().into();

                        // let parent =
                        //     env.get_field(&x, "parent", "I").unwrap().i().unwrap() as usize;

                        Node {
                            id,
                            ..Default::default()
                        }
                    })
                    .collect();

                let data: Vec<Arc<RefCell<Node4>>> = data
                    .into_iter()
                    .map(|x| {
                        Arc::new(RefCell::new(Node4 {
                            info: x,
                            ..Default::default()
                        }))
                    })
                    .collect();

                for x in data.iter() {
                    let t = Some(data[x.borrow().info.parent].clone());
                    x.borrow_mut().parent = t;
                    let t = x
                        .borrow()
                        .info
                        .children
                        .iter()
                        .map(|&i| data[i].clone())
                        .collect();

                    x.borrow_mut().children = t;
                }

                Ok(data)
                // for i in &data {
                //     new_data.
                // }

                // let second = self.env.new_global_ref(second.l().unwrap()).unwrap();
                // let second: &JObjectArray = second.as_obj().into();
                // let second: JObjectArray = second.l().unwrap().into();
                // Ok(first)
            })
            .unwrap()

        // for node in first {
        //     if node.text.contains("mail") {
        //         let i = node.index.try_into().unwrap();
        //         let s = self.env.get_array_length(&second).unwrap();
        //
        //         error!("get array element {} / {}", 1, s);
        //         let e0 = self.env.get_object_array_element(&second, i).unwrap();
        //
        //         const ACTION_CLICK: i32 = 0x00000010;
        //         self.env
        //             .call_method(&e0, "performAction", "(I)Z", &[ACTION_CLICK.into()])
        //             .unwrap();
        //         // self.env
        //         //     .call_method(
        //         //         &self.host,
        //         //         "clickNode",
        //         //         "(Landroid/view/accessibility/AccessibilityNodeInfo;)Z",
        //         //         &[(&e0).into()],
        //         //     )
        //         //     .unwrap();
        //
        //         error!("141");
        //     }
        //     error!("142");
        // }
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

    fn root_node(&mut self) -> Option<AutoLocal<'static, JObject<'static>>> {
        let obj = self
            .env
            .call_method(
                &self.host,
                "rootNode",
                "()Landroid/view/accessibility/AccessibilityNodeInfo;",
                &[],
            )
            .unwrap()
            .l()
            .unwrap();
        if obj.is_null() {
            None
        } else {
            Some(self.env.auto_local(obj))
        }
    }

    fn get_node_view_id(&mut self, node: &JObject) -> String {
        self.env
            .with_local_frame(4, |env| -> std::result::Result<String, Error> {
                let class: &JClass = NODE_CLASS.get().unwrap().as_obj().into();
                let res: JString = unsafe {
                    env
                        // .call_method(node, "getViewIdResourceName", "()Ljava/lang/String;", &[])
                        .call_method_unchecked(
                            node,
                            (class, "getViewIdResourceName", "()Ljava/lang/String;"),
                            ReturnType::Object,
                            &[],
                        )
                }
                .unwrap()
                .l()
                .unwrap()
                .into();
                if res.is_null() {
                    return Ok(String::from(""));
                }
                Ok(unsafe { env.get_string_unchecked(&res) }.unwrap().into())
            })
            .unwrap()
    }

    fn get_string_test(&mut self) -> String {
        self.env
            .with_local_frame(4, |env| -> std::result::Result<String, Error> {
                // let res = env
                //     .call_method(&self.host, "getStringTest", "()V", &[])
                //     .unwrap();
                let res: JString = env
                    .get_field(&self.host, "what2", "Ljava/lang/String;")
                    .unwrap()
                    .l()
                    .unwrap()
                    .into();
                let res = env.get_string(&res).unwrap();

                return Ok(String::from(""));
                // if res.is_null() {
                //     return Ok(String::from(""));
                // }
                // Ok(env.get_string(&res).unwrap().into())
            })
            .unwrap()
    }

    fn get_node_text(&mut self, node: &JObject) -> String {
        self.env
            .with_local_frame(4, |env| -> Result<String, Box<dyn std::error::Error>> {
                let char_seq = env
                    .call_method(node, "getText", "()Ljava/lang/CharSequence;", &[])
                    .unwrap()
                    .l()
                    .unwrap();
                // let char_seq = self.env.auto_local(char_seq);

                if char_seq.is_null() {
                    return Ok(String::new());
                }

                let s: JString = env
                    .call_method(char_seq, "toString", "()Ljava/lang/String;", &[])
                    .unwrap()
                    .l()
                    .unwrap()
                    .into();
                // let s = self.env.auto_local(s);

                Ok(env.get_string(&s).unwrap().into())
            })
            .unwrap()
    }

    fn get_node_children(&mut self, node: &JObject) -> Vec<Node2> {
        let child_count = self
            .env
            .with_local_frame(4, |env| -> Result<_, Error> {
                Ok(env
                    .call_method(node, "getChildCount", "()I", &[])
                    .unwrap()
                    .i()
                    .unwrap())
            })
            .unwrap();
        let ans = (0..child_count)
            .map(|i| {
                let child = self
                    .env
                    .call_method(
                        node,
                        "getChild",
                        "(I)Landroid/view/accessibility/AccessibilityNodeInfo;",
                        &[i.into()],
                    )
                    .unwrap()
                    .l()
                    .unwrap();
                Node2(Arc::new(self.env.auto_local(child)))
            })
            .collect();
        ans
    }

    // fn take_nodeshot_in_kotlin(&mut self) {
    //     self.env
    //         .call_method(&self.host, "findNodeInKotlin", "()V", &[]);
    // }

    fn take_nodeshot(&mut self) -> Vec<Node2> {
        let x: JObjectArray = self
            .env
            .call_method(
                &self.host,
                "takeNodeshot",
                "()[Landroid/view/accessibility/AccessibilityNodeInfo;",
                &[],
            )
            .unwrap()
            .l()
            .unwrap()
            .into();
        let len = self.env.get_array_length(&x).unwrap();
        let ans = (0..len)
            .map(|i| {
                let o = self.env.get_object_array_element(&x, i).unwrap();
                Node2(Arc::new(self.env.auto_local(o)))
            })
            .collect();
        self.env.delete_local_ref(x).unwrap();
        ans
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

pub fn take_screenshot() -> Screenshot {
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

// find node from root
pub fn root_node() -> Option<Node2> {
    Store::proxy().root_node().map(|n| Node2(Arc::new(n)))
}

pub fn take_nodeshot() -> Vec<Node2> {
    Store::proxy().take_nodeshot()
    // root_node().map_or(vec![], |n| n.find_all(|_| true))
}

pub fn get_string_test() -> String {
    Store::proxy().get_string_test();
    // Store::proxy().fetch_screen_node();
    String::from("")
    // root_node().map_or(vec![], |n| n.find_all(|_| true))
}

pub fn take_nodeshot_serde() -> Vec<Arc<RefCell<Node4>>> {
    Store::proxy().take_screen_node()
}

pub fn take_nodeshot_locally() -> Vec<Node2> {
    root_node().map_or(vec![], |n| n.find_all(|_| true))
}

pub fn find_node_at(root: &Node2, filter: impl Fn(&Node2) -> bool) -> Option<Node2> {
    let mut stack = vec![root.clone()];
    while !stack.is_empty() {
        for root in std::mem::take(&mut stack) {
            if filter(&root) {
                return Some(root);
            }
            stack.extend(root.children());
        }
    }
    None
}

pub fn find_all_node_at(root: &Node2, filter: impl Fn(&Node2) -> bool) -> Vec<Node2> {
    let mut ans = vec![];
    let mut stack = vec![root.clone()];
    while !stack.is_empty() {
        for root in std::mem::take(&mut stack) {
            if filter(&root) {
                ans.push(root.clone());
            }
            stack.extend(root.children());
        }
    }
    ans
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

        // let mut proxy = Proxy {
        //     env: env,
        //     host: &host,
        // };
        // let n1 = proxy.root_node().unwrap();
        // n1.find_by_id("a");

        Store::init(&mut env, &host).unwrap();

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
