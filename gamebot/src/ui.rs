use std::{
    any::Any,
    collections::HashMap,
    default,
    marker::PhantomData,
    sync::mpsc::{Receiver, Sender},
    thread::JoinHandle,
};

use serde::{Deserialize, Serialize};

use crate::api::proxy;

#[typetag::serialize(tag = "type")]
trait View<State> {
    fn take_callback(&mut self) -> Option<CallbackFunc<State>> {
        None
    }
    fn set_callback_id(&mut self, _: usize) {}

    fn children_mut(&mut self) -> Vec<&mut Element<State>> {
        vec![]
    }

    fn into_element(self) -> Element<State>
    where
        Self: Sized + 'static,
    {
        Element {
            item: Box::new(self),
        }
    }
}

#[typetag::serde(tag = "type")]
pub trait CallbackValue: Any + std::fmt::Debug + Send {}

#[typetag::serde()]
impl CallbackValue for usize {}

#[typetag::serde()]
impl CallbackValue for isize {}

#[typetag::serde()]
impl CallbackValue for f64 {}

#[typetag::serde(name = "unit")]
impl CallbackValue for () {}

#[typetag::serde()]
impl CallbackValue for bool {}

#[typetag::serde(name = "string")]
impl CallbackValue for String {}

#[derive(Serialize)]
pub struct Element<State> {
    #[serde(flatten)]
    item: Box<dyn View<State>>,
    // #[serde(skip)]
    // ty: PhantomData<State>,
}

#[derive(Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum UIEvent<State> {
    Empty,
    Exit,
    Callback {
        id: usize,
        value: Box<dyn CallbackValue>,
    },
    #[serde(skip)]
    Update(Box<dyn FnOnce(&mut State) + Send>),
}

type CallbackFunc<State> = Box<dyn Fn(&mut State, Box<dyn CallbackValue>, UIContext<State>) + Send>;

impl<State> Element<State> {
    pub fn collect_callback(&mut self) -> Vec<CallbackFunc<State>> {
        let mut ans: Vec<CallbackFunc<State>> = vec![];
        let mut stack = vec![self];
        while !stack.is_empty() {
            for Element { item } in std::mem::take(&mut stack) {
                if let Some(callback) = item.take_callback() {
                    item.set_callback_id(ans.len());
                    ans.push(callback);
                }
                stack.extend(item.children_mut());
            }
        }
        ans
    }
}

// impl<State, T: View<State> + 'static> From<T> for Element<State> {
//     fn from(value: T) -> Self {
//         Self {
//             item: Box::new(value),
//             // ty: PhantomData,
//         }
//     }
// }

#[derive(Serialize)]
pub struct Text {
    content: String,
}

#[typetag::serialize]
impl<State> View<State> for Text {}

pub fn text<State>(content: impl ToString) -> Element<State> {
    Text {
        content: content.to_string(),
    }
    .into_element()
}

impl<State> From<&str> for Element<State> {
    fn from(value: &str) -> Self {
        text(value)
    }
}

impl<State> From<&String> for Element<State> {
    fn from(value: &String) -> Self {
        text(value)
    }
}

impl<State> From<String> for Element<State> {
    fn from(value: String) -> Self {
        text(value)
    }
}

#[derive(Serialize)]
pub struct Column<State> {
    content: Vec<Element<State>>,
}

#[typetag::serialize]
impl<State: Serialize> View<State> for Column<State> {
    fn children_mut(&mut self) -> Vec<&mut Element<State>> {
        self.content.iter_mut().collect()
    }
}

pub fn col<State>(content: impl IntoIterator<Item = Element<State>>) -> Element<State>
where
    State: 'static + Serialize,
{
    Column {
        content: content.into_iter().collect(),
    }
    .into_element()
}

#[derive(Serialize)]
pub struct TextField<State: Serialize> {
    content: String,
    #[serde(skip)]
    callback: Option<CallbackFunc<State>>,
    #[serde(rename = "callbackId")]
    callback_id: usize,
}

#[typetag::serialize]
impl<State: Serialize> View<State> for TextField<State> {
    fn take_callback(&mut self) -> Option<CallbackFunc<State>> {
        self.callback.take()
    }
    fn set_callback_id(&mut self, id: usize) {
        self.callback_id = id
    }
}

pub fn text_field<State, Callback, Content>(content: Content, callback: Callback) -> Element<State>
where
    State: 'static + Serialize,
    Callback: Fn(&mut State, String, UIContext<State>) + Send + 'static,
    Content: ToString,
{
    TextField {
        callback_id: 0,
        content: content.to_string(),
        callback: Some(Box::new(
            move |state: &mut State, new: Box<dyn CallbackValue>, ui: UIContext<State>| {
                if let Ok(new) = (new as Box<dyn Any>).downcast::<String>() {
                    callback(state, *new, ui)
                }
            },
        )),
    }
    .into_element()
}

#[derive(Serialize)]
pub struct Button<State: Serialize> {
    content: Element<State>,
    #[serde(skip)]
    callback: Option<CallbackFunc<State>>,
    #[serde(rename = "callbackId")]
    callback_id: usize,
}
#[typetag::serialize]
impl<State: Serialize + 'static> View<State> for Button<State> {
    fn take_callback(&mut self) -> Option<CallbackFunc<State>> {
        self.callback.take()
    }
    fn set_callback_id(&mut self, id: usize) {
        self.callback_id = id
    }
}

