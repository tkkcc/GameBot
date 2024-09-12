use std::{thread, time::Instant};

use gamebot::{
    self, click, click_recent, log, root_node, take_nodeshot, take_nodeshot_in_kotlin,
    take_screenshot, toast, toast2, touch_down, touch_move, touch_up, wait_millis, wait_secs,
    NodeSelector,
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
    wait_millis(500);
    click_recent();
    wait_secs(1);

    // take_nodeshot();
    let start = Instant::now();

    // for n in take_nodeshot() {
    //     error!("68 {:?}", start.elapsed());
    //     if n.text().is_empty() {
    //         continue;
    //     }
    //     error!("{}", n.text());
    //     error!("69 {:?}", start.elapsed());
    // }

    for i in 0..10 {
        take_nodeshot_in_kotlin();
        // take_nodeshot();
        // let node =
        //     NodeSelector::new(|node| node.text().to_ascii_lowercase().contains("dev")).find();
        // if let Some(node) = node {
        error!("78,  {:?}", start.elapsed());
        // }
    }
    error!("{:?}", start.elapsed());

    // find_node(root_node().uwwrap(), |x| x.text() == "abc");
}
