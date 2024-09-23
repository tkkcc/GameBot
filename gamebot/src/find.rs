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
    node::{ANode, Node, NodeSelector},
    screenshot::Screenshot,
};

pub trait Find {
    type FindOut;

    fn find(&self) -> Option<Self::FindOut>;
    fn appear(&self, timeout: Duration) -> bool;

    fn exist(&self) -> bool {
        self.find().is_some()
    }
    fn appear_millis(&self, timeout: u64) -> bool {
        self.appear(Duration::from_millis(timeout))
    }
}

pub trait FindExt: Find {
    fn appear_secs(&self, timeout: impl IntoSeconds) -> bool {
        self.appear(timeout.into_seconds())
    }
}

impl<R> FindExt for dyn Find<FindOut = R> {}

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
        self.into_iter()
            .all(|x| shot.data.iter().any(|y| (x.filter)(y)))
    }

    fn any_exist(self) -> bool {
        let shot = take_nodeshot();
        self.into_iter()
            .any(|x| shot.data.iter().any(|y| (x.filter)(y)))
    }
}

impl<'a, T: IntoIterator<Item = &'a NodeSelector> + Copy> GroupFind<'a, NodeSelector> for T {
    fn all_appear(self, timeout: Duration) -> bool {
        appear_with_nodeshot(timeout, |node| self.into_iter().all(|x| (x.filter)(node)))
    }

    fn any_appear(self, timeout: Duration) -> bool {
        appear_with_nodeshot(timeout, |node| self.into_iter().any(|x| (x.filter)(node)))
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

pub trait FindGroupDynamic {
    fn all_exist(&self) -> bool {
        todo!()
    }
    fn any_exist(&self) -> bool {
        todo!()
    }
    fn any_find_index(&self) -> Option<usize> {
        Some(0)
    }
}

impl<R1, R2, T1, T2> FindGroupDynamic for (T1, T2)
where
    T1: Find<FindOut = R1>,
    T2: Find<FindOut = R2>,
{
}

fn ttt() {
    fn a(x: &[ColorPoint]) {}
    let x = vec![ColorPoint::default()];
    x.all_appear_millis(300);
    x.any_appear_millis(300);
    x.any_exist();
    x.any_exist();

    let x1 = ColorPoint::default();
    let x = [&x1];
    x.all_exist();
    x.all_appear_secs(10);
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

fn wait_for_true(mut func: impl FnMut() -> bool, timeout: Duration, interval: Duration) -> bool {
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

fn appear_with_screenshot(timeout: Duration, f: impl Fn(&Screenshot) -> bool) -> bool {
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

fn appear_with_nodeshot(timeout: Duration, f: impl Fn(&Node) -> bool) -> bool {
    let start = Instant::now();
    loop {
        if start.elapsed() > timeout {
            return false;
        }
        let shot = take_nodeshot();
        if shot.data.iter().any(|x| f(x)) {
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
    fn appear(&self, timeout: Duration) -> bool {
        appear_with_screenshot(timeout, |shot| shot.find_color_point(self).is_some())
    }
}

impl Find for ColorPointGroup {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        take_screenshot().find_color_point_group(self)
    }
    fn appear(&self, timeout: Duration) -> bool {
        appear_with_screenshot(timeout, |shot| shot.find_color_point_group(self).is_some())
    }
}

impl Find for ColorPointGroupIn {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        take_screenshot().find_color_point_group_in(self)
    }
    fn appear(&self, timeout: Duration) -> bool {
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
    fn appear(&self, timeout: Duration) -> bool {
        appear_with_screenshot(timeout, |shot| shot.find_image_in(self).is_some())
    }
}

impl Find for DiskImageIn {
    type FindOut = Point;

    fn find(&self) -> Option<Self::FindOut> {
        ImageIn::from(self.clone()).find()
    }
    fn appear(&self, timeout: Duration) -> bool {
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

    fn appear(&self, timeout: Duration) -> bool {
        appear_with_nodeshot(timeout, |node| (self.filter)(node))
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
}

impl Find for Condition {
    type FindOut = bool;

    fn find(&self) -> Option<Self::FindOut> {
        if (self.cond)() {
            Some(true)
        } else {
            None
        }
    }

    fn appear(&self, timeout: Duration) -> bool {
        wait_for_true(|| (self.cond)(), timeout, Duration::ZERO)
    }
}

impl<T> Find for ConditionOption<T> {
    type FindOut = T;

    fn find(&self) -> Option<Self::FindOut> {
        (self.cond)()
    }

    fn appear(&self, timeout: Duration) -> bool {
        wait_for_true(|| (self.cond)().is_some(), timeout, Duration::ZERO)
    }
}
