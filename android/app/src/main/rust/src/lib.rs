use std::collections::HashMap;
use std::io;
use std::path::Path;
use std::path::PathBuf;
use std::sync::LazyLock;
use std::sync::Mutex;
use std::time::Duration;
use std::time::Instant;

use jni::objects::JObject;
use jni::objects::JString;
use jni::strings::JavaStr;
use jni::sys::jint;
use jni::{objects::JClass, JNIEnv};
use log::error;

static STORE: LazyLock<Mutex<HashMap<String, Guest>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));

struct Guest<'a> {
    pub before_start: libloading::Symbol<'a, extern "C" fn()>,
    pub start: libloading::Symbol<'a, extern "C" fn(JNIEnv, JObject)>,
    pub stop: libloading::Symbol<'a, extern "C" fn(JNIEnv, JObject)>,
}

fn load_library(name: &str) -> Result<(), Box<dyn std::error::Error>> {
    if STORE.lock().unwrap().contains_key(name) {
        return Ok(());
    }
    unsafe {
        let lib = libloading::Library::new("/data/local/tmp/libgamebot.so")?;
        let lib = Box::leak(Box::new(lib));
        let before_start: libloading::Symbol<extern "C" fn()> = lib.get(b"before_start")?;
        let start: libloading::Symbol<extern "C" fn(JNIEnv, JObject)> = lib.get(b"start")?;
        let stop: libloading::Symbol<extern "C" fn(JNIEnv, JObject)> = lib.get(b"stop")?;
        STORE.lock().unwrap().insert(
            name.to_owned(),
            Guest {
                before_start,
                start,
                stop,
            },
        );
    }
    Ok(())
}

#[no_mangle]
extern "C" fn Java_RemoteService_startGuest(
    mut env: JNIEnv,
    _: JClass,
    name: JString,
    host: JObject,
) {
    let name: String = env.get_string(&name).unwrap().into();
    if let Err(err) = load_library(&name) {
        error!("{:?}", err);
    };
    let func = (STORE.lock().unwrap()[&name]).before_start.clone();
    func();
    let func = (STORE.lock().unwrap()[&name]).start.clone();
    func(env, host);
}

#[no_mangle]
extern "C" fn Java_RemoteService_stopGuest(
    mut env: JNIEnv,
    _: JClass,
    name: JString,
    host: JObject,
) {
    let name: String = env.get_string(&name).unwrap().into();
    if let Some(func) = STORE.lock().unwrap().get(&name).map(|x| x.stop.clone()) {
        func(env, host);
    }
}

fn recreate_dir(path: impl AsRef<Path>) -> io::Result<()> {
    let path = path.as_ref();
    if path.exists() {
        if path.is_dir() {
            std::fs::remove_dir_all(&path)?;
        } else {
            std::fs::remove_file(&path)?;
        }
    }
    std::fs::create_dir_all(&path)?;
    Ok(())
}

#[no_mangle]
extern "C" fn Java_RemoteService_fetchWithCache<'a>(
    mut env: JNIEnv<'a>,
    _: JClass,
    url: JString,
    path: JString,
    progress_listener: JObject,
) -> JString<'a> {
    let url: String = JavaStr::from_env(&env, &url).unwrap().into();
    let path: String = JavaStr::from_env(&env, &path).unwrap().into();

    let cache_dir = PathBuf::from("/data/local/tmp/cache");
    std::fs::create_dir_all(&cache_dir).unwrap();
    let cache = cached_path::CacheBuilder::new()
        .dir(cache_dir)
        .freshness_lifetime(u64::MAX)
        .build()
        .unwrap();
    let path = cache
        .cached_path(&url)
        .unwrap_or_else(|_| panic!("fail to fetch: {url}"));

    let ans = env.new_string(path.to_str().unwrap()).unwrap();
    return ans;
}

#[no_mangle]
extern "C" fn Java_RemoteService_gitClone(
    mut env: JNIEnv,
    _: JClass,
    url: JString,
    branch: JString,
    path: JString,
    progress_listener: JObject,
) -> jint {
    let url: String = JavaStr::from_env(&env, &url).unwrap().into();
    let url = url.trim_start();
    let url = url.strip_prefix("https://").unwrap_or(url);
    let url = format!("http://{url}");
    error!("{}", url);

    let path: String = JavaStr::from_env(&env, &path).unwrap().into();
    let path = PathBuf::from(path);
    if recreate_dir(&path).is_err() {
        return 1;
    }

    let branch: String = JavaStr::from_env(&env, &branch).unwrap().into();

    let mut builder = git2::build::RepoBuilder::new();
    builder.remote_create(|repo, name, url| {
        let refspec = format!("+refs/heads/{0}:refs/remotes/origin/{0}", &branch);
        repo.remote_with_fetch(name, url, &refspec)
    });
    let mut fetch_option = git2::FetchOptions::new();
    fetch_option.depth(1);

    if !progress_listener.is_null() {
        fetch_option.remote_callbacks({
            let mut cb = git2::RemoteCallbacks::new();

            let mut prev_time = Instant::now();
            let mut prev_bytes = 0;
            cb.transfer_progress(move |state| {
                if prev_time.elapsed() < Duration::from_secs(3) {
                    return true;
                }
                let object_percent = state.received_objects() as f32 / state.total_objects() as f32;
                let bytes = state.received_bytes();
                let time = Instant::now();
                let bytes_per_second =
                    (bytes - prev_bytes) as f32 / (time - prev_time).as_secs_f32();
                prev_time = time;
                prev_bytes = bytes;
                env.call_method(
                    &progress_listener,
                    "onUpdate",
                    "(ff)V",
                    &[object_percent.into(), bytes_per_second.into()],
                );

                true
            });
            cb
        });
    }

    builder.fetch_options(fetch_option);
    builder.branch(&branch);

    match builder.clone(&url, &path) {
        Ok(_) => return 0,
        Err(err) => {
            error!("{}", err);
            return 1;
        }
    }
}

#[no_mangle]
extern "C" fn Java_RemoteService_initLogger(
    mut env: JNIEnv,
    _: JClass,
    name: JString,
    host: JObject,
) {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("gamebot"),
    );
}
