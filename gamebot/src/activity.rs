use std::default;

use serde::Deserialize;

#[derive(Deserialize, Debug)]
pub struct ActivityInfo {
    pub package: String,
    pub class: String,
}

#[derive(Deserialize, Debug)]
pub enum Importance {
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
    pub process: String,
    pub importance: Importance,
}
