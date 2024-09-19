use std::{
    any::Any,
    error::Error,
    sync::{Arc, Mutex, MutexGuard, TryLockError},
    thread::JoinHandle,
};

use serde::{Deserialize, Serialize};
use strum::VariantArray;

use crate::{d, CallbackMsg, Store};

#[derive(VariantArray, Clone, Copy, Default, Serialize)]
enum Server {
    #[default]
    Official,
    Bilibili,
    VVV,
}

#[derive(Default, Serialize)]
struct AccountConfig {
    username: String,
    password: String,
    server: Server,
    id: usize,
}
#[derive(Default, Serialize)]
pub struct Config {
    name: String,
    account: Vec<AccountConfig>,
    enable_abc: bool,
}
impl Config {
    fn change_name(&mut self, new: String) {
        self.name = new;
    }
}

// trait MutableState: erased_serde::Serialize {}
// serialize_trait_object!(MutableState);
// impl<T: erased_serde::Serialize> MutableState for T {}

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
pub trait CallbackValue: Any + std::fmt::Debug {}

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

#[derive(Debug, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum UIEvent {
    ReRender,
    Exit,
    Callback {
        id: usize,
        value: Box<dyn CallbackValue>,
    },
}

type CallbackFunc<State> = Box<dyn FnOnce(&mut State, Box<dyn CallbackValue>, AUI<State>) + Send>;

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

pub fn column<State>(content: impl IntoIterator<Item = Element<State>>) -> Element<State>
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
    Callback: FnOnce(&mut State, String, AUI<State>) + Send + 'static,
    Content: ToString,
{
    TextField {
        callback_id: 0,
        content: content.to_string(),
        callback: Some(Box::new(
            move |state: &mut State, new: Box<dyn CallbackValue>, ui: AUI<State>| {
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
    Callback: Fn(&mut State, AUI<State>) + Send + 'static,
{
    Button {
        content: content.into(),
        callback: Some(Box::new(
            move |state: &mut State, new: Box<dyn CallbackValue>, ui: AUI<State>| {
                callback(state, ui);
            },
        )),
        callback_id: 0,
    }
    .into_element()
}

pub fn simple_view(state: &Config) -> Element<Config> {
    let layout = column([
        text(format!("state.enable_abc {}", state.enable_abc.to_string())),
        button(&state.name, |state: &mut Config, _| state.enable_abc = true),
        button(text(&state.name), |state: &mut Config, _| {
            state.enable_abc = false
        }),
        text_field(&state.name, |state: &mut Config, new, _| state.name = new),
        text_field(&state.name, |state, new, _| {
            Config::change_name(state, new);
        }),
        text("abc"),
    ]);
    // state.account_list.iter().enumerate().map(|i, account: AccountConfig| {
    //   section_row(account.title, account.info, checkbox(account.enable_abc, |state|state.account[i].enable_abc=new))
    // });
    layout
}

pub fn simple_config() -> Config {
    Config {
        account: vec![
            AccountConfig {
                username: "use1".into(),
                password: "paww1".into(),
                id: 0,
                ..Default::default()
            },
            AccountConfig {
                username: "use2".into(),
                password: "paww2".into(),
                id: 1,
                ..Default::default()
            },
        ],
        name: "what my name".into(),
        enable_abc: true,
    }
}

#[derive(Clone)]
pub struct AUI<State>(Arc<Mutex<UI<State>>>);

pub struct UI<State> {
    state: State,
    view: Box<dyn Fn(&State) -> Element<State> + Send>,
    callback: Vec<CallbackFunc<State>>,
    is_in_render_loop: bool,
}

impl<State: Serialize + Clone + Send + 'static> AUI<State> {
    pub fn new(state: State, view: impl Fn(&State) -> Element<State> + Send + 'static) -> Self {
        AUI(Arc::new(Mutex::new(UI {
            state,
            view: Box::new(view),
            callback: vec![],
            is_in_render_loop: false,
        })))
    }

    fn commit(&self) {
        let mut ui = self.ui();
        let mut view = (ui.view)(&ui.state);
        ui.callback = view.collect_callback();
        Store::proxy().set_config_ui(view);
    }

    fn ui(&self) -> MutexGuard<UI<State>> {
        self.0.lock().unwrap()
    }

    pub fn into_state(self) -> State {
        Arc::into_inner(self.0).unwrap().into_inner().unwrap().state
    }

    pub fn enter_render_loop(&self) {
        // ensure single render loop
        {
            let Ok(mut ui) = self.0.try_lock() else {
                return;
            };
            if ui.is_in_render_loop {
                return;
            };
            ui.is_in_render_loop = true;
        }

        loop {
            self.commit();

            match Store::proxy().wait_config_ui_event() {
                UIEvent::ReRender => continue,
                UIEvent::Exit => break,
                UIEvent::Callback { id, value } => {
                    let mut ui = self.ui();
                    let f = ui.callback.swap_remove(id);
                    f(&mut ui.state, value, self.clone());
                }
            };
        }

        self.ui().is_in_render_loop = false;
    }

    pub fn exit_render_loop(&self) {
        // if self.0.try_lock().is_err() {
        //     self.spawn(|ui| {
        //         ui.exit_render_loop();
        //     });
        //     return;
        // }

        // Store::proxy().send_exit_config_ui_event();
    }

    pub fn update<F: FnOnce(&mut State) + Send + 'static>(&self, f: F) {
        // if during lock, like event handler
        // if self.0.try_lock().is_err() {
        //     self.spawn(move |ui| {
        //         ui.update(f);
        //     });
        //     return;
        // }

        f(&mut self.ui().state);

        // if lock hold in show, it's ok
        // if lock hold in handle_event, it's ok too, callback is still valid
        // if at proxy.wait_config_ui_event, we need to trigger an event
        Store::proxy().send_re_render_config_ui_event();
    }

    pub fn spawn<F: FnOnce(AUI<State>) + Send + 'static>(&self, f: F) -> JoinHandle<()> {
        let ctx: AUI<State> = self.clone();
        let handler = std::thread::spawn(move || {
            f(ctx);
        });
        handler
    }
}
