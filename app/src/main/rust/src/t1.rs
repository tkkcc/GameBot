use std::cell::RefCell;

use slotmap::{new_key_type, SlotMap};

struct AccountConfig {
    username: String,
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
impl<'a> TextField<'a> {
    fn new(state: &'a mut String) -> Self {
        // insert a viewid into view storage
        let id = VIEW_STORAGE.with_borrow_mut(|s| s.insert(()));
        Self {
            id,
            text: state.to_string(),
            on_save: Box::new(|new| *state = new),
        }
    }
}

fn account_config_screen(state: &mut AccountConfig) {
    let col = vec![
        TextField::new(&mut state.username),
    ];
}

fn home_screen(state: &mut Config) {
    let col = state
        .account
        .iter_mut()
        .map(|account| account_config_screen(account))
        .collect::<Vec<_>>();
}
