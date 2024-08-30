#![feature(trait_upcasting)]
mod asset;
mod find_trait;
mod mail;
mod node;
mod t1;
mod ui;

use core::error;
use std::{sync::OnceLock, thread::sleep, time::Duration};

use anyhow::{Error, Result};

// use erased_serde::Serialize;
// use git2::{CertificateCheckStatus, RemoteCallbacks};
use jni::{
    objects::{GlobalRef, JByteArray, JClass, JObject, JObjectArray},
    JNIEnv, JavaVM,
};
use node::Node;
use serde::{Deserialize, Serialize};
use ui::{simple_config, simple_view, CallbackValue, Element};
// use wasmtime::{Caller, Engine, Linker, Module, Store};
// use tracing::{debug, error, Subscriber};
// use tracing_subscriber::Registry;

pub fn add(left: usize, right: usize) -> usize {
    left + right
}

#[macro_use]
extern crate log;

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

struct Store {
    vm: JavaVM,
    host_ref: GlobalRef,
    screen_node: ScreenNode,
}
static STORE: OnceLock<Store> = OnceLock::new();
impl Store {
    fn init(env: &JNIEnv, obj: &JObject) -> Result<()> {
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

    fn proxy() -> Result<Proxy<'static>> {
        let store = STORE.get().ok_or(Error::msg("Store get fail"))?;
        let env = store.vm.attach_current_thread_permanently()?;
        let host = store.host_ref.as_obj();

        Ok(Proxy { env, host })
    }
}

struct Proxy<'a> {
    env: JNIEnv<'a>,
    host: &'a JObject<'a>,
}

impl<'a> Proxy<'a> {
    fn find(&self) {}
    fn try_fetch_screen_node(&self) {}

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
}

// static CELL3: OnceLock<JObject> = OnceLock::new();
// fn get_object() -> &'static JObject<'static> {
//     CELL3.get().unwrap()
// }

fn click() {
    let obj = get_object();
}

#[no_mangle]
extern "C" fn Java_Native_start(mut env: JNIEnv, class: JClass, host: JObject) {
    // log_panics::init();
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Debug),
    );
    std::panic::set_hook(Box::new(|info| {
        // let backtrace = std::backtrace::Backtrace::capture();

        // error!("{}", std::backtrace::Backtrace::capture());
        error!("{}", info);
    }));
    Store::init(&env, &host);

    // let x = env.new_global_ref(&host).unwrap();
    // let o = x.as_obj();
    //
    let mut state = simple_config();

    loop {
        let mut proxy = Store::proxy().unwrap();
        // proxy.update_config_ui(&mut state, simple_view);
        proxy.fetch_screen_node();
        sleep(Duration::from_millis(330));
    }

    // call updateConfigUI() and waitConfigUIEvent()
    // call updateFloatUI() and waitFloatUIEvent()
    // for waitConfigUIEvent, we use a channel of Event
    // what about wantConfigUIEvent and waitFloatUIEvent at same time?
    // need to be async in user side

    // let msg = env.new_string("native toast").unwrap();
    // let obj: &JObject = msg.as_ref();
    // loop {
    //     let _ = env.call_method(
    //         &host,
    //         "toast",
    //         "(Ljava/lang/String;)V",
    //         &[obj.as_ref().into()],
    //     );
    //     error!("what");
    //     sleep(Duration::from_secs(3));
    //     break
    // }
    //
    // struct Button {
    //     text: String,
    // }
    // impl Button {
    //     fn text(mut self, x: &str) -> Self {
    //         self.text = x.into();
    //         self
    //     }
    // }
    // fn button<D: Display>(x: impl Fn() -> D) -> Button {
    //     Button {
    //         text: x().to_string(),
    //     }
    // }

    // call the toast function of input

    // let path = String::from(env.get_string(&input).unwrap());
    // let out = format!("{:?}", f(path));
    // env.new_string(out).unwrap().into_raw();
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);

        let x = Box::new(|state: &mut i32| *state += 1);
        let mut i = 0;
        (*x)(&mut i);
    }
}
