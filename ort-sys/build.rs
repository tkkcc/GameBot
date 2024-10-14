use core::panic;
use std::{env, path::PathBuf};

fn download_and_link() {
    let version = "1.19.2";
    let name = format!("onnxruntime-android-{version}");
    let url = &format!("https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/{version}/{name}.aar");
    let cache_dir = dirs::cache_dir().unwrap().join("rust_cached_path");
    std::fs::create_dir_all(&cache_dir).unwrap();
    let cache = cached_path::CacheBuilder::new()
        .dir(cache_dir)
        .freshness_lifetime(u64::MAX)
        .build()
        .unwrap();
    let mut path = cache
        .cached_path_with_options(url, &cached_path::Options::default().extract())
        .unwrap_or_else(|_| panic!("fail to download ort library: {url}"));

    let target_arch = {
        let arch = env::var("CARGO_CFG_TARGET_ARCH").unwrap();
        match arch.as_str() {
            "x86" | "x86_64" => arch.to_string(),
            "arm" => "armeabi-v7a".to_owned(),
            "aarch64" => "arm64-v8a".to_owned(),
            _ => panic!("unexpect target_arch"),
        }
    };

    let header_path = path
        .join("headers")
        .join("onnxruntime_c_api.h")
        .into_os_string()
        .into_string()
        .unwrap();

    let library_path = path
        .join("jni")
        .join(&target_arch)
        .into_os_string()
        .into_string()
        .unwrap();

    let bindings = bindgen::Builder::default()
        .header(header_path)
        .parse_callbacks(Box::new(bindgen::CargoCallbacks::new()))
        .allowlist_type("Ort.*")
        .allowlist_type("Onnx.*")
        .allowlist_type("ONNX.*")
        .allowlist_function("Ort.*")
        .allowlist_var("ORT.*")
        .rustified_enum(".*")
        .generate()
        .expect("Unable to generate bindings");

    let out_path = PathBuf::from(env::var("OUT_DIR").unwrap());
    bindings
        .write_to_file(out_path.join("bindings.rs"))
        .expect("fail to write binding");
    // dbg!(out_path);
    // panic!();
    println!("cargo:rustc-link-search=native={}", library_path);
    println!("cargo:rustc-link-lib=dylib=onnxruntime");
}

fn main() {
    download_and_link();
}
