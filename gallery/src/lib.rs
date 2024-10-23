use std::collections::HashMap;

use gamebot::{
    d,
    ui::{button, col, nav_host, text, web_view, Element, NavController, Text, UIContext, UI},
};
use serde::Serialize;

#[derive(Serialize, Default, PartialEq, Eq, Hash, Clone)]
enum Screen {
    #[default]
    Main,
    Second,
}

#[derive(Serialize, Default)]
struct State {
    nav_controller: NavController<Screen>,
    info: String,
}

fn view(state: &mut State, ctx: UIContext<State>) -> Element<State> {
    let mut graph = HashMap::new();
    graph.insert(
        Screen::Main,
        col([
            text("content"),
            button("goto view2", |state: &mut State, _| {
                d!("button clicked");
                state.nav_controller.push(Screen::Second);
            }),
        ]),
    );
    graph.insert(Screen::Second, web_view("https://baidu.com"));
    nav_host(graph, state.nav_controller.clone())
}

pub fn main_ui() {
    let mut ui = UI::new(State::default(), view);
    ui.enter_render_loop();
}

pub fn start() {
    main_ui()
}

gamebot::entry!(start);
