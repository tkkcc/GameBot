mod t;

use std::{error::Error, fmt::Display, thread::sleep, time::Duration};

// use git2::{CertificateCheckStatus, RemoteCallbacks};
use jni::{
    objects::{JClass, JObject, JString},
    sys::{jint, jstring},
    JNIEnv,
};
// use wasmtime::{Caller, Engine, Linker, Module, Store};
// use tracing::{debug, error, Subscriber};
// use tracing_subscriber::Registry;

pub fn add(left: usize, right: usize) -> usize {
    left + right
}

#[macro_use]
extern crate log;

extern crate android_logger;

#[no_mangle]
extern "C" fn Java_gamebot_host_Native_callback(mut env: JNIEnv, class: JClass, input: JString) {}

#[no_mangle]
extern "C" fn Java_gamebot_host_Native_start(mut env: JNIEnv, class: JClass, host: JObject) {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Trace)
            .with_tag("mytag")
            .with_filter(
                android_logger::FilterBuilder::new()
                    .parse("debug,hello::crate=trace")
                    .build(),
            ),
    );
    let msg = env.new_string("native toast").unwrap();
    let obj: &JObject = msg.as_ref();
    loop {
        let _ = env.call_method(
            &host,
            "toast",
            "(Ljava/lang/String;)V",
            &[obj.as_ref().into()],
        );
        error!("what");
        sleep(Duration::from_secs(3));
        break
    }

    struct Button {
        text: String,
    }
    impl Button {
        fn text(mut self, x: &str) -> Self {
            self.text = x.into();
            self
        }
    }
    fn button<D: Display>(x: impl Fn() -> D) -> Button {
        Button {
            text: x().to_string(),
        }
    }

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
