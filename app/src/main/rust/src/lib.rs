use std::error::Error;

use git2::{CertificateCheckStatus, RemoteCallbacks};
use jni::{
    objects::{JClass, JString},
    sys::jstring,
    JNIEnv,
};
// use tracing::{debug, error, Subscriber};
// use tracing_subscriber::Registry;

pub fn add(left: usize, right: usize) -> usize {
    left + right
}

#[macro_use]
extern crate log;

// extern crate android_logger;

#[no_mangle]
extern "C" fn Java_bilabila_gamebot_host_MainActivity_test(
    mut env: JNIEnv,
    class: JClass,
    input: JString,
) -> jstring {
    // let subscriber = Registry::default();
    // use tracing_subscriber::layer::SubscriberExt;
    // let subscriber = subscriber.with(tracing_android::layer("com.example").unwrap());
    // tracing::subscriber::set_global_default(subscriber).unwrap();
    
    // android_logger::init_once(
    //     android_logger::Config::default()
    //         .with_max_level(log::LevelFilter::Trace)
    //         .with_tag("mytag")
    //         .with_filter(
    //             android_logger::FilterBuilder::new()
    //                 .parse("debug,hello::crate=trace")
    //                 .build(),
    //         ),
    // );

    std::env::set_var("SSL_CERT_DIR", "/system/etc/security/cacerts");

    fn f(input: String) -> Result<String, Box<dyn Error>> {
        error!("dddddddd");
        let url = "https://github.com/alexcrichton/git2-rs";
        // unsafe {
        //     // let _ = git2::opts::set_ssl_cert_dir("/system/etc/security/cacerts");
        //     let r = git2::opts::set_ssl_cert_dir("/system/etc/security/cacerts");
        //     debug!("{r:?}");
        //     debug!("26");
        // }
        let ans = String::new();

        // let mut callbacks = RemoteCallbacks::new();
        // callbacks.certificate_check(|_, _| Ok(CertificateCheckStatus::CertificateOk));
        // let mut fo = git2::FetchOptions::new();
        // fo.remote_callbacks(callbacks);
        // let mut builder = git2::build::RepoBuilder::new();
        // builder.fetch_options(fo);
        // let repo = builder.clone(url, input.as_ref())?;
        // let ans = repo.path().to_str().unwrap().to_string();


        // let repo = match Repository::clone(url, input) {
        //     Ok(repo) => repo,
        //     Err(e) => return Err(Box::new(e)),
        // };
        // let body = reqwest::blocking::get("https://www.rust-lang.org")?.text()?;
        // let ans = ans + &body;

        // let ans = String::from("1");
        Ok(ans)
    }

    let path = String::from(env.get_string(&input).unwrap());
    let out = format!("{:?}", f(path));
    env.new_string(out).unwrap().into_raw()
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
