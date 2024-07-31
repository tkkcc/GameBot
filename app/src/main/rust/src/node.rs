use serde::Deserialize;

use crate::mail::Rect;

#[derive(Deserialize, Default)]
#[serde(default)]
pub struct Node {
    pub id: String,
    pub region: Rect,
    pub text: String,
    pub class: String,
    pub package: String,
    pub description: String,
    pub checkable: bool,
    pub clickable: bool,
    pub long_clickable: bool,
    pub focusable: bool,
    pub scrollable: bool,
    pub visible: bool,
    pub checked: bool,
    pub enabled: bool,
    pub focused: bool,
    pub selected: bool,
    pub parent: usize,
    pub parent2: usize,
    pub children: Vec<usize>,
    pub index: usize,
}

pub struct Id(String);
pub struct Region(Rect);
pub struct Text(Rect);
pub struct Class(Rect);
pub struct Package(Rect);
pub struct Description(Rect);
pub struct Checkable(bool);
pub struct Clickable(bool);
pub struct LongClickable(bool);
pub struct Focusable(bool);
pub struct Visible(bool);
pub struct Checked(bool);
pub struct Enabled(bool);
pub struct Focused(bool);
pub struct Selected(bool);
