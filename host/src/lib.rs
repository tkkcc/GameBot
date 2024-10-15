use std::collections::HashMap;
use std::ffi::c_char;
use std::ffi::CString;
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
use jni::sys::jstring;
use jni::{objects::JClass, JNIEnv};

static STORE: LazyLock<Mutex<HashMap<String, Guest>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));

struct Guest<'a> {
    pub before_start: libloading::Symbol<'a, extern "C" fn()>,
    pub start: libloading::Symbol<'a, extern "C" fn(&mut JNIEnv, JObject) -> *mut c_char>,
    pub stop: libloading::Symbol<'a, extern "C" fn(&mut JNIEnv, JObject)>,
}

fn load_library(name: &str) -> Result<(), Box<dyn std::error::Error>> {
    if STORE.lock().unwrap().contains_key(name) {
        return Ok(());
    }
    unsafe {
        let lib = format!("/data/local/tmp/gamebot/guest/{name}/libguest.so");
        let lib = libloading::Library::new(lib)?;
        let lib = Box::leak(Box::new(lib));
        let before_start: libloading::Symbol<extern "C" fn()> = lib.get(b"before_start")?;
        let start: libloading::Symbol<extern "C" fn(&mut JNIEnv, JObject) -> *mut c_char> =
            lib.get(b"start")?;
        let stop: libloading::Symbol<extern "C" fn(&mut JNIEnv, JObject)> = lib.get(b"stop")?;
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
) -> jstring {
    let name: String = env.get_string(&name).unwrap().into();
    if let Err(err) = load_library(&name) {
        return env.new_string(err.to_string()).unwrap().into_raw();
    };

    let func = (STORE.lock().unwrap()[&name]).before_start.clone();
    func();

    let func = (STORE.lock().unwrap()[&name]).start.clone();
    let ret = func(&mut env, host);
    let ret = unsafe { CString::from_raw(ret) };
    env.new_string(ret.to_str().unwrap()).unwrap().into_raw()
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
        func(&mut env, host);
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

enum GitFetchMode {
    Clone,
    Pull,
}

fn git_fetch(
    mut env: JNIEnv,
    url: JString,
    branch: JString,
    path: JString,
    progress_listener: JObject,
    mode: GitFetchMode,
) -> jstring {
    let url: String = JavaStr::from_env(&env, &url).unwrap().into();
    let path: String = JavaStr::from_env(&env, &path).unwrap().into();
    let path = PathBuf::from(path);
    let branch: String = JavaStr::from_env(&env, &branch).unwrap().into();
    let env_ref = &mut env;

    if matches!(mode, GitFetchMode::Clone) {
        if let Err(e) = recreate_dir(&path) {
            return env.new_string(e.to_string()).unwrap().into_raw();
        }
    }

    let f = || -> Result<(), git2::Error> {
        unsafe {
            git2::opts::set_server_timeout_in_milliseconds(10 * 1000)?;
            git2::opts::set_server_connect_timeout_in_milliseconds(10 * 1000)?;
        }

        if matches!(mode, GitFetchMode::Clone) {
            let repo = git2::Repository::init(&path)?;
            repo.remote_set_url("origin", &url)?;
        }
        let repo = git2::Repository::open(&path)?;
        let mut remote = repo.find_remote("origin")?;

        let mut fetch_option = git2::FetchOptions::new();

        // TODO: we can't use depth=1 for pull,
        // otherwise the second pull will fail for "object not found - no match for id",
        // may be a libgit2 issue?
        if matches!(mode, GitFetchMode::Clone) {
            fetch_option.depth(1);
        }

        // pass progress to kotlin
        if !progress_listener.is_null() {
            fetch_option.remote_callbacks({
                let mut cb = git2::RemoteCallbacks::new();

                let mut prev_time = Instant::now();
                let mut prev_byte = 0;
                let mut byte_per_second = 0.;
                cb.transfer_progress(move |state| {
                    let time = Instant::now();
                    if (time - prev_time) > Duration::from_secs(1) {
                        let byte = state.received_bytes();
                        byte_per_second =
                            (byte - prev_byte) as f32 / (time - prev_time).as_secs_f32();
                        prev_byte = byte;
                        prev_time = time;
                    }
                    let percent = state.received_objects() as f32 / state.total_objects() as f32;
                    env_ref
                        .call_method(
                            &progress_listener,
                            "onUpdate",
                            "(FF)V",
                            &[percent.into(), byte_per_second.into()],
                        )
                        .unwrap();
                    true
                });

                // disable ssl cert check
                cb.certificate_check(|_, _| Ok(git2::CertificateCheckStatus::CertificateOk));
                cb
            });
        }

        remote.fetch(&[&branch], Some(&mut fetch_option), None)?;
        let fetch_head = repo.find_reference("FETCH_HEAD")?;
        let obj = fetch_head.peel(git2::ObjectType::Commit)?;
        repo.reset(&obj, git2::ResetType::Hard, None)?;
        Ok(())
    };

    let msg = match f() {
        Ok(_) => "".to_string(),
        Err(err) => err.to_string(),
    };
    env.new_string(msg).unwrap().into_raw()
}

#[no_mangle]
extern "C" fn Java_RemoteService_gitClone(
    env: JNIEnv,
    _: JClass,
    url: JString,
    branch: JString,
    path: JString,
    progress_listener: JObject,
) -> jstring {
    git_fetch(
        env,
        url,
        branch,
        path,
        progress_listener,
        GitFetchMode::Clone,
    )
}

#[no_mangle]
extern "C" fn Java_RemoteService_gitPull(
    env: JNIEnv,
    _: JClass,
    url: JString,
    branch: JString,
    path: JString,
    progress_listener: JObject,
) -> jstring {
    git_fetch(
        env,
        url,
        branch,
        path,
        progress_listener,
        GitFetchMode::Pull,
    )
}

#[no_mangle]
extern "C" fn Java_RemoteService_initHostLogger(_: JNIEnv, _: JClass) {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("gamebot"),
    );
}
