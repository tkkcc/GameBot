mod dbg;
pub mod entry;
mod proxy;
mod status;
mod store;

use std::{
    ops::Range,
    path::PathBuf,
    time::{Duration, Instant},
};

use status::{assert_running_status, Status, STATUS_TOKEN};
use store::Store;

use crate::{
    activity::{ActivityInfo, AppProcessInfo, PackageInfo},
    color::{ColorPointGroup, DiskImageIn, ImageIn, Region},
    d,
    node::{ANode, Nodeshot},
    screenshot::Screenshot,
};

pub(crate) fn proxy() -> proxy::Proxy {
    Store::proxy()
}

pub fn toast(msg: &str) {
    proxy().toast(msg);
}

pub fn take_screenshot() -> Screenshot {
    proxy().take_screenshot()
}
pub fn wait_screenshot_after(timestamp: i64, timeout: Duration) {
    proxy().wait_screenshot_after(timestamp, timeout)
}
pub fn take_screenshot_after(timestamp: i64, timeout: Duration) -> Screenshot {
    wait_screenshot_after(timestamp, timeout);
    take_screenshot()
}

pub fn wait_nodeshot_after(timestamp: i64, timeout: Duration) {
    proxy().wait_nodeshot_after(timestamp, timeout)
}

pub fn take_nodeshot_after(timestamp: i64, timeout: Duration) -> Nodeshot {
    wait_nodeshot_after(timestamp, timeout);
    take_nodeshot()
}

pub fn click(x: f32, y: f32) {
    touch_down(x, y, 0);
    touch_up(x, y, 0);
}
pub fn touch_down(x: f32, y: f32, id: i32) {
    proxy().touch_down(x, y, id);
}
pub fn touch_up(x: f32, y: f32, id: i32) {
    proxy().touch_up(x, y, id);
}
pub fn touch_move(x: f32, y: f32, id: i32) {
    proxy().touch_move(x, y, id);
}
pub fn click_recent() {
    proxy().click_recent();
}

pub fn take_nodeshot() -> Nodeshot {
    proxy().take_nodeshot()
}

pub fn wait_forever() {
    let _ = STATUS_TOKEN.wait(Status::Running as u32);
    assert_running_status();
}

pub fn wait(s: impl Seconds) {
    let _ = STATUS_TOKEN.wait_for(Status::Running as u32, s.into_duration());
    assert_running_status();
}

pub fn wait_millis(s: u64) {
    wait(Duration::from_millis(s));
}

pub mod ease {
    pub type EaseFunc = fn(f32) -> f32;

    pub fn linear(t: f32) -> f32 {
        t
    }

    // https://docs.rs/simple-easing/latest/src/simple_easing/cubic.rs.html#12-18
    pub fn cubic_in_out(t: f32) -> f32 {
        if t < 0.5 {
            4.0 * t * t * t
        } else {
            1.0 - (-2.0 * t + 2.0).powi(3) / 2.0
        }
    }
}

pub fn test_gesture() {
    // multi finger
    gesture(&[
        vec![],
        vec![(0, (600, 500)), (500, (1000, 0)), (100, (1000, 0))],
        vec![],
        vec![(100, (600, 500)), (500, (1000, 0)), (100, (1000, 0))],
        vec![],
        vec![],
    ]);
}

pub fn gesture(path: &[Vec<(u64, (i32, i32))>]) {
    #[derive(Debug)]
    enum Action {
        Down,
        Up,
        Move,
    }

    #[derive(Debug)]
    struct PointEvent {
        x: f32,
        y: f32,
        id: i32,
        action: Action,
        time: Duration,
    }
    let mut point_list: Vec<_> = path
        .iter()
        .enumerate()
        .flat_map(|(i, path)| {
            let mut ans = vec![];
            let mut time = Duration::ZERO;
            for (j, &(delay, (x, y))) in path.iter().enumerate() {
                time += Duration::from_millis(delay);
                let point = move |action: Action| PointEvent {
                    x: x as _,
                    y: y as _,
                    id: i as i32,
                    action,
                    time,
                };
                if j == 0 {
                    ans.push(point(Action::Down));
                }
                if j > 0 {
                    ans.push(point(Action::Move));
                }

                if j + 1 == path.len() {
                    ans.push(point(Action::Up));
                }
            }
            ans
        })
        .collect();

    point_list.sort_by_key(|e| e.time);

    let start = Instant::now();
    for PointEvent {
        x,
        y,
        id,
        action,
        time,
    } in point_list
    {
        wait(time.saturating_sub(start.elapsed()));

        // d!(start.elapsed(), x, y, id, &action);

        match action {
            Action::Down => touch_down(x, y, id),
            Action::Up => touch_up(x, y, id),
            Action::Move => touch_move(x, y, id),
        }
    }
}

pub fn gesture_smooth(path: &[Vec<(u64, (i32, i32))>]) {
    gesture_interpolated(path, ease::cubic_in_out, Duration::from_millis(33))
}

