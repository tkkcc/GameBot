use std::{
    mem::take,
    ops::Deref,
    sync::{atomic::AtomicU64, LazyLock},
    time::{Duration, Instant},
    u64,
};

use crate::{
    api::{
        take_nodeshot, take_nodeshot_after, take_screenshot, wait, wait_nodeshot_after,
        wait_screenshot_after, wait_secs, Seconds,
    },
    color::{ColorPoint, ColorPointGroup, ColorPointGroupIn, DiskImageIn, ImageIn, Point},
    node::{ANode, Node, NodeSelector, Nodeshot},
    screenshot::Screenshot,
};

static DEFAULT_WAIT_INTERVAL: Duration = Duration::from_millis(33);

pub trait Find {
    type FindOut;

    fn find(&self) -> Option<Self::FindOut>;
    fn exist(&self) -> bool {
        self.find().is_some()
    }
    fn appear(&self, timeout: impl Seconds) -> bool {
        self.appear(timeout.into_duration())
    }
}

pub trait GroupFindOnce<'a, I: Find + 'a>: IntoIterator<Item = &'a I> {
    fn all_exist(self) -> bool;
    fn any_exist(self) -> bool;
}

pub trait GroupFind<'a, I: Find + 'a>: GroupFindOnce<'a, I> + Copy {
    fn all_appear(self, timeout: impl Seconds) -> bool;
    fn any_appear(self, timeout: impl Seconds) -> bool;
}

impl<'a, T: IntoIterator<Item = &'a ColorPoint>> GroupFindOnce<'a, ColorPoint> for T {
    fn all_exist(self) -> bool {
        let shot = take_screenshot();
        self.into_iter().all(|x| shot.find_color_point(x).is_some())
    }

    fn any_exist(self) -> bool {
        let shot = take_screenshot();
        self.into_iter().any(|x| shot.find_color_point(x).is_some())
    }
}

impl<'a, T: IntoIterator<Item = &'a ColorPoint> + Copy> GroupFind<'a, ColorPoint> for T {
    fn all_appear(self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter().all(|x| shot.find_color_point(x).is_some())
        })
    }

    fn any_appear(self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter().any(|x| shot.find_color_point(x).is_some())
        })
    }
}

impl<'a, T: IntoIterator<Item = &'a ColorPointGroup>> GroupFindOnce<'a, ColorPointGroup> for T {
    fn all_exist(self) -> bool {
        let shot = take_screenshot();
        self.into_iter()
            .all(|x| shot.find_color_point_group(x).is_some())
    }

    fn any_exist(self) -> bool {
        let shot = take_screenshot();
        self.into_iter()
            .any(|x| shot.find_color_point_group(x).is_some())
    }
}

impl<'a, T: IntoIterator<Item = &'a ColorPointGroup> + Copy> GroupFind<'a, ColorPointGroup> for T {
    fn all_appear(self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter()
                .all(|x| shot.find_color_point_group(x).is_some())
        })
    }

    fn any_appear(self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter()
                .any(|x| shot.find_color_point_group(x).is_some())
        })
    }
}

impl<'a, T: IntoIterator<Item = &'a ColorPointGroupIn>> GroupFindOnce<'a, ColorPointGroupIn> for T {
    fn all_exist(self) -> bool {
        let shot = take_screenshot();
        self.into_iter()
            .all(|x| shot.find_color_point_group_in(x).is_some())
    }

    fn any_exist(self) -> bool {
        let shot = take_screenshot();
        self.into_iter()
            .any(|x| shot.find_color_point_group_in(x).is_some())
    }
}

impl<'a, T: IntoIterator<Item = &'a ColorPointGroupIn> + Copy> GroupFind<'a, ColorPointGroupIn>
    for T
{
    fn all_appear(self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter()
                .all(|x| shot.find_color_point_group_in(x).is_some())
        })
    }

    fn any_appear(self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter()
                .any(|x| shot.find_color_point_group_in(x).is_some())
        })
    }
}

impl<'a, T: IntoIterator<Item = &'a ImageIn>> GroupFindOnce<'a, ImageIn> for T {
    fn all_exist(self) -> bool {
        let shot = take_screenshot();
        self.into_iter().all(|x| shot.find_image_in(x).is_some())
    }

    fn any_exist(self) -> bool {
        let shot = take_screenshot();
        self.into_iter().any(|x| shot.find_image_in(x).is_some())
    }
}

impl<'a, T: IntoIterator<Item = &'a ImageIn> + Copy> GroupFind<'a, ImageIn> for T {
    fn all_appear(self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter().all(|x| shot.find_image_in(x).is_some())
        })
    }

    fn any_appear(self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter().any(|x| shot.find_image_in(x).is_some())
        })
    }
}

