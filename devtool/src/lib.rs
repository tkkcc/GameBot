use std::{any::Any, thread, time::Instant};

use gamebot::{
    self, click_recent, take_nodeshot, take_screenshot, touch_down, touch_move, touch_up,
    wait_millis, wait_secs, ColorPoint, ColorPointGroup, ColorPointGroupIn, Find, NodeSelector,
    Rect, Store,
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

gamebot::entry!(start);
fn start() {
    error!("what");

    click_recent();
    wait_millis(100);
    click_recent();
    wait_secs(1);

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
    let nodeshot = Store::proxy().take_nodeshot_in_kotlin2();
    let start = Instant::now();
    // for n in &nodeshot {
    //     if n.view_id().is_empty() {
    //         continue;
    //     }
    //     error!("node view id {}", n.view_id());
    // }

    for i in (0..10000).cycle() {
        // take_nodeshot();
        // let mut proxy = Store::proxy();
        // proxy.toast2("msg");
        // take_screenshot();
        // screenshot.

        // let x = ColorPointGroupIn {
        //     group: vec![ColorPoint::default(), ColorPoint::default()],
        //     region: Rect {
        //         left: 0,
        //         right: 720,
        //         top: 0,
        //         bottom: 1080,
        //     },
        //     tolerance: 0.05,
        // };
        // screenshot.find_all_color_point_group_in(&x, usize::MAX);

        // take_screenshot();
        // take_nodeshot_in_kotlin();
        let nodeshot = Store::proxy().take_nodeshot_in_kotlin2();

        let mail: Vec<_> = nodeshot
            .iter()
            .filter(|n| {
                n.text().to_ascii_lowercase().contains("mail")
                // n.text().to_ascii_lowercase().contains("mail")
                //     || n.view_id().contains("aa")
                //     || n.children().len() == 3
            })
            .collect();
        if !mail.is_empty() {
            error!("found mail");
        }
        wait_millis(33);

        // let node =
        //     NodeSelector::new(|node| node.text().to_ascii_lowercase().contains("dev")).find();
        // if let Some(node) = node {
        // error!("78, size:{} {:?}", nodeshot.len(), start.elapsed());
        // }
    }
    error!("{:?}", start.elapsed());

    // find_node(root_node().uwwrap(), |x| x.text() == "abc");
}
