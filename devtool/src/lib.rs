use std::time::{Duration, Instant};

use gamebot::{
    self, click_recent, d, gesture, gesture_smooth, simple_config, simple_view, take_nodeshot,
    take_screenshot, touch_down, touch_move, touch_up, update_config_ui, wait_millis, wait_secs,
    ColorPoint, ColorPointGroup, ColorPointGroupIn, NodeSelector, Region,
};
use log::error;

fn operator_swipe() {
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
    for i in (0..100) {
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

gamebot::entry!(start);
fn start() {
    // click_recent();
    // wait_millis(100);
    // click_recent();
    // wait_secs(1);

    let mut state = simple_config();
    loop {
        update_config_ui(&mut state, simple_view);
    }
}
