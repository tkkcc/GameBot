use std::{thread, time::Instant};

use gamebot::{self, sleep, toast, toast2};
use log::error;

fn start() {
    error!("what");

    let start_time = Instant::now();
    for i in 0..100 {
        toast2("what1");
    }
    error!("toast2x {:?}", start_time.elapsed());

    thread::spawn(|| {
        toast("what2");
        sleep(1);
    });

    sleep(3);
    toast("what3");
}

gamebot::entry!(start);
