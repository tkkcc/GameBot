use std::{any::Any, cell::RefCell};

use slotmap::{new_key_type, DefaultKey, SlotMap};
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
struct Config {
    account: Vec<AccountConfig>,
    enable_abc: bool,
}

new_key_type! {
    struct ViewId;
}
thread_local! {
  static VIEW_STORAGE : RefCell<SlotMap<ViewId, ()>> = Default::default()
}

struct TextField<'a> {
    text: String,
    id: ViewId,
    on_save: Box<dyn FnMut(String) -> () + 'a>,
}

fn text_field(state: &mut String) -> TextField {
    // insert a viewid into view storage
    let id = VIEW_STORAGE.with_borrow_mut(|s| s.insert(()));
    TextField {
        id,
        text: state.to_string(),
        on_save: Box::new(|new| *state = new),
    }
}

struct Column<'a> {
    children: Vec<Box<dyn View + 'a>>,
}

macro_rules! col {
    ($($e:expr),* $(,)?) => {
      Column {
        children: vec![
            $(Box::new($e) as Box<dyn View>),*
        ]
      }
    };
}

struct DropdownMenu<'a> {
    candicates: Vec<String>,
    select: String,
    on_save: Box<dyn FnMut(usize) -> () + 'a>,
}

fn dropdown_menu<T: Copy>(
    select: &mut T,
    candicates: Vec<T>,
    display: impl Fn(&T) -> String,
) -> DropdownMenu {
    DropdownMenu {
        candicates: candicates.iter().map(&display).collect(),
        select: display(select),
        on_save: Box::new(move |idx| *select = candicates[idx]),
    }
}

trait View {}

impl<'a> View for TextField<'a> {}

impl<'a> View for Column<'a> {}
impl<'a> View for DropdownMenu<'a> {}

fn account_config_screen(state: &mut AccountConfig) -> impl View + '_ {
    let c = col![
        text_field(&mut state.username),
        text_field(&mut state.password),
        dropdown_menu(&mut state.server, Server::VARIANTS.into(), |server| {
            match server {
                Server::Official => "官服",
                Server::Bilibili => "B服",
                Server::VVV => "what",
            }
            .into()
        }),
        dropdown_menu(&mut state.id, (0..9).collect(), |s| { "".into() })
    ];
    c
}

fn home_screen(state: &mut Config) {
    let col = state
        .account
        .iter_mut()
        .map(|account| account_config_screen(account))
        .collect::<Vec<_>>();
}

#[cfg(test)]
mod test {

    use std::{fmt::Debug, io};

    use erased_serde::serialize_trait_object;
    use serde::{Serialize, Serializer};
    use slotmap::{DefaultKey, SlotMap};

    use super::AccountConfig;

    #[test]
    fn test() {
        let mut m = SlotMap::new();
        let k1 = m.insert(String::from(""));
        let k2 = m.insert("".into());
        let x1 = &mut m[k1];
        *x1 = "abc".into();
        let x2 = &mut m[k2];
        *x2 = "abc".into();

        let mut s = AccountConfig::default();

        #[derive(Serialize)]
        enum Statable {
            U32(u32),
            I32(i32),
            String(String),
        }

        s.username = String::from("abc");
        s.password = String::from("abc");
        let mut x = Statable::String(s.username);
        let mut m = SlotMap::<DefaultKey, &mut Statable>::new();
        let k1 = m.insert(&mut x);

        dbg!(serde_json::to_string(&m));

        trait Statable2: erased_serde::Serialize + Debug {}
        impl Statable2 for String {}
        impl Statable2 for u8 {}
        impl Statable2 for u32 {}

        serialize_trait_object!(Statable2);

        let mut x1 = String::from("abc");
        let mut x2 = 0u8;
        let mut x3 = 0u32;
        let mut m = SlotMap::<DefaultKey, &mut dyn Statable2>::new();
        let k1 = m.insert(&mut x1);
        let k1 = m.insert(&mut x2);
        let k1 = m.insert(&mut x3);

        dbg!(serde_json::to_string(&m));

        // let json = &mut serde_json::Serializer::new(io::stdout());
        // let mut json = Box::new(<dyn erased_serde::Serializer>::erase(json));
        // dbg!(&m[k1]);
        // dbg!(&m[k1].erased_serialize(&mut json));
    }
}
