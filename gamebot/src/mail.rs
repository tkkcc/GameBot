use std::{
    fs,
    path::{Path, PathBuf},
    sync::LazyLock,
    thread::sleep,
    time::Duration,
};

use serde::Deserialize;
use static_toml::static_toml;

enum Error {}

type Result<T> = std::result::Result<T, Error>;

#[derive(Debug, Clone, Copy)]
pub struct Point {
    pub x: u32,
    pub y: u32,
}

impl Point {
    // only return jni error
    fn click(&self) -> Result<()> {
        // let btn = ColorPointGroup::from_str("#FFFFFF,0,0|#AAAAAA,100,100").at([100, 100]);
        //
        // let btn =
        //     cpg("#FFFFFF,0,0|#AAAAAA,100,100,#FFFFFF,0,0|#AAAAAA,100,100,0,0|#AAAAAA,100,100")
        //         .at([100, 100, 200, 200]);

        // let btn =
        //     cpg("#FFFFFF,0,0|#AAAAAA,100,100,#FFFFFF,0,0|#AAAAAA,100,100,0,0|#AAAAAA,100,100#FFFFFF,0,0|#AAAAAA,100,100,#FFFFFF,0,0|#AAAAAA,100,100,0,0|#AAAAAA,100,100");
        fn fff() -> Result<u8> {
            Ok(0)
        }

        fn fff2() -> Option<u8> {
            Some(0)
        }

        // fs::read("path");
        fff2();

        // TODO: click via jni
        Ok(())
    }
}

struct Img(String);

#[derive(Default, Clone)]
pub struct ColorPoint {
    pub red: u8,
    pub green: u8,
    pub blue: u8,
    pub x: u32,
    pub y: u32,
}

#[derive(Default, Clone)]
pub struct ColorPointIn {
    pub red: u8,
    pub green: u8,
    pub blue: u8,
    pub tolerance: u8,
    pub x: u32,
    pub y: u32,
    pub region: Rect,
}

#[derive(Debug)]
struct ParseError {}
use thiserror::Error;

#[derive(Error, Debug)]
pub enum GameBotError {
    #[error("wrong format")]
    ParseError,
}

impl ColorPoint {
    fn click(&self) -> Result<()> {
        Ok(())
    }
}
impl TryFrom<&str> for ColorPointGroup {
    type Error = GameBotError;

    fn try_from(value: &str) -> std::result::Result<Self, Self::Error> {
        todo!()
    }
}

enum ImageSource {
    UnLoaded(PathBuf),
    Loaded(Vec<u8>),
}
impl ImageSource {
    fn load() -> ImageSource {
        ImageSource::Loaded(vec![])
    }
}

#[derive(Deserialize, Default, Debug, Clone)]
pub struct Rect {
    pub left: u32,
    pub top: u32,
    pub right: u32,
    pub bottom: u32,
}

pub struct ImageAt {
    pub img: ImageSource,
    pub left_top_point: Point,
    pub color_tolerance: u8,
}

pub struct ImageIn {
    pub img: ImageSource,
    pub left_top_region: Rect,
    pub color_tolerance: u8,
}

#[derive(Default)]
pub struct ColorPointGroup {
    pub group: Vec<ColorPoint>,
    pub color_tolerance: u8,
}

pub struct ColorPointGroupIn {
    pub group: Vec<ColorPoint>,
    pub color_tolerance: u8,
    pub region: Rect,
}

fn img(path: impl Into<String>) -> Img {
    Img(path.into())
}

// static_toml! {
//     static P = include_toml!("src/assert/mod.toml");
// }

// first way: point is static in central
// mod R1 {
//     use std::{cell::LazyCell, sync::LazyLock};
//
//     use super::ColorPoint;
//
//     // can't use function
//     pub static A1: ColorPoint = ColorPoint {
//         red: 0,
//         green: 0,
//         blue: 0,
//         x: 0,
//         y: 0,
//     };
//
//     // can use function
//     pub static A2: LazyLock<ColorPoint> = LazyLock::new(|| ColorPoint::default());
// }

// fn mail1() {
//     R1::A1.click();
//     R1::A2.click();
// }

// second way: point is in static struct in central
struct R2 {
    a1: ColorPoint,
    a2: ColorPoint,
}
static R2: LazyLock<R2> = LazyLock::new(|| R2 {
    a1: ColorPoint::default(),
    a2: ColorPoint::default(),
});

fn mail2() {
    R2.a1.click();
    R2.a2.click();
    let a3 = R2.a2.clone();
}

// third way: point is in struct, the initial way
fn mail3() {
    let a1 = ColorPoint::default();
    let a2 = ColorPoint::default();
    a1.click();
    a2.click();
}

pub trait IntoSeconds {
    fn into_seconds(self) -> Duration;
}
impl IntoSeconds for u64 {
    fn into_seconds(self) -> Duration {
        Duration::from_secs(self)
    }
}
impl IntoSeconds for f64 {
    fn into_seconds(self) -> Duration {
        Duration::from_secs_f64(self)
    }
}
impl IntoSeconds for Duration {
    fn into_seconds(self) -> Duration {
        self
    }
}
pub trait IntoMilliseconds {
    fn into_milliseconds(self) -> Duration;
}
impl IntoMilliseconds for u64 {
    fn into_milliseconds(self) -> Duration {
        Duration::from_millis(self)
    }
}
impl IntoMilliseconds for Duration {
    fn into_milliseconds(self) -> Duration {
        self
    }
}

fn mail4() {
    fn cp(data: &str) -> ColorPoint {
        ColorPoint::default()
    }

    let t = Duration::from_millis(500);
    let t2 = Duration::from_millis(500);
    let t3 = Duration::from_secs(20);

    trait TT {
        fn appear<T: IntoSeconds>(timeout: T) {
            let x: Duration = T::into_seconds(timeout);
        }
    }

    let a = 1.0;
    let a1 = cp("#FFFFFF,1,1");
    a1.click();
}

impl From<&ColorPoint> for Point {
    fn from(&ColorPoint { x, y, .. }: &ColorPoint) -> Self {
        Point { x, y }
    }
}
