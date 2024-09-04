use std::collections::HashMap;
use std::sync::LazyLock;
use std::sync::Mutex;

use jni::objects::JObject;
use jni::objects::JString;
use jni::{objects::JClass, JNIEnv};

static STORE: LazyLock<Mutex<HashMap<String, Guest>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));

struct Guest<'a> {
    pub start: libloading::Symbol<'a, extern "C" fn(JNIEnv, JObject)>,
    pub stop: libloading::Symbol<'a, extern "C" fn()>,
}

fn load_library(name: &str) -> Result<(), Box<dyn std::error::Error>> {
    if STORE.lock().unwrap().contains_key(name) {
        return Ok(());
    }
    unsafe {
        let lib = libloading::Library::new("/data/local/tmp/libgamebot.so")?;
        let lib = Box::leak(Box::new(lib));
        let start: libloading::Symbol<extern "C" fn(JNIEnv, JObject)> = lib.get(b"start")?;
        let stop: libloading::Symbol<extern "C" fn()> = lib.get(b"stop")?;
        STORE
            .lock()
            .unwrap()
            .insert(name.to_owned(), Guest { start, stop });
    }
    Ok(())
}

#[no_mangle]
extern "C" fn Java_RemoteService_startGuest(
    mut env: JNIEnv,
    class: JClass,
    name: JString,
    host: JObject,
) {
    let name: String = env.get_string(&name).unwrap().into();
    if load_library(&name).is_err() {
        return;
    };
    let method = (&STORE.lock().unwrap()[&name]).start.clone();

    method(env, host);
}

#[no_mangle]
extern "C" fn Java_RemoteService_stopGuest(mut env: JNIEnv, class: JClass, name: JString) {
    let name: String = env.get_string(&name).unwrap().into();
    let method = (&STORE.lock().unwrap()[&name]).stop.clone();
    method();
}
