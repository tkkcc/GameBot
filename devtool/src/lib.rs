mod candle_ddddocr;
mod candle_onnx;
mod tract_onnx;
// mod ort_onnx;
// mod model;
// mod model;
// mod tract_onnx;

use std::{
    i64,
    mem::take,
    rc::Rc,
    thread,
    time::{Duration, Instant},
};

use axum::{routing::get, Router};
use candle_ddddocr::test_ddddocr_candle;
// use burn_onnx::test_burn_onnx;
use candle_onnx::test_candle_onnx;
use gamebot::{
    api::*,
    color::{ColorPoint, ColorPointGroup, ColorPointGroupIn, Region},
    d,
    find::GroupFind,
    node::NodeSelector,
    ui::{button, col, text, text_field, Element, UIContext, UI},
};
use log::error;
// use ort_onnx::test_ort_onnx;
use serde::Serialize;
use tokio::net::TcpListener;
use tract_onnx::test_ddddocr_tract;
// use tract_onnx::test_tract_onnx;

fn operator_swipe() {
    d!(1);
    let x0 = 1600.0;
    touch_down(x0, 100.0, 0);
    wait_millis(66);
    touch_move(x0, 200.0, 0); // this need and affect x distance, it tells unity to scroll
    wait_millis(200);
    for i in 1..3 {
        touch_move(x0 - 1080.0 * i as f32, 200.0, 0);
        wait_millis(2000);
    }
    touch_move(x0, 200.0, 0);
    wait_millis(1000);
    touch_up(x0, 200.0, 0);
}

fn infrastructure_overview_swipe() {
    let x0 = 1600.0;
    touch_down(x0, 500.0, 0);
    wait_millis(66);
    touch_move(x0 + 1000.0, 500.0, 0); // this need and affect distance, it tells unity to scroll
    wait_millis(200);
    for i in 1..12 {
        touch_move(x0, 500.0 - i as f32 * 500.0, 0);
        wait_millis(1000);
    }
    touch_move(x0, 500.0, 0);
    wait_millis(1000);
    touch_up(x0, 500.0, 0);
}

fn fight_swipe() {
    // swipe to first fight
    touch_down(1000.0, 300.0, 0);
    wait_millis(66); // must
    touch_move(10000.0, 300.0, 0);
    wait_millis(66); // must
    touch_up(10000.0, 300.0, 0);

    // nav
    touch_down(1000.0, 300.0, 0);
    wait_millis(66);
    touch_move(-4000.0, 300.0, 0);
    wait_millis(66);
    touch_move(-4000.0, 301.0, 0);
    wait_millis(66);
    touch_up(-4000.0, 301.0, 0);
}
fn zoom() {
    touch_down(500.0, 500.0, 0);
    touch_down(600.0, 500.0, 1);

    wait_millis(33);
    touch_move(500.0 - 100.0, 500.0, 0);
    touch_move(600.0 + 100.0, 500.0, 1);

    wait_millis(100);
    touch_move(500.0, 500.0, 0);
    touch_move(500.0, 500.0, 1);

    wait_millis(100);
    touch_up(500.0, 500.0, 0);
    touch_up(599.0, 500.0, 1);
    // wait_millis(100);
    // touch_move(400.0, 500.0, 0);
    // touch_move(600.0, 500.0, 1);
    //
    // wait_millis(1000); // must
    // touch_up(400.0, 500.0, 0);
    // touch_up(600.0, 500.0, 1);
}
fn zoom_by_gesture() {
    gesture(&[
        vec![
            (0, (500, 500)),
            (33, (500 - 100, 500)),
            (100, (500, 500)),
            (100, (500, 500)),
        ],
        vec![
            (0, (600, 500)),
            (33, (600 + 100, 500)),
            (100, (500, 500)),
            (100, (500, 500)),
        ],
    ]);
}
fn zoom_by_gesture_smooth() {
    gesture_smooth(&[
        vec![
            (0, (500, 500)),
            (1000, (500 - 100, 500)),
            (1000, (500, 500)),
            (100, (500, 500)),
        ],
        vec![
            (0, (600, 500)),
            (1000, (600 + 100, 500)),
            (1000, (600, 500)),
            (100, (600, 500)),
        ],
    ]);
}

fn float_move() {
    touch_down(500., 500., 0);
    for i in 0..10000 {
        wait_millis(500);
        touch_move(500.0 + 0.25 * ((i % 2) == 0) as i32 as f32, 500., 0);
    }
}

fn test_find() {
    let nodeshot = take_nodeshot();
    let screenshot = take_screenshot();
    let start = Instant::now();
    for i in 0..100 {
        let x = ColorPointGroupIn {
            group: vec![ColorPoint::default(), ColorPoint::default()],
            region: Region {
                left: 0,
                top: 0,
                width: 720,
                height: 1080,
            },
            tolerance: 0.05,
        };
        let y = ColorPointGroup {
            group: vec![ColorPoint::default(), ColorPoint::default()],
            tolerance: 0.05,
        };

        let mail = NodeSelector::new(|n| n.clickable && !n.id.is_empty()).find_all();
        for n in mail {
            if n.id.eq("com.android.settings:id/search") {
                n.click();
            }
        }
        break;
    }
    error!("{:?}", start.elapsed());
}