impl<'a, T: IntoIterator<Item = &'a DiskImageIn>> GroupFindOnce<'a, DiskImageIn> for T {
    fn all_exist(self) -> bool {
        let shot = take_screenshot();
        self.into_iter()
            .map(|x| ImageIn::from(x.clone()))
            .all(|x| shot.find_image_in(&x).is_some())
    }

    fn any_exist(self) -> bool {
        let shot = take_screenshot();
        self.into_iter()
            .map(|x| ImageIn::from(x.clone()))
            .any(|x| shot.find_image_in(&x).is_some())
    }
}

impl<'a, T: IntoIterator<Item = &'a DiskImageIn> + Copy> GroupFind<'a, DiskImageIn> for T {
    fn all_appear(self, timeout: impl Seconds) -> bool {
        let img: Vec<_> = self.into_iter().map(|x| ImageIn::from(x.clone())).collect();
        appear_with_screenshot(timeout, |shot| {
            img.iter().all(|x| shot.find_image_in(x).is_some())
        })
    }

    fn any_appear(self, timeout: impl Seconds) -> bool {
        let img: Vec<_> = self.into_iter().map(|x| ImageIn::from(x.clone())).collect();
        appear_with_screenshot(timeout, |shot| {
            img.iter().any(|x| shot.find_image_in(x).is_some())
        })
    }
}

impl<'a, T: IntoIterator<Item = &'a NodeSelector>> GroupFindOnce<'a, NodeSelector> for T {
    fn all_exist(self) -> bool {
        let shot = take_nodeshot();
        self.into_iter().all(|x| shot.find_selector(x).is_some())
    }

    fn any_exist(self) -> bool {
        let shot = take_nodeshot();
        self.into_iter().any(|x| shot.find_selector(x).is_some())
    }
}

impl<'a, T: IntoIterator<Item = &'a NodeSelector> + Copy> GroupFind<'a, NodeSelector> for T {
    fn all_appear(self, timeout: impl Seconds) -> bool {
        appear_with_nodeshot(timeout, |shot| {
            self.into_iter().all(|x| shot.find_selector(x).is_some())
        })
    }

    fn any_appear(self, timeout: impl Seconds) -> bool {
        appear_with_nodeshot(timeout, |shot| {
            self.into_iter().any(|x| shot.find_selector(x).is_some())
        })
    }
}

impl<'a, T: IntoIterator<Item = &'a Condition>> GroupFindOnce<'a, Condition> for T {
    fn all_exist(self) -> bool {
        self.into_iter().all(|x| (x.cond)())
    }

    fn any_exist(self) -> bool {
        self.into_iter().any(|x| (x.cond)())
    }
}

impl<'a, T: IntoIterator<Item = &'a Condition> + Copy> GroupFind<'a, Condition> for T {
    fn all_appear(self, timeout: impl Seconds) -> bool {
        wait_for_true(
            || self.into_iter().all(|x| (x.cond)()),
            timeout,
            DEFAULT_WAIT_INTERVAL,
        )
    }

    fn any_appear(self, timeout: impl Seconds) -> bool {
        wait_for_true(
            || self.into_iter().any(|x| (x.cond)()),
            timeout,
            DEFAULT_WAIT_INTERVAL,
        )
    }
}

impl<'a, T: IntoIterator<Item = &'a ConditionOption<R>>, R: 'a>
    GroupFindOnce<'a, ConditionOption<R>> for T
{
    fn all_exist(self) -> bool {
        self.into_iter().all(|x| (x.cond)().is_some())
    }

    fn any_exist(self) -> bool {
        self.into_iter().any(|x| (x.cond)().is_some())
    }
}

impl<'a, T: IntoIterator<Item = &'a ConditionOption<R>> + Copy, R: 'a>
    GroupFind<'a, ConditionOption<R>> for T
{
    fn all_appear(self, timeout: impl Seconds) -> bool {
        wait_for_true(
            || self.into_iter().all(|x| (x.cond)().is_some()),
            timeout,
            DEFAULT_WAIT_INTERVAL,
        )
    }

    fn any_appear(self, timeout: impl Seconds) -> bool {
        wait_for_true(
            || self.into_iter().any(|x| (x.cond)().is_some()),
            timeout,
            DEFAULT_WAIT_INTERVAL,
        )
    }
}

fn wait_for<T>(
    mut func: impl FnMut() -> Option<T>,
    timeout: impl Seconds,
    interval: impl Seconds,
) -> Option<T> {
    let timeout = timeout.into_duration();
    let interval = interval.into_duration();
    let start = std::time::Instant::now();
    loop {
        let per_loop_start = Instant::now();
        if start.elapsed() >= timeout {
            return None;
        }
        if let p @ Some(_) = func() {
            return p;
        }
        wait(interval.saturating_sub(per_loop_start.elapsed()));
    }
}