pub fn gesture_interpolated(
    path: &[Vec<(u64, (i32, i32))>],
    ease_func: ease::EaseFunc,
    sample_interval: Duration,
) {
    #[derive(Debug)]
    struct MoveEvent {
        x1: f32,
        y1: f32,
        x2: f32,
        y2: f32,
        id: i32,
        time: Range<Duration>,
    }

    #[derive(Debug)]
    enum Action {
        Down,
        Up,
    }

    #[derive(Debug)]
    struct UpDownEvent {
        x: f32,
        y: f32,
        id: i32,
        action: Action,
        time: Duration,
    }

    let mut updown_event = vec![];
    let mut move_event = vec![];

    for (i, path) in path.iter().enumerate() {
        let mut time = Duration::ZERO;
        for (j, &(delay, (x, y))) in path.iter().enumerate() {
            time += Duration::from_millis(delay);

            if j > 0 {
                move_event.push(MoveEvent {
                    x1: path[j - 1].1 .0 as f32,
                    y1: path[j - 1].1 .1 as f32,
                    x2: x as f32,
                    y2: y as f32,
                    id: i as _,
                    time: time - Duration::from_millis(delay)..time,
                });
            }

            let updown = move |action: Action| UpDownEvent {
                x: x as _,
                y: y as _,
                id: i as i32,
                action,
                time,
            };
            if j == 0 {
                updown_event.push(updown(Action::Down));
            }
            if j + 1 == path.len() {
                updown_event.push(updown(Action::Up));
            }
        }
    }

    updown_event.sort_by_key(|e| e.time);
    move_event.sort_by_key(|e| e.time.end);
    let mut updown_event = updown_event.as_slice();
    let mut move_event = move_event.as_slice();
    let Some(mut next_wake_time) = updown_event.first().map(|x| x.time) else {
        return;
    };
    let start = Instant::now();

    loop {
        // d!(move_event.len(), move_event);
        // break;
        // d!(next_wake_time, start.elapsed());
        wait(next_wake_time.saturating_sub(start.elapsed()));
        let i = updown_event
            .iter()
            .position(|e| e.time > start.elapsed())
            .unwrap_or(updown_event.len());

        for &UpDownEvent {
            x,
            y,
            id,
            ref action,
            ..
        } in &updown_event[..i]
        {
            // d!("updown", x, y, id, action);
            match action {
                Action::Down => touch_down(x, y, id),
                Action::Up => {
                    // touch_move(x, y, id);
                    touch_up(x, y, id)
                }
            }
        }
        updown_event = &updown_event[i..];

        let mut i = 0;
        for (
            j,
            &MoveEvent {
                x1,
                y1,
                x2,
                y2,
                id,
                ref time,
            },
        ) in move_event.iter().enumerate()
        {
            let now = start.elapsed();
            if time.end <= now {
                i = j;
            }
            if time.contains(&now) {
                let t = (now - time.start).as_secs_f32()
                    / ((time.end - time.start).as_secs_f32() + 1e-6);
                let t = ease_func(t);
                let x = x1 + (x2 - x1) * t;
                let y = y1 + (y2 - y1) * t;
                d!("move", x, y, id);
                touch_move(x, y, id);
            }
        }
        move_event = &move_event[i..];

        if move_event.is_empty() && updown_event.is_empty() {
            break;
        } else if move_event.is_empty() {
            next_wake_time = updown_event.first().unwrap().time;
        } else if updown_event.is_empty() {
            next_wake_time += sample_interval;
        } else {
            next_wake_time = updown_event
                .first()
                .unwrap()
                .time
                .min(next_wake_time + sample_interval);
        }
    }
}

pub trait Seconds {
    fn into_duration(self) -> Duration;
}
impl Seconds for Duration {
    fn into_duration(self) -> Duration {
        self
    }
}
impl Seconds for u64 {
    fn into_duration(self) -> Duration {
        Duration::from_secs(self)
    }
}
impl Seconds for f64 {
    fn into_duration(self) -> Duration {
        Duration::from_secs_f64(self)
    }
}
impl Seconds for f32 {
    fn into_duration(self) -> Duration {
        Duration::from_secs_f32(self)
    }
}

// pub trait IntoMilliseconds {
//     fn into_milliseconds(self) -> Duration;
// }
// impl IntoMilliseconds for u64 {
//     fn into_milliseconds(self) -> Duration {
//         Duration::from_millis(self)
//     }
// }
// impl IntoMilliseconds for Duration {
//     fn into_milliseconds(self) -> Duration {
//         self
//     }
// }

pub fn running_app_process_list() -> Vec<AppProcessInfo> {
    proxy().running_app_process_list()
}
pub fn running_activity_list() -> Vec<ActivityInfo> {
    proxy().running_activity_list()
}
pub fn current_activity() -> ActivityInfo {
    proxy().current_activity()
}
pub fn installed_package_list() -> Vec<PackageInfo> {
    proxy().installed_package_list()
}
pub fn activity_list(package: &str) -> Vec<String> {
    proxy().activity_list(package)
}
pub fn start_package(package: &str) {
    let class = proxy().package_launch_activity(package);
    start_activity(package, &class);
}
pub fn start_activity(package: &str, class: &str) {
    let _ = std::process::Command::new("am")
        .args(["start", &format!("{package}/{class}")])
        .spawn();
}
pub fn stop_package(package: &str) {
    let _ = std::process::Command::new("am")
        .args(["force-stop", package])
        .spawn();
}

pub fn screen_width() -> usize {
    0
}
pub fn screen_height() -> usize {
    0
}

pub fn fullscreen_region() -> Region {
    Region {
        left: 0,
        top: 0,
        width: screen_width() as _,
        height: screen_height() as _,
    }
}
pub fn img(path: impl ToString) -> ImageIn {
    return DiskImageIn {
        img: PathBuf::from(path.to_string()),
        region: fullscreen_region(),
        tolerance: crate::color::Tolerance::MAE(0.0),
    }
    .into();
}

pub fn cpg(color: &str) -> ColorPointGroup {
    ColorPointGroup::try_from(color).unwrap()
}