pub fn button<State, Callback>(
    content: impl Into<Element<State>>,
    callback: Callback,
) -> Element<State>
where
    State: 'static + Serialize,
    Callback: Fn(&mut State, UIContext<State>) + Send + 'static,
{
    Button {
        content: content.into(),
        callback: Some(Box::new(
            move |state: &mut State, new: Box<dyn CallbackValue>, ui: UIContext<State>| {
                callback(state, ui);
            },
        )),
        callback_id: 0,
    }
    .into_element()
}

pub struct UI<State> {
    state: State,
    view: Box<dyn Fn(&mut State, UIContext<State>) -> Element<State>>,
    callback: Vec<CallbackFunc<State>>,
    event_receiver: Receiver<UIEvent<State>>,
    event_sender: Sender<UIEvent<State>>,
}

// #[derive(Clone)]
pub struct UIContext<State> {
    event_sender: Sender<UIEvent<State>>,
}

impl<State: Send + 'static> UIContext<State> {
    pub fn rerender(&self) {
        proxy().send_empty_config_ui_event()
    }
    pub fn exit(&self) {
        self.event_sender.send(UIEvent::Exit).unwrap();
        self.rerender();
    }
    pub fn update(&self, f: impl FnOnce(&mut State) + Send + 'static) {
        self.event_sender
            .send(UIEvent::Update(Box::new(f)))
            .unwrap();
        self.rerender();
    }
    pub fn spawn(&self, f: impl FnOnce(UIContext<State>) + Send + 'static) -> JoinHandle<()> {
        let ctx = UIContext {
            event_sender: self.event_sender.clone(),
        };
        std::thread::spawn(move || f(ctx))
    }
}

impl<State: Serialize> UI<State> {
    pub fn new(
        state: State,
        view: impl Fn(&mut State, UIContext<State>) -> Element<State> + 'static,
    ) -> Self {
        let (event_sender, event_receiver) = std::sync::mpsc::channel();
        UI {
            state,
            view: Box::new(view),
            callback: vec![],
            event_receiver,
            event_sender,
        }
    }

    fn render(&mut self) {
        let mut view = (self.view)(
            &mut self.state,
            UIContext {
                event_sender: self.event_sender.clone(),
            },
        );
        self.callback = view.collect_callback();
        proxy().set_config_ui(view);
    }

    pub fn into_state(self) -> State {
        self.state
    }

    pub fn enter_render_loop(&mut self) {
        'outer: loop {
            self.render();

            let event = proxy()
                .wait_config_ui_event()
                .into_iter()
                .chain(self.event_receiver.try_iter());

            for event in event {
                match event {
                    UIEvent::Empty => continue,
                    UIEvent::Exit => break 'outer,
                    UIEvent::Callback { id, value } => (self.callback[id])(
                        &mut self.state,
                        value,
                        UIContext {
                            event_sender: self.event_sender.clone(),
                        },
                    ),
                    UIEvent::Update(f) => f(&mut self.state),
                }
            }
        }
    }
}

#[derive(Serialize, Default, Clone)]
#[serde(tag = "type")]
pub enum NavHostEvent<Key: Default> {
    Push {
        id: i32,
        destination: Key,
    },
    Pop {
        id: i32,
    },
    #[default]
    None,
}

#[derive(Serialize)]
pub struct NavHost<Key: Serialize + Default, State> {
    children: HashMap<Key, Element<State>>,

    #[serde(flatten)]
    controller: NavController<Key>,
}

#[typetag::serialize]
impl<Key: Serialize + Default, State: Serialize> View<State> for NavHost<Key, State> {
    fn children_mut(&mut self) -> Vec<&mut Element<State>> {
        self.children.values_mut().collect()
    }
}

#[derive(Serialize, Default, Clone)]
pub struct NavController<Key: Serialize + Default> {
    pub start: Key,
    #[serde(rename = "oneTimeEvent")]
    pub one_time_event: NavHostEvent<Key>,
    #[serde(skip)]
    pub id: i32,
}
impl<Key: Serialize + Default> NavController<Key> {
    pub fn push(&mut self, destination: Key) {
        self.id += 1;
        self.one_time_event = NavHostEvent::Push {
            id: self.id,
            destination,
        }
    }
    pub fn pop(&mut self) {
        self.id += 1;
        self.one_time_event = NavHostEvent::Pop { id: self.id }
    }
}

pub fn nav_host<Key: Default, State>(
    children: HashMap<Key, Element<State>>,
    controller: NavController<Key>,
) -> Element<State>
where
    Key: 'static + Serialize,
    State: 'static + Serialize,
{
    NavHost {
        children,
        controller,
    }
    .into_element()
}

#[derive(Serialize)]
struct WebView<State> {
    url: String,
    #[serde(skip)]
    ty: PhantomData<State>,
}

#[typetag::serialize]
impl<State: Serialize> View<State> for WebView<State> {}

pub fn web_view<State: 'static + Serialize>(url: impl ToString) -> Element<State> {
    WebView {
        url: url.to_string(),
        ty: PhantomData,
    }
    .into_element()
}
