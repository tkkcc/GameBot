use std::{
    cell::RefCell,
    ops::Deref,
    sync::{Arc, Weak},
};

use jni::objects::{GlobalRef, JObject};
use serde::Deserialize;

use crate::{
    api::{proxy, take_nodeshot},
    color::Rect,
};

static NODE_ACTION_CLICK: i32 = 0x00000010;

#[derive(Deserialize, Default)]
#[serde(default)]
pub struct NodeInfo {}

#[derive(Deserialize, Default, Debug)]
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
    pub parent_idx: usize,
    pub children_idx: Vec<usize>,
    #[serde(skip)]
    pub(crate) parent: RefCell<Weak<Node>>,
    #[serde(skip)]
    pub(crate) children: RefCell<Vec<ANode>>,
    #[serde(skip)]
    pub(crate) obj: RefCell<Option<GlobalRef>>,
}

#[derive(Clone, Debug)]
pub struct Nodeshot {
    pub data: Vec<ANode>,
    pub timestamp: i64,
}

impl Nodeshot {
    pub fn find_selector(&self, selector: &NodeSelector) -> Option<ANode> {
        self.data.iter().find(|x| (selector.filter)(x)).cloned()
    }
    pub fn find_all_selector(&self, selector: &NodeSelector) -> Vec<ANode> {
        self.data
            .iter()
            .filter(|x| (selector.filter)(x))
            .map(Clone::clone)
            .collect()
    }
}

#[derive(Clone, Debug)]
pub struct ANode(pub Arc<Node>);

impl Deref for ANode {
    type Target = Arc<Node>;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl ANode {
    pub fn find(&self, filter: impl Fn(&Node) -> bool) -> Option<ANode> {
        let mut stack = vec![self.clone()];
        while !stack.is_empty() {
            for node in std::mem::take(&mut stack) {
                if filter(&node) {
                    return Some(node);
                }
                stack.extend(node.children());
            }
        }
        None
    }
    pub fn find_all(&self, filter: impl Fn(&Node) -> bool) -> Vec<ANode> {
        let mut ans = vec![];
        let mut stack = vec![self.clone()];
        while !stack.is_empty() {
            for node in std::mem::take(&mut stack) {
                if filter(&node) {
                    ans.push(node.clone());
                }
                stack.extend(node.children());
            }
        }
        ans
    }
    pub fn parent(&self) -> Option<ANode> {
        self.parent.borrow().upgrade().map(|x| ANode(x))
    }
    pub fn children(&self) -> Vec<ANode> {
        self.children.borrow().iter().map(|x| x.clone()).collect()
    }
    pub fn click(&self) {
        let obj = self.obj.borrow();
        let obj: &JObject = obj.as_ref().unwrap();
        proxy().node_action(obj, NODE_ACTION_CLICK);
    }
}

pub struct NodeSelector {
    pub filter: Box<dyn Fn(&Node) -> bool>,
}

impl NodeSelector {
    pub fn new(filter: impl Fn(&Node) -> bool + 'static) -> Self {
        NodeSelector {
            filter: Box::new(filter),
        }
    }
    pub fn find_all(&self) -> Vec<ANode> {
        take_nodeshot().find_all_selector(&self)
    }
}