fn wait_for_true(
    mut func: impl FnMut() -> bool,
    timeout: impl Seconds,
    interval: impl Seconds,
) -> bool {
    let timeout = timeout.into_duration();
    let interval = interval.into_duration();
    let start = std::time::Instant::now();
    loop {
        let per_loop_start = Instant::now();
        if start.elapsed() >= timeout {
            return false;
        }
        if func() {
            return true;
        }
        wait(interval.saturating_sub(per_loop_start.elapsed()));
    }
}

fn appear_with_screenshot(timeout: impl Seconds, f: impl Fn(&Screenshot) -> bool) -> bool {
    let timeout = timeout.into_duration();
    let start = Instant::now();
    loop {
        if start.elapsed() > timeout {
            return false;
        }
        let shot = take_screenshot();
        if f(&shot) {
            return true;
        }
        wait_screenshot_after(shot.timestamp, timeout.saturating_sub(start.elapsed()));
    }
}

fn appear_with_nodeshot(timeout: impl Seconds, f: impl Fn(&Nodeshot) -> bool) -> bool {
    let timeout = timeout.into_duration();
    let start = Instant::now();
    loop {
        if start.elapsed() > timeout {
            return false;
        }
        let shot = take_nodeshot();
        if f(&shot) {
            return true;
        }
        wait_screenshot_after(shot.timestamp, timeout.saturating_sub(start.elapsed()));
    }
}

impl Find for ColorPoint {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        take_screenshot().find_color_point(self)
    }
    fn appear(&self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| shot.find_color_point(self).is_some())
    }
}

impl Find for ColorPointGroup {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        take_screenshot().find_color_point_group(self)
    }
    fn appear(&self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| shot.find_color_point_group(self).is_some())
    }
}

impl Find for ColorPointGroupIn {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        take_screenshot().find_color_point_group_in(self)
    }
    fn appear(&self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| {
            shot.find_color_point_group_in(self).is_some()
        })
    }
}

impl Find for ImageIn {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        take_screenshot().find_image_in(self)
    }
    fn appear(&self, timeout: impl Seconds) -> bool {
        appear_with_screenshot(timeout, |shot| shot.find_image_in(self).is_some())
    }
}

impl Find for DiskImageIn {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        ImageIn::from(self.clone()).find()
    }
    fn appear(&self, timeout: impl Seconds) -> bool {
        ImageIn::from(self.clone()).appear(timeout)
    }
}

impl ColorPointGroupIn {
    pub fn find_all(&self) -> Vec<Point> {
        take_screenshot().find_all_color_point_group_in(self, usize::MAX)
    }
}

impl ImageIn {
    fn find_all(&self) -> Vec<Point> {
        take_screenshot().find_all_image_in(self, usize::MAX)
    }
}

impl DiskImageIn {
    fn find_all(&self) -> Vec<Point> {
        ImageIn::from(self.clone()).find_all()
    }
}

impl Find for NodeSelector {
    type FindOut = ANode;

    fn find(&self) -> Option<Self::FindOut> {
        take_nodeshot().find_selector(self)
    }

    fn appear(&self, timeout: impl Seconds) -> bool {
        appear_with_nodeshot(timeout, |shot| shot.find_selector(self).is_some())
    }
}

struct Condition {
    pub cond: Box<dyn Fn() -> bool>,
}

impl Condition {
    fn new(cond: impl Fn() -> bool + 'static) -> Self {
        Self {
            cond: Box::new(cond),
        }
    }

    fn evaluate(&self) -> bool {
        (self.cond)()
    }
}

struct ConditionOption<T> {
    pub cond: Box<dyn Fn() -> Option<T>>,
}

impl<T> ConditionOption<T> {
    fn new(cond: impl Fn() -> Option<T> + 'static) -> Self {
        Self {
            cond: Box::new(cond),
        }
    }
    fn evaluate(&self) -> Option<T> {
        (self.cond)()
    }
}

impl Find for Condition {
    type FindOut = bool;

    fn find(&self) -> Option<Self::FindOut> {
        if self.evaluate() {
            Some(true)
        } else {
            None
        }
    }

    fn appear(&self, timeout: impl Seconds) -> bool {
        wait_for_true(|| self.evaluate(), timeout, DEFAULT_WAIT_INTERVAL)
    }
}

impl<T> Find for ConditionOption<T> {
    type FindOut = T;

    fn find(&self) -> Option<Self::FindOut> {
        self.evaluate()
    }

    fn appear(&self, timeout: impl Seconds) -> bool {
        wait_for_true(|| self.evaluate().is_some(), timeout, DEFAULT_WAIT_INTERVAL)
    }
}
