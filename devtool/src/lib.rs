use std::{thread, time::Instant};

use gamebot::{
    self, click, click_recent, log, take_screenshot, toast, toast2, touch_down, touch_move,
    touch_up, wait_millis, wait_secs,
};
use log::error;

fn start() {
    error!("what");

    click_recent();
    wait_millis(500);
    click_recent();
    wait_secs(1);
    let start_time = Instant::now();

    touch_down(1000.0, 300.0, 0);
    wait_millis(200);
    touch_down(1000.0, 300.0, 1);
    wait_millis(200);
    // touch_move(0.0, 300.0, 0);
    // touch_move(2000.0, 300.0, 1);
    // let x2 = 500.0;
    // wait_millis(2000);
    // for i in (0..1000) {
    //     wait_millis(33);
    //     touch_move(1000.0 - i as f32 * 5.0, 300.0);
    // }
    // wait_millis(200);
    // touch_move(x2, 301.0);
    wait_millis(2000);
    touch_up(0.0, 300.0, 0);
    wait_millis(200);
    touch_up(2000.0, 300.0, 1);

    /// fight nav
    //
    // touch_down(1000.0, 300.0);
    // wait_millis(66); // must
    // touch_move(10000.0, 300.0);
    // wait_millis(66); // must
    // touch_up(10000.0, 300.0);
    //
    // touch_down(1000.0, 300.0);
    // wait_millis(66);
    // touch_move(-4000.0, 300.0);
    // wait_millis(66);
    // touch_move(-4000.0, 301.0);
    // wait_millis(66);
    // touch_up(-4000.0, 301.0);
    for i in (0..) {
        // let start_time = Instant::now();
        // click(400, 400);
        // log!(start_time.elapsed());
        // toast2("what1");
        // let screenshot = take_screenshot();
        // ssleep(1);
        break;

        // log!(screenshot);
        // break;

        // error!(screenshot);
        // error!(
        //     "{}x{} {}",
        //     screenshot.width,
        //     screenshot.height,
        //     screenshot.data.len()
        // );
    }

    error!("10000x {:?}", start_time.elapsed());

    thread::spawn(|| {
        toast("what2");
        wait_secs(1);
    });

    wait_secs(3);
    toast("what3");
}

gamebot::entry!(start);
