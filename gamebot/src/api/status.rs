use std::sync::LazyLock;

use linux_futex::{Futex, Private};

pub(crate) static STATUS_TOKEN: LazyLock<Futex<Private>> =
    LazyLock::new(|| Futex::new(Status::Stopped as u32));

pub enum Status {
    Stopped = 0,
    Running = 1,
}

pub fn get_status() -> Status {
    let i = STATUS_TOKEN
        .value
        .load(std::sync::atomic::Ordering::Relaxed);

    match i {
        0 => Status::Stopped,
        1 => Status::Running,
        _ => panic!(),
    }
}

pub fn set_status(status: Status) {
    STATUS_TOKEN
        .value
        .store(status as u32, std::sync::atomic::Ordering::Relaxed);
}

pub fn set_running_status() {
    set_status(Status::Running)
}
pub fn set_stopped_status() {
    set_status(Status::Stopped)
}

pub fn is_running_status() -> bool {
    matches!(get_status(), Status::Running)
}
pub fn is_stopped_status() -> bool {
    matches!(get_status(), Status::Stopped)
}

pub fn check_running_status() {
    if !matches!(get_status(), Status::Running) {
        panic!();
    }
}
