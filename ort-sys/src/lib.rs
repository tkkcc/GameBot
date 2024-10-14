#![allow(non_upper_case_globals)]
#![allow(non_camel_case_types)]
#![allow(non_snake_case)]

include!(concat!(env!("OUT_DIR"), "/bindings.rs"));

// https://github.com/pykeio/ort/blob/2e1f0143772319ca29db1771fd7d24c3680dc998/ort-sys/src/lib.rs#L17C1-L27C27
pub use std::ffi::{c_char, c_int, c_ulong, c_ulonglong, c_ushort, c_void};

// #[cfg(target_os = "windows")]
// pub type ortchar = c_ushort;
// #[cfg(not(target_os = "windows"))]
pub type ortchar = c_char;

// #[cfg(any(target_arch = "x86_64", target_arch = "x86"))]
pub type size_t = usize;

// #[cfg(all(target_arch = "aarch64", target_os = "windows"))]
// pub type size_t = c_ulonglong;
// #[cfg(all(
//     any(target_arch = "aarch64", target_arch = "arm"),
//     not(target_os = "windows")
// ))]
// pub type size_t = c_ulong;
