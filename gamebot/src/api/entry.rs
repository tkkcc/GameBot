use std::{
    ffi::{c_char, CString},
    sync::OnceLock,
};

use jni::{objects::JObject, JNIEnv};

use super::{
    status::{is_running_status, set_running_status, set_stopped_status, STATUS_TOKEN},
    store::Store,
};

extern crate android_logger;

pub static USER_START: OnceLock<fn()> = OnceLock::new();

#[macro_export]
macro_rules! entry {
    ($f:expr) => {
        #[no_mangle]
        extern "C" fn before_start() {
            let _ = $crate::api::entry::USER_START.set($f);
        }
    };
}

// static BACKSTRACE: RwLock<Option<Backtrace>> = RwLock::new(None);

#[no_mangle]
extern "C" fn start(env: &mut JNIEnv, host: JObject) -> *mut c_char {
    log_panics::init();

    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("gamebot"),
    );

    // running before previous stopped? unexpected!
    if is_running_status() {
        stop(env, host);
        let s = CString::new("fail: start before stop").unwrap();
        return s.into_raw();
    }
    set_running_status();

    env.call_method(&host, "onStart", "()V", &[]).unwrap();

    Store::init(env, &host).unwrap();

    let mut ret = String::from("");
    if let Some(f) = USER_START.get() {
        let r = std::panic::catch_unwind(|| {
            f();
        });
        if let Err(err) = r {
            ret = format!("{:?}", err);
        }
    }

    stop(env, host);

    let s = CString::new(ret).unwrap();
    return s.into_raw();
}

#[no_mangle]
extern "C" fn stop(env: &mut JNIEnv, host: JObject) {
    set_stopped_status();
    STATUS_TOKEN.wake(i32::MAX);

    // stop callback / channel
    env.call_method(&host, "onStop", "()V", &[]).unwrap();
}
