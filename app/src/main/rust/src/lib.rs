mod t;
mod t1;
mod t2;
use std::{error::Error, fmt::Display, thread::sleep, time::Duration};

// use git2::{CertificateCheckStatus, RemoteCallbacks};
use jni::{
    objects::{JClass, JObject, JString},
    sys::{jint, jstring},
    JNIEnv,
};
use t2::{simple_ui, Element};
// use wasmtime::{Caller, Engine, Linker, Module, Store};
// use tracing::{debug, error, Subscriber};
// use tracing_subscriber::Registry;

pub fn add(left: usize, right: usize) -> usize {
    left + right
}

#[macro_use]
extern crate log;

extern crate android_logger;

struct Proxy<'a> {
    env: JNIEnv<'a>,
    host: JObject<'a>,
}

impl<'a> Proxy<'a> {
    fn update_config_ui<State>(&mut self, element: Element<State>) {
        // serialize element into non-callback with id
        // store callabck in central
        let byte = serde_json::to_vec(&element).unwrap();

        debug!("{:?}", serde_json::to_string(&element));

        let value = self.env.byte_array_from_slice(&byte).unwrap();

        let _ = self
            .env
            .call_method(&self.host, "updateConfigUI", "([B)V", &[(&value).into()]);
    }

    fn handle_config_ui_event<State>(&mut self, element: Element<State>) {}

    fn update_float_ui() {}
}

#[no_mangle]
extern "C" fn Java_gamebot_host_Native_start(mut env: JNIEnv, class: JClass, host: JObject) {
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Debug), 
        // .with_tag("mytag")
// .with_filter(
//     android_logger::FilterBuilder::new()
//         .parse("debug,hello::crate=trace")
//         .build(),
// ),
    );
    let mut proxy = Proxy { env, host };
    proxy.update_config_ui(simple_ui())

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
    }
}
