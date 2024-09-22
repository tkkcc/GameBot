use std::default;

use serde::Deserialize;

use crate::api::{activity_list, start_activity};

#[derive(Deserialize, Debug)]
pub struct ActivityInfo {
    pub package: String,
    pub class: String,
}
impl ActivityInfo {
    pub fn start(&self) {
        start_activity(&self.package, &self.class);
    }
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

#[derive(Deserialize, Debug)]
pub struct PackageInfo {
    pub name: String,
    pub version: String,
    // #[serde(default)]
    // pub activity_list: Vec<String>,
}

impl PackageInfo {
    pub fn activity_list(&self) -> Vec<String> {
        activity_list(&self.name)
    }
}
