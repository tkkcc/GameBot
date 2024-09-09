use std::{thread, time::Instant};

use gamebot::{
    self, click, click_recent, log, msleep, ssleep, take_screenshot, toast, toast2, touch_down,
    touch_move, touch_up,
};
use log::error;

fn start() {
    error!("what");

    click_recent();
    msleep(500);
    click_recent();
    ssleep(3);
    let start_time = Instant::now();
    touch_down(1000, 300);
    msleep(200);
    touch_move(100, 300);
    msleep(200);
    touch_move(100, 301);
    msleep(200);
    touch_up(100, 301);

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
        ssleep(1);
    });

    ssleep(3);
    toast("what3");
}

gamebot::entry!(start);
