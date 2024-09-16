use std::time::Instant;

use gamebot::{
    self, click_recent, d, take_nodeshot, take_screenshot, touch_down, touch_move, touch_up,
    wait_millis, wait_secs, ColorPoint, ColorPointGroup, ColorPointGroupIn, NodeSelector, Rect,
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
    touch_down(500.0, 500.0, 1);

    wait_millis(33);
    touch_move(500.0 - 10000.0, 500.0, 0);
    touch_move(500.0 + 10000.0, 500.0, 1);

    wait_millis(1000);
    touch_move(500.0, 500.0, 0);
    touch_move(500.0, 500.0, 1);

    wait_millis(33);
    touch_up(500.0, 500.0, 0);
    touch_up(500.0, 500.0, 1);
    // wait_millis(100);
    // touch_move(400.0, 500.0, 0);
    // touch_move(600.0, 500.0, 1);
    //
    // wait_millis(1000); // must
    // touch_up(400.0, 500.0, 0);
    // touch_up(600.0, 500.0, 1);
}

gamebot::entry!(start);
fn start() {
    error!("what");

    click_recent();
    wait_millis(100);
    click_recent();
    wait_secs(1);
    zoom();
    return;

    // take_nodeshot();

    // for n in take_nodeshot() {
    //     error!("68 {:?}", start.elapsed());
    //     if n.text().is_empty() {
    //         continue;
    //     }
    //     error!("{}", n.text());
    //     error!("69 {:?}", start.elapsed());
    // }

    let nodeshot = take_nodeshot();
    let screenshot = take_screenshot();

    d!(nodeshot.len());

    // let nodeshot = Store::proxy().take_nodeshot();
    let start = Instant::now();
    // for n in &nodeshot {
    //     if n.view_id().is_empty() {
    //         continue;
    //     }
    //     error!("node view id {}", n.view_id());
    // }

    for i in (0..100) {
        let x = ColorPointGroupIn {
            group: vec![ColorPoint::default(), ColorPoint::default()],
            region: Rect {
                left: 0,
                right: 720,
                top: 0,
                bottom: 1080,
            },
            tolerance: 0.05,
        };
        let y = ColorPointGroup {
            group: vec![ColorPoint::default(), ColorPoint::default()],
            tolerance: 0.05,
        };

        let mail = NodeSelector::new(|n| n.clickable && !n.id.is_empty()).find_all();
        // if let Some(n) = mail.find() {
        for n in mail {
            // d!(&n.text, &n.id, &n.clickable, &n.class);
            if n.id.eq("com.android.settings:id/search") {
                n.click();
            }
            // n.click();
        }
        break;
    }
    error!("{:?}", start.elapsed());

    // find_node(root_node().uwwrap(), |x| x.text() == "abc");
}
