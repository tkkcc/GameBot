use std::{collections::HashMap, marker::PhantomData, mem::discriminant};

use erased_serde::serialize_trait_object;
use jni::{
    objects::{JClass, JObject},
    sys::JNIEnv,
};
use strum::VariantArray;

#[derive(VariantArray, Clone, Copy, Default)]
enum Server {
    #[default]
    Official,
    Bilibili,
    VVV,
}

#[derive(Default)]
struct AccountConfig {
    username: String,
    password: String,
    server: Server,
    id: usize,
}
#[derive(Default)]
struct Config {
    name: String,
    account: Vec<AccountConfig>,
    enable_abc: bool,
}
impl Config {
    fn name(&mut self, new: String) {
        self.name = new;
    }
}

trait MutableState: erased_serde::Serialize {}
serialize_trait_object!(MutableState);
impl<T: erased_serde::Serialize> MutableState for T {}

trait View {}

struct Element<State> {
    item: Box<dyn View>,
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

struct Text {
    content: String,
}
impl View for Text {}
fn text<State>(content: impl ToString) -> Element<State> {
    Text {
        content: content.to_string(),
    }
    .into()
}

// impl<State> From<&str> for Element<State> {
//     fn from(value: &str) -> Self {
//         text(value)
//     }
// }

struct Column<State> {
    content: Vec<Element<State>>,
}

impl<State> View for Column<State> {}
fn column<State>(x: impl IntoIterator<Item = Element<State>>) -> Element<State>
where
    State: 'static,
{
    Column {
        content: x.into_iter().collect(),
    }
    .into()
}

struct TextField<State> {
    content: String,
    callback: Box<dyn Fn(State, String) -> ()>,
}
impl<State> View for TextField<State> {}

fn text_field<State, Callback, Content>(content: Content, callback: Callback) -> Element<State>
where
    State: 'static,
    Callback: Fn(&mut State, String) -> () + 'static,
    Content: ToString,
{
    TextField {
        content: content.to_string(),
        callback: Box::new(callback),
    }
    .into()
}

struct Button<State> {
    content: Element<State>,
    callback: Box<dyn Fn(&mut State) -> ()>,
}
impl<State> View for Button<State> {}

fn button<State, Callback>(content: impl Into<Element<State>>, callback: Callback) -> Element<State>
where
    State: 'static,
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

fn t() {
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
    UI::show(Config::default(), view);
}
