use std::{thread, time::Instant};

use gamebot::{self, click, log, ssleep, take_screenshot, toast, toast2};
use log::error;

fn start() {
    error!("what");

    // ssleep(3);
    let start_time = Instant::now();
    for i in (0..) {
        let start_time = Instant::now();
        click(400, 400);
        log!(start_time.elapsed());
        // toast2("what1");
        // let screenshot = take_screenshot();
        ssleep(1);

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
