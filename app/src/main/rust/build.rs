use std::{
    env, fs,
    path::{Path, PathBuf},
};

fn main() {
    if env::var("CARGO_CFG_TARGET_OS").unwrap() == "android" {
        android();
    }
}

fn android() {
    // env::remove_var("E");

    // for k in env::vars() {
    //     println!("{k:?}");
    // }
    // panic!();

    // println!("cargo:rustc-link-lib=c++_shared");
    //
    // let output_path = env::var("CARGO_NDK_OUTPUT_PATH").unwrap();
    // let src_dir = PathBuf::from(env::var_os("CARGO_NDK_SYSROOT_LIBS_PATH").unwrap());
    // let dst_dir = Path::new(&output_path).join(&env::var("CARGO_NDK_ANDROID_TARGET").unwrap());
    // fs::create_dir_all(&dst_dir).unwrap();
    // fs::copy(src_dir.join("libc++_shared.so"), dst_dir.join("libc++_shared.so")).unwrap();
}

