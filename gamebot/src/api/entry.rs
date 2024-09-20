use std::sync::{LazyLock, OnceLock};

use jni::{objects::JObject, JNIEnv};

use super::{
    status::{
        get_status, is_running_status, set_running_status, set_status, set_stopped_status, Status,
        STATUS_TOKEN,
    },
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
extern "C" fn start(mut env: JNIEnv, host: JObject) {
    log_panics::init();

    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("gamebot"),
    );

    // running before previous stopped? unexpected!
    if is_running_status() {
        stop(env, host);
        return;
    }
    set_running_status();

    Store::init(&mut env, &host).unwrap();

    // Store::proxy().wait_config_ui_event();

    if let Some(f) = USER_START.get() {
        let _ = std::panic::catch_unwind(|| {
            f();
        });
    }

    stop(env, host);
}

#[no_mangle]
extern "C" fn stop(mut env: JNIEnv, host: JObject) {
    set_stopped_status();
    STATUS_TOKEN.wake(i32::MAX);

    // stop callback / channel
    let _ = env.call_method(&host, "stopConfigUIEvent", "()V", &[]);
}
