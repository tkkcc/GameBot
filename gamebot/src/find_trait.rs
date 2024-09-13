use std::sync::LazyLock;

use crate::{
    mail::{
        ColorPoint, ColorPointGroup, ColorPointGroupIn, DiskImageIn, ImageIn, IntoSeconds, Point,
    },
    take_screenshot,
};

pub trait Find {
    type Return;

    fn find(&self) -> Option<Self::Return>;

    fn exist(&self) -> bool {
        self.find().is_some()
    }
}

// pub trait FindAll {
//     type Return;
//     fn find_all(&self) -> Vec<Self::Return>;
// }

pub trait Appear {
    type Return;
    fn appear(&self, timeout: impl IntoSeconds) -> Option<Self::Return> {
        todo!()
    }
}

pub trait GroupFind {
    type T;

    fn all_exist(&self) -> bool {
        todo!()
    }
    fn any_exist(&self) -> bool {
        todo!()
    }

    fn all_find(&self) -> Option<Vec<Self::T>> {
        todo!()
    }

    fn any_find(&self) -> Option<Self::T> {
        todo!()
    }

    fn any_find_index(&self) -> Option<usize> {
        todo!()
    }
}

pub trait GroupAppear {
    type T;
    fn all_appear(&self, timeout: impl IntoSeconds) -> Option<Vec<Self::T>> {
        todo!()
    }

    fn any_appear(&self, timeout: impl IntoSeconds) -> Option<Self::T> {
        todo!()
    }

    fn any_appear_index(&self, timeout: impl IntoSeconds) -> Option<usize> {
        todo!()
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
    T1: Find<Return = R1>,
    T2: Find<Return = R2>,
{
}

impl<R> GroupFind for [Box<dyn Find<Return = R>>] {
    type T = R;
}

impl GroupFind for [ColorPoint] {
    type T = Point;
}
impl GroupAppear for [ColorPoint] {
    type T = Point;
}

fn t1() {
    let v = ColorPoint::default();
    let v2 = ColorPoint::default();
    let v3 = [Box::new(v) as Box<dyn Find<Return = Point>>];
    v2.find();
    v3.all_find();
    let x = [v2];
    x.all_find();
    x.all_appear(4);

    (ColorPoint::default(), ColorPointGroup::default()).any_find_index();

    let v22: LazyLock<ColorPoint> = LazyLock::new(|| ColorPoint::default());
    let v23: &ColorPoint = &v22;
}

impl Find for ColorPoint {
    type Return = Point;

    fn find(&self) -> Option<Self::Return> {
        take_screenshot().find_color_point(self)
    }
}

impl Find for ColorPointGroup {
    type Return = Point;

    fn find(&self) -> Option<Self::Return> {
        take_screenshot().find_color_point_group(self)
    }
}

impl Find for ColorPointGroupIn {
    type Return = Point;

    fn find(&self) -> Option<Self::Return> {
        take_screenshot().find_color_point_group_in(self)
    }
}

impl Find for ImageIn {
    type Return = Point;

    fn find(&self) -> Option<Self::Return> {
        take_screenshot().find_image_in(self)
    }
}

impl Find for DiskImageIn {
    type Return = Point;

    fn find(&self) -> Option<Self::Return> {
        ImageIn::from(self.clone()).find()
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

// TODO make it trait?
fn wait<T>(timeout: impl IntoSeconds, mut func: impl FnMut() -> Option<T>) -> Option<T> {
    let timeout = timeout.into_seconds();
    let start = std::time::Instant::now();
    loop {
        if start.elapsed() >= timeout {
            return None;
        }
        if let p @ Some(_) = func() {
            return p;
        }
    }
}
