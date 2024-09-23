use std::{
    mem::take,
    ops::Deref,
    time::{Duration, Instant},
    u64,
};

use crate::{
    api::{
        take_nodeshot, take_nodeshot_after, take_screenshot, wait, wait_nodeshot_after,
        wait_screenshot_after, wait_secs, IntoSeconds,
    },
    color::{ColorPoint, ColorPointGroup, ColorPointGroupIn, DiskImageIn, ImageIn, Point},
    node::{ANode, Node, NodeSelector, Nodeshot},
    screenshot::Screenshot,
};

pub trait Find {
    type FindOut;

    fn find(&self) -> Option<Self::FindOut>;
    // fn appear(&self, timeout: Duration) -> bool;

    fn exist(&self) -> bool {
        self.find().is_some()
    }
    // fn appear_millis(&self, timeout: u64) -> bool {
    //     self.appear(Duration::from_millis(timeout))
    // }
    fn appear(&self, timeout: impl IntoSeconds) -> bool {
        self.appear(timeout.into_seconds())
    }
}

pub trait FindExt: Find {
    fn appear_secs(&self, timeout: impl IntoSeconds) -> bool {
        self.appear(timeout.into_seconds())
    }
}

// impl<R> FindExt for dyn Find<FindOut = R> {}

pub trait GroupFindOnce<'a, I: Find + 'a>: IntoIterator<Item = &'a I> {
    fn all_exist(self) -> bool;
    fn any_exist(self) -> bool;
}

pub trait GroupFind<'a, I: Find + 'a>: GroupFindOnce<'a, I> + Copy {
    fn all_appear(self, timeout: Duration) -> bool;
    // {
    //     wait_for_true(|| self.all_exist(), timeout, Duration::ZERO)
    // }
    fn any_appear(self, timeout: Duration) -> bool;
    // {
    //     wait_for_true(|| self.any_exist(), timeout, Duration::ZERO)
    // }

    fn all_appear_millis(self, timeout: u64) -> bool {
        self.all_appear(Duration::from_millis(timeout))
    }
    fn any_appear_millis(self, timeout: u64) -> bool {
        self.any_appear(Duration::from_millis(timeout))
    }
    fn all_appear_secs(self, timeout: impl IntoSeconds) -> bool {
        self.all_appear(timeout.into_seconds())
    }
    fn any_appear_secs(self, timeout: impl IntoSeconds) -> bool {
        self.any_appear(timeout.into_seconds())
    }
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
    fn all_appear(self, timeout: Duration) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter().all(|x| shot.find_color_point(x).is_some())
        })
    }

    fn any_appear(self, timeout: Duration) -> bool {
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
    fn all_appear(self, timeout: Duration) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter()
                .all(|x| shot.find_color_point_group(x).is_some())
        })
    }

    fn any_appear(self, timeout: Duration) -> bool {
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
    fn all_appear(self, timeout: Duration) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter()
                .all(|x| shot.find_color_point_group_in(x).is_some())
        })
    }

    fn any_appear(self, timeout: Duration) -> bool {
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
    fn all_appear(self, timeout: Duration) -> bool {
        appear_with_screenshot(timeout, |shot| {
            self.into_iter().all(|x| shot.find_image_in(x).is_some())
        })
    }

    fn any_appear(self, timeout: Duration) -> bool {
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
    fn all_appear(self, timeout: Duration) -> bool {
        let img: Vec<_> = self.into_iter().map(|x| ImageIn::from(x.clone())).collect();
        appear_with_screenshot(timeout, |shot| {
            img.iter().all(|x| shot.find_image_in(x).is_some())
        })
    }

    fn any_appear(self, timeout: Duration) -> bool {
        let img: Vec<_> = self.into_iter().map(|x| ImageIn::from(x.clone())).collect();
        appear_with_screenshot(timeout, |shot| {
            img.iter().any(|x| shot.find_image_in(x).is_some())
        })
    }
}

impl<'a, T: IntoIterator<Item = &'a NodeSelector>> GroupFindOnce<'a, NodeSelector> for T {
    fn all_exist(self) -> bool {
        let shot = take_nodeshot();
        self.into_iter().all(|x| shot.match_selector(x))
    }

    fn any_exist(self) -> bool {
        let shot = take_nodeshot();
        self.into_iter().any(|x| shot.match_selector(x))
    }
}

