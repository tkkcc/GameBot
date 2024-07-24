use std::{collections::HashMap, marker::PhantomData, mem::discriminant};

use erased_serde::serialize_trait_object;
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

#[typetag::serialize]
trait View {}
// serialize_trait_object!(View);

#[derive(Serialize)]
pub(crate) struct Element<State> {
    #[serde(flatten)]
    item: Box<dyn View>,
    #[serde(skip)]
    ty: PhantomData<State>,
}

impl<State, T: View + 'static> From<T> for Element<State> {
    fn from(value: T) -> Self {
        Self {
            item: Box::new(value),
            ty: PhantomData,
        }
    }
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

#[derive(Serialize, Deserialize)]
struct Text {
    content: String,
}

#[typetag::serialize]
impl View for Text {}

fn text<State>(content: impl ToString) -> Element<State> {
    Text {
        content: content.to_string(),
    }
    .into()
}

#[derive(Serialize)]
struct Column<State> {
    content: Vec<Element<State>>,
}

#[typetag::serialize]
impl<State: Serialize> View for Column<State> {}

fn column<State>(x: impl IntoIterator<Item = Element<State>>) -> Element<State>
where
    State: 'static + Serialize,
{
    Column {
        content: x.into_iter().collect(),
    }
    .into()
}

#[derive(Serialize)]
struct TextField<State: Serialize> {
    content: String,
    #[serde(skip)]
    callback: Box<dyn Fn(State, String) -> ()>,
}
#[typetag::serialize]
impl<State: Serialize> View for TextField<State> {}

fn text_field<State, Callback, Content>(content: Content, callback: Callback) -> Element<State>
where
    State: 'static + Serialize,
    Callback: Fn(&mut State, String) -> () + 'static,
    Content: ToString,
{
    TextField {
        content: content.to_string(),
        callback: Box::new(callback),
    }
    .into()
}

#[derive(Serialize)]
struct Button<State: Serialize> {
    content: Element<State>,
    #[serde(skip)]
    callback: Box<dyn Fn(&mut State) -> ()>,
}
#[typetag::serialize]
impl<State: Serialize> View for Button<State> {}

fn button<State, Callback>(content: impl Into<Element<State>>, callback: Callback) -> Element<State>
where
    State: 'static + Serialize,
    Callback: Fn(&mut State) -> () + 'static,
{
    Button {
        content: content.into(),
        callback: Box::new(callback),
    }
    .into()
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

pub(crate) fn simple_ui() -> Element<Config> {
    fn view(state: &Config) -> Element<Config> {
        let layout = column::<Config>([
            button(&state.name, |state: &mut Config| state.enable_abc = true),
            button(text(&state.name), |state: &mut Config| {
                state.enable_abc = true
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
    let config = Config {
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
    };
    view(&config)
}
