// use data_downloader::DownloadRequest;
// use data_downloader::{self, get_path};
// use hex_literal::hex;

use core::panic;
use std::{env, path::PathBuf};

fn download_and_link() {
    let version = "20240820";
    let vulkan_suffix = if cfg!(feature = "vulkan") {
        "-vulkan"
    } else {
        ""
    };
    let static_link =
        cfg!(feature = "static-link") || env::var("CARGO_CFG_TARGET_OS").unwrap() != "android";
    let link_suffix = if static_link { "" } else { "-shared" };

    let name = format!("ncnn-{version}-android{vulkan_suffix}{link_suffix}",);
    let url = &format!("https://github.com/Tencent/ncnn/releases/download/{version}/{name}.zip");

    let cache_dir = dirs::cache_dir().unwrap().join("rust_cached_path");
    std::fs::create_dir_all(&cache_dir).unwrap();
    let cache = cached_path::CacheBuilder::new()
        .dir(cache_dir)
        .freshness_lifetime(u64::MAX)
        .build()
        .unwrap();
    let mut path = cache
        .cached_path_with_options(url, &cached_path::Options::default().extract())
        .unwrap_or_else(|_| panic!("fail to download ncnn library: {url}"));

    let target_arch = {
        let arch = env::var("CARGO_CFG_TARGET_ARCH").unwrap();
        match arch.as_str() {
            "x86" | "x86_64" => arch.to_string(),
            "arm" => "armeabi-v7a".to_owned(),
            "aarch64" => "arm64-v8a".to_owned(),
            _ => panic!("unexpect target_arch"),
        }
    };

    path.extend([&name, &target_arch]);

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

    if static_link {
        println!("cargo:rustc-link-lib=static=ncnn");
    } else {
        println!("cargo:rustc-link-lib=dylib=ncnn");
    }
}

fn main() {
    download_and_link();
}
