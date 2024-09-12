use jni::objects::{AutoLocal, JObject};
use serde::Deserialize;

use crate::{find_all_node, find_node, mail::Rect, root_node, Store};

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

pub struct Node2<'a> {
    pub inner: AutoLocal<'a, JObject<'a>>,
}

impl<'a> Node2<'a> {
    pub fn find(&self, filter: impl Fn(&Node2) -> bool) -> Option<Node2> {
        Store::proxy().find_node(self, filter)
    }
    pub fn find_all(&self, filter: impl Fn(&Node2) -> bool) -> Vec<Node2> {
        Store::proxy().find_all_node(self, filter)
    }
    pub fn id(&self) -> String {
        Store::proxy().get_node_id(self)
    }
    pub fn text(&self) -> String {
        todo!()
    }
    pub fn region(&self) -> Rect {
        todo!()
    }
    pub fn parent(&self) -> Option<Node2> {
        todo!()
    }
    pub fn children(&self) -> Vec<Node2> {
        todo!()
    }
    pub fn is_focused(&self) -> bool {
        todo!()
    }
    pub fn is_focusable(&self) -> bool {
        todo!()
    }
}

struct NodeSelector<'a> {
    pub filter: Box<dyn Fn(&Node2) -> bool + 'a>,
}
impl<'a> NodeSelector<'a> {
    fn new(filter: impl Fn(&Node2) -> bool + 'a) -> Self {
        NodeSelector {
            filter: Box::new(filter),
        }
    }
    fn find(&self) -> Option<Node2> {
        find_node(&self.filter)
    }
    fn find_all(&self) -> Vec<Node2> {
        find_all_node(&self.filter)
    }
}
