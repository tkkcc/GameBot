use std::sync::LazyLock;

use structz::stru;

use crate::{
    find_trait::Find,
    mail::{ColorPoint, ColorPointGroup, Point},
};

struct Resource {
    task: usize,
    mail: ColorPointGroup,
    home: Point,
    home_task: ColorPoint,
    func: Box<dyn FnMut() -> bool + Send + Sync>,
}

fn point(x: i32, y: i32) -> Point {
    Point { x, y }
}
fn cpg(s: &str) -> ColorPointGroup {
    ColorPointGroup::try_from(s).unwrap()
}
fn fun(f: impl FnMut() -> bool + Send + Sync + 'static) -> Box<dyn FnMut() -> bool + Send + Sync> {
    Box::new(f)
}

// let's wait https://github.com/rust-lang/rustfmt/issues/3863
static R: LazyLock<Resource> = LazyLock::new(|| Resource {
    task: 0,
    home: point(100, 100),
    #[rustfmt::skip]
    mail: cpg("maiiailmaiddddddddddddddddddlmailamailmailamailmailamailmailamailmailamailmailamailmailamaiiailmaiddddddddddddddddddlmailamailmailamailmailamailmailamailmailamailmailamailmailamaiiailmaiddddddddddddddddddlmailamailmailamailmailamailmailamailmailamailmailamailmailamaiiailmaiddddddddddddddddddlmailamailmailamailmailamailmailamailmailamailmailamailmaila"),
    func: fun(|| R.home_task.exist() && R.home_task.exist()),
    home_task: todo!(),
});

fn t() {
    // let (tx, rx) = std::sync::mpsc::channel();
    let rc = std::sync::Arc::new(0);

    let x = (&R.home, &R.home);
    dbg!(x);
    // let y = structx!(
    //     mail: "abc".to_string()
    //
    // );
    let ags = vec![0, 2];
}
