// use data_downloader::DownloadRequest;
// use data_downloader::{self, get_path};
// use hex_literal::hex;

use core::panic;
use std::{env, path::PathBuf};

use cached_path::cached_path_with_options;

fn download_link_static_lib() {
    let version = "20240820";
    let url = &format!(
        "https://github.com/Tencent/ncnn/releases/download/{version}/ncnn-{version}-android.zip"
    );
    let mut path = cached_path_with_options(url, &cached_path::Options::default().extract())
        .unwrap_or_else(|_| panic!("fail to download ncnn library: {url}"));

    // dbg!(env::var("CARGO_CFG_TARGET_ARCH"));
    // panic!();
    let target_arch = {
        let arch = env::var("CARGO_CFG_TARGET_ARCH").unwrap();
        match arch.as_str() {
            "x86" | "x86_64" => arch.to_string(),
            "arm" => "armeabi-v7a".to_owned(),
            "aarch64" => "arm64-v8a".to_owned(),
            _ => panic!("unexpect target_arch"),
        }
    };

    path.extend([&format!("ncnn-{version}-android"), &target_arch]);

    let header_path = path
        .join("include")
        .join("ncnn")
        .join("c_api.h")
        .into_os_string()
        .into_string()
        .unwrap();

    let library_path = path.join("lib").into_os_string().into_string().unwrap();

    let bindings = bindgen::Builder::default()
        .header(header_path)
        .parse_callbacks(Box::new(bindgen::CargoCallbacks::new()))
        .allowlist_type("regex")
        .allowlist_function("ncnn.*")
        .allowlist_var("NCNN.*")
        .allowlist_type("ncnn.*")
        .generate()
        .expect("Unable to generate bindings");

    let out_path = PathBuf::from(env::var("OUT_DIR").unwrap());
    bindings
        .write_to_file(out_path.join("bindings.rs"))
        .expect("fail to write binding");

    println!("cargo:rustc-link-search=native={}", library_path);
    println!("cargo:rustc-link-lib=static=ncnn");
}

fn main() {
    download_link_static_lib();
}