fn test_ui() {
    #[derive(Clone, Copy, Default, Serialize)]
    enum Server {
        #[default]
        Official,
        Bilibili,
        VVV,
    }

    #[derive(Default, Serialize, Clone)]
    struct AccountConfig {
        username: String,
        password: String,
        server: Server,
        id: usize,
    }
    #[derive(Default, Serialize, Clone)]
    pub struct Config {
        name: String,
        account: Vec<AccountConfig>,
        enable_abc: bool,
        launched: bool,
    }
    impl Config {
        fn change_name(&mut self, new: String) {
            self.name = new;
        }
    }

    pub fn simple_view(state: &mut Config, ui: UIContext<Config>) -> Element<Config> {
        if !state.launched {
            state.launched = true;
            ui.spawn(|ui| loop {
                wait(1);
                ui.update(|state| {
                    state.name += "1";
                })
            });
        }
        let layout = col([
            text(format!("state.enable_abc {}", state.enable_abc.to_string())),
            button(&state.name, |state: &mut Config, ui| {
                ui.update(|s| {
                    s.enable_abc = false;
                });
                ui.spawn(|ui| {
                    wait(1);
                    ui.update(|state| {
                        state.enable_abc = true;
                        state.name = "aaa".into();
                    });
                    wait(1);
                    ui.update(|state| {
                        state.enable_abc = false;
                    })
                });
                state.enable_abc = true;
                state.name = "true".into();
            }),
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
            launched: false,
        }
    }
    let mut ui = UI::new(simple_config(), simple_view);
    ui.enter_render_loop();
}

#[tokio::main]
async fn test_axum() {
    d!();
    tokio::spawn(async {
        loop {
            wait(1);
            // tokio::time::sleep(Duration::from_secs(1)).await;
            d!()
        }
    });
    let app = Router::new()
        .route("/slow", get(|| tokio::time::sleep(Duration::from_secs(5))))
        .route("/forever", get(std::future::pending::<()>));
    let listener = TcpListener::bind("0.0.0.0:3000").await.unwrap();

    tokio::select! {
        _ = async {wait_forever()} => {}
        _ = axum::serve(listener, app) => {}
    }
}

fn test_activity() {
    d!(current_activity());
    d!(running_activity_list());
    d!(running_app_process_list());
}

fn test_current_activity() {
    loop {
        let start = Instant::now();
        current_activity();
        // running_activity_list();
        d!(1, start.elapsed());
        // wait_secs(1);

        // let start = Instant::now();
        // take_nodeshot();
        // // d!(&take_nodeshot()[0]);
        // d!(2, start.elapsed());
        wait_millis(3);
    }
}

fn test_package() {
    for package in installed_package_list() {
        d!(&package.name, package.activity_list().first());
        if package.name.contains("android.settings") {
            // click_recent();
            // d!(&package.name);
            wait_millis(500);
            // let activity = &package.activity_list()[0];
            // start_activity(&package.name, &activity);
            start_package(&package.name);
            wait_millis(1500);
            stop_package(&package.name);
            wait_millis(500);
            break;
        }
    }
}

fn test_screenshot_after() {
    let shot = take_screenshot();
    let x = Instant::now();
    d!(x.elapsed());
    let y = take_screenshot_after(shot.timestamp, Duration::from_secs(1));
    d!(x.elapsed());

    thread::spawn(|| {
        wait(1);
        let shot = take_nodeshot();
        let shot = take_nodeshot_after(shot.timestamp, Duration::MAX);
        let shot = take_nodeshot_after(shot.timestamp, Duration::MAX);
        let shot = take_nodeshot_after(shot.timestamp, Duration::MAX);
        let shot = take_nodeshot_after(shot.timestamp, Duration::MAX);
    })
    .join()
    .unwrap();

    // let shot = take_nodeshot();
    // let x = Instant::now();
    // let y = take_nodeshot_after(shot.timestamp, Some(Duration::from_millis(i64::MAX as u64)));
    // d!(x.elapsed());
}

fn test_group_find() {
    let x = vec![ColorPointGroup::default()];
    x.all_appear(0.5);
}

fn test_ncnn() {
    ncnn::version();
}

gamebot::entry!(start);
fn start() {
    d!(ncnn::version());
    // test_ddddocr_candle().unwrap();
    // test_ddddocr_tract().unwrap();
    // test_burn_onnx();
    //
    // test_tract_onnx();

    // test_candle_onnx();
    // test_ort_onnx();
    // return;

    // click_recent();
    // wait_millis(100);
    // click_recent();
    // wait_secs(1);
    // test_ui();

    // test_axum();
    // test_activity();
    // test_current_activity();
    // test_package();
    // test_screenshot_after();
}
