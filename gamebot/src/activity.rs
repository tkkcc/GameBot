use std::default;

use serde::Deserialize;

#[derive(Deserialize, Debug)]
pub struct ActivityInfo {
    package: String,
    class: String,
}

#[derive(Deserialize, Debug)]
enum Importance {
    Foreground,
    ForegroundService,
    Visible,
    Service,
    CantSaveState,
    Cached,
    Gone,
    Perceptible,
    TopSleeping,
    Unknown,
}

#[derive(Deserialize, Debug)]
pub struct AppProcessInfo {
    process: String,
    importance: Importance,
}
