use std::path::{Path, PathBuf};

use serde::Deserialize;

enum Error {}

type Result<T> = std::result::Result<T, Error>;

trait Searchable {
    fn exist(&self) -> Result<Point>;
    fn appear(&self) -> Result<Point>;
    fn disappear(&self) -> Result<Point>;
}

struct Point {
    x: u32,
    y: u32,
}

struct Img(String);

struct ColorPoint {
    red: u8,
    green: u8,
    blue: u8,
    x: u32,
    y: u32,
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

#[derive(Deserialize, Default)]
pub struct Rect {
    left: u32,
    top: u32,
    right: u32,
    bottom: u32,
}

struct ImageAt {
    img: ImageSource,
    left_top_point: Point,
    color_tolerance: u8,
}

struct ImageIn {
    img: ImageSource,
    left_top_region: Rect,
    color_tolerance: u8,
}

struct ColorPointGroup {
    group: Vec<ColorPoint>,
    color_tolerance: u8,
}

struct ColorPointGroupIn {
    img: ImageSource,
    left_top_region: Rect,
    color_tolerance: u8,
}

fn img(path: impl Into<String>) -> Img {
    Img(path.into())
}

fn get_mail() {}