impl<'a, T: IntoIterator<Item = &'a NodeSelector> + Copy> GroupFind<'a, NodeSelector> for T {
    fn all_appear(self, timeout: Duration) -> bool {
        appear_with_nodeshot(timeout, |shot| {
            self.into_iter().all(|x| shot.match_selector(x))
        })
    }

    fn any_appear(self, timeout: Duration) -> bool {
        appear_with_nodeshot(timeout, |shot| {
            self.into_iter().any(|x| shot.match_selector(x))
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
    fn all_appear(self, timeout: Duration) -> bool {
        wait_for_true(
            || self.into_iter().all(|x| (x.cond)()),
            timeout,
            Duration::ZERO,
        )
    }

    fn any_appear(self, timeout: Duration) -> bool {
        wait_for_true(
            || self.into_iter().any(|x| (x.cond)()),
            timeout,
            Duration::ZERO,
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
    fn all_appear(self, timeout: Duration) -> bool {
        wait_for_true(
            || self.into_iter().all(|x| (x.cond)().is_some()),
            timeout,
            Duration::ZERO,
        )
    }

    fn any_appear(self, timeout: Duration) -> bool {
        wait_for_true(
            || self.into_iter().any(|x| (x.cond)().is_some()),
            timeout,
            Duration::ZERO,
        )
    }
}

fn wait_for<T>(
    mut func: impl FnMut() -> Option<T>,
    timeout: Duration,
    interval: Duration,
) -> Option<T> {
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
    timeout: impl IntoSeconds,
    interval: Duration,
) -> bool {
    let timeout = timeout.into_seconds();
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

fn appear_with_screenshot(timeout: impl IntoSeconds, f: impl Fn(&Screenshot) -> bool) -> bool {
    let timeout = timeout.into_seconds();
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

fn appear_with_nodeshot(timeout: impl IntoSeconds, f: impl Fn(&Nodeshot) -> bool) -> bool {
    let timeout = timeout.into_seconds();
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
    fn appear(&self, timeout: impl IntoSeconds) -> bool {
        appear_with_screenshot(timeout, |shot| shot.find_color_point(self).is_some())
    }
}

impl Find for ColorPointGroup {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        take_screenshot().find_color_point_group(self)
    }
    fn appear(&self, timeout: impl IntoSeconds) -> bool {
        appear_with_screenshot(timeout, |shot| shot.find_color_point_group(self).is_some())
    }
}

impl Find for ColorPointGroupIn {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        take_screenshot().find_color_point_group_in(self)
    }
    fn appear(&self, timeout: impl IntoSeconds) -> bool {
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
    fn appear(&self, timeout: impl IntoSeconds) -> bool {
        appear_with_screenshot(timeout, |shot| shot.find_image_in(self).is_some())
    }
}

impl Find for DiskImageIn {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        ImageIn::from(self.clone()).find()
    }
    fn appear(&self, timeout: impl IntoSeconds) -> bool {
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
        self.find()
    }

    fn appear(&self, timeout: impl IntoSeconds) -> bool {
        appear_with_nodeshot(timeout, |shot| shot.match_selector(self))
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

    fn appear(&self, timeout: impl IntoSeconds) -> bool {
        wait_for_true(|| self.evaluate(), timeout, Duration::ZERO)
    }
}

impl<T> Find for ConditionOption<T> {
    type FindOut = T;

    fn find(&self) -> Option<Self::FindOut> {
        self.evaluate()
    }

    fn appear(&self, timeout: impl IntoSeconds) -> bool {
        wait_for_true(|| self.evaluate().is_some(), timeout, Duration::ZERO)
    }
}

// pub trait GroupFindDynamic {
//     fn all_exist(&self) -> bool {
//         todo!()
//     }
//     fn any_exist(&self) -> bool {
//         todo!()
//     }
//     fn all_appear(&self, timeout: Duration) -> bool {
//         todo!()
//     }
//     fn any_appear(&self, timeout: Duration) -> bool {
//         todo!()
//     }
//     fn all_appear_millis(&self, timeout: u64) -> bool {
//         self.all_appear(Duration::from_millis(timeout))
//     }
//     fn any_appear_millis(&self, timeout: u64) -> bool {
//         self.any_appear(Duration::from_millis(timeout))
//     }
//     fn all_appear_secs(&self, timeout: impl IntoSeconds) -> bool {
//         self.all_appear(timeout.into_seconds())
//     }
//     fn any_appear_secs(&self, timeout: impl IntoSeconds) -> bool {
//         self.any_appear(timeout.into_seconds())
//     }
// }
//
// impl<R1, R2, T1, T2> GroupFindDynamic for (T1, T2)
// where
//     T1: Find<FindOut = R1>,
//     T2: Find<FindOut = R2>,
// {
//     fn all_exist(&self) -> bool {
//         self.0.exist() && self.1.exist()
//     }
// }
