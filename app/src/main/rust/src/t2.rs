use std::{any::Any, collections::HashMap, marker::PhantomData, mem::discriminant};

// use erased_serde::serialize_trait_object;
use jni::{
    objects::{JClass, JObject},
    sys::JNIEnv,
};
use serde::{Deserialize, Serialize};
use strum::VariantArray;

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
pub(crate) struct Config {
    name: String,
    account: Vec<AccountConfig>,
    enable_abc: bool,
}
impl Config {
    fn name(&mut self, new: String) {
        self.name = new;
    }
}

// trait MutableState: erased_serde::Serialize {}
// serialize_trait_object!(MutableState);
// impl<T: erased_serde::Serialize> MutableState for T {}

#[typetag::serialize(tag = "type")]
trait View<State> {
    fn take_callback(&mut self) -> Option<Box<dyn Fn(&mut State, Box<dyn CallbackValue>)>> {
        None
    }
    fn set_callback_id(&mut self, id: usize) {}

    fn children_mut(&mut self) -> Vec<&mut Element<State>> {
        vec![]
    }

    fn to_element(self) -> Element<State>
    where
        Self: Sized + 'static,
    {
        Element {
            item: Box::new(self),
        }
    }
}

#[typetag::serde(tag = "type")]
pub(crate) trait CallbackValue: Any + std::fmt::Debug {}

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
pub(crate) struct Element<State> {
    #[serde(flatten)]
    item: Box<dyn View<State>>,
    // #[serde(skip)]
    // ty: PhantomData<State>,
}

impl<State: 'static> Element<State> {
    pub(crate) fn collect_callback(
        &mut self,
    ) -> Vec<Box<dyn Fn(&mut State, Box<dyn CallbackValue>)>> {
        let mut ans: Vec<Box<dyn Fn(&mut State, Box<dyn CallbackValue>)>> = vec![];
        let mut stack = vec![self];
        while !stack.is_empty() {
            for Element { item } in std::mem::take(&mut stack) {
                if let Some(callback) = item.take_callback() {
                    item.set_callback_id(ans.len());
                    ans.push(Box::new(
                        move |state: &mut State, new: Box<dyn CallbackValue>| {
                            callback(state, new);
                        },
                    ));
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
struct Text {
    content: String,
}

#[typetag::serialize]
impl<State> View<State> for Text {}

fn text<State>(content: impl ToString) -> Element<State> {
    Text {
        content: content.to_string(),
    }
    .to_element()
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
struct Column<State> {
    content: Vec<Element<State>>,
}

#[typetag::serialize]
impl<State: Serialize> View<State> for Column<State> {
    fn children_mut(&mut self) -> Vec<&mut Element<State>> {
        self.content.iter_mut().collect()
    }
}

fn column<State>(x: impl IntoIterator<Item = Element<State>>) -> Element<State>
where
    State: 'static + Serialize,
{
    Column {
        content: x.into_iter().collect(),
    }
    .to_element()
}

#[derive(Serialize)]
struct TextField<State: Serialize> {
    content: String,
    #[serde(skip)]
    callback: Box<dyn Fn(&mut State, String) -> ()>,
    #[serde(rename = "callbackId")]
    callback_id: usize,
}
#[typetag::serialize]
impl<State: Serialize + 'static> View<State> for TextField<State> {
    fn take_callback(&mut self) -> Option<Box<dyn Fn(&mut State, Box<dyn CallbackValue>)>> {
        let mut callback: Box<dyn Fn(&mut State, String) -> ()> = Box::new(|_, _| {});
        std::mem::swap(&mut self.callback, &mut callback);

        Some(Box::new(
            move |state: &mut State, new: Box<dyn CallbackValue>| {
                if let Ok(new) = (new as Box<dyn Any>).downcast::<String>() {
                    callback(state, *new);
                }
            },
        ))
    }
    fn set_callback_id(&mut self, id: usize) {
        self.callback_id = id
    }
}

fn text_field<State, Callback, Content>(content: Content, callback: Callback) -> Element<State>
where
    State: 'static + Serialize,
    Callback: Fn(&mut State, String) -> () + 'static,
    Content: ToString,
{
    TextField {
        callback_id: 0,
        content: content.to_string(),
        callback: Box::new(callback),
    }
    .to_element()
}

#[derive(Serialize)]
struct Button<State: Serialize> {
    content: Element<State>,
    #[serde(skip)]
    callback: Box<dyn Fn(&mut State) -> ()>,
    #[serde(rename = "callbackId")]
    callback_id: usize,
}
#[typetag::serialize]
impl<State: Serialize + 'static> View<State> for Button<State> {
    fn take_callback(&mut self) -> Option<Box<dyn Fn(&mut State, Box<dyn CallbackValue>)>> {
        let mut callback: Box<dyn Fn(&mut State) -> ()> = Box::new(|_| {});
        std::mem::swap(&mut self.callback, &mut callback);

        Some(Box::new(move |state: &mut State, _| {
            callback(state);
        }))
    }
    fn set_callback_id(&mut self, id: usize) {
        self.callback_id = id
    }
}

fn button<State, Callback>(content: impl Into<Element<State>>, callback: Callback) -> Element<State>
where
    State: 'static + Serialize,
    Callback: Fn(&mut State) -> () + 'static,
{
    Button {
        content: content.into(),
        callback: Box::new(callback),
        callback_id: 0,
    }
    .to_element()
}

struct UI<State> {
    callback: HashMap<u64, u64>,
    ty: PhantomData<State>,
}
impl<State> UI<State> {
    fn pull_event() -> Vec<i32> {
        // serde_json::from_slice(v)
        vec![]
    }

    fn display(element: Element<State>) {}

    fn show(default: State, view: impl Fn(&State) -> Element<State>) {
        let element = view(&default);
        Self::display(element);
    }
}

#[no_mangle]
extern "C" fn Java_gamebot_host_Native_callback(mut env: JNIEnv, class: JClass, msg: JObject) {}

pub(crate) fn simple_view(state: &Config) -> Element<Config> {
    let layout = column::<Config>([
        text(format!("state.enable_abc {}", state.enable_abc.to_string())),
        button(&state.name, |state: &mut Config| state.enable_abc = true),
        button(text(&state.name), |state: &mut Config| {
            state.enable_abc = false
        }),
        text_field(&state.name, |state: &mut Config, new| state.name = new),
        text_field(&state.name, Config::name),
        text("abc"),
    ]);
    // state.account_list.iter().enumerate().map(|i, account: AccountConfig| {
    //   section_row(account.title, account.info, checkbox(account.enable_abc, |state|state.account[i].enable_abc=new))
    // });
    layout
}

pub(crate) fn simple_config() -> Config {
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
