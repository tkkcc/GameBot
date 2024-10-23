use std::{path::PathBuf, result::Result};

use crate::api::click;
use image::{ImageReader, RgbaImage};
use serde::Deserialize;
use thiserror::Error;

#[derive(Clone)]
pub struct Point {
    pub x: i32,
    pub y: i32,
}

impl Point {
    // only return jni error
    pub fn click(&self) {
        click(self.x as f32, self.y as f32)
    }
}

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
    pub region: Region,
}

#[derive(Debug)]
struct ParseError {}

#[derive(Error, Debug)]
pub enum GameBotError {
    #[error("wrong format")]
    ParseError,
}

impl ColorPoint {
    fn click(&self) {}
}
impl TryFrom<&str> for ColorPointGroup {
    type Error = GameBotError;

    fn try_from(value: &str) -> Result<Self, Self::Error> {
        todo!()
    }
}

#[derive(Deserialize, Default, Debug, Clone)]
pub struct Rect {
    pub left: i32,
    pub top: i32,
    pub width: u32,
    pub height: u32,
}
impl Rect {
    pub fn right(&self) -> i32 {
        self.left + self.width as i32
    }

    pub fn bottom(&self) -> i32 {
        self.top + self.height as i32
    }

    pub fn contains(&self, x: &Rect) -> bool {
        x.left >= self.left
            && x.right() <= self.right()
            && x.top >= self.top
            && x.bottom() <= self.bottom()
    }
}

#[derive(Deserialize, Default, Debug, Clone)]
pub struct Region {
    pub left: u32,
    pub top: u32,
    pub width: u32,
    pub height: u32,
}
impl Region {
    pub fn right(&self) -> u32 {
        self.left + self.width
    }

    pub fn bottom(&self) -> u32 {
        self.top + self.height
    }

    pub fn contains(&self, x: &Region) -> bool {
        x.left >= self.left
            && x.right() <= self.right()
            && x.top >= self.top
            && x.bottom() <= self.bottom()
    }
}
impl From<(u32, u32, u32, u32)> for Region {
    fn from(value: (u32, u32, u32, u32)) -> Self {
        Region {
            left: value.0,
            top: value.1,
            width: value.2,
            height: value.3,
        }
    }
}

// #[derive(Deserialize, Default, Debug, Clone)]
// pub struct Rect {
//     pub left: i32,
//     pub top: i32,
//     pub right: i32,
//     pub bottom: i32,
// }

#[derive(Clone)]
pub enum Tolerance {
    MAE(f32),
    MSE(f32),
    MAX(f32),
}

#[derive(Clone)]
pub struct DiskImageIn {
    pub img: PathBuf,
    pub region: Region,
    pub tolerance: Tolerance,
}
pub struct ImageIn {
    pub img: RgbaImage,
    pub region: Region,
    pub tolerance: Tolerance,
}
impl ImageIn {
    pub fn within(&self, region: impl Into<Region>) -> ImageIn {
        ImageIn {
            img: self.img.clone(),
            region: region.into(),
            tolerance: self.tolerance.clone(),
        }
    }
}

impl From<DiskImageIn> for ImageIn {
    fn from(
        DiskImageIn {
            img,
            region,
            tolerance,
        }: DiskImageIn,
    ) -> Self {
        let img = ImageReader::open(img)
            .unwrap()
            .decode()
            .unwrap()
            .into_rgba8();
        Self {
            img,
            region,
            tolerance,
        }
    }
}

#[derive(Default)]
pub struct ColorPointGroup {
    pub group: Vec<ColorPoint>,
    pub tolerance: f32,
}

#[derive(Default)]
pub struct ColorPointGroupIn {
    pub group: Vec<ColorPoint>,
    pub tolerance: f32,
    pub region: Region,
}

impl From<&ColorPoint> for Point {
    fn from(&ColorPoint { x, y, .. }: &ColorPoint) -> Self {
        Point {
            x: x as _,
            y: y as _,
        }
    }
}
impl From<(u32, u32)> for Point {
    fn from((x, y): (u32, u32)) -> Self {
        Point {
            x: x as _,
            y: y as _,
        }
    }
}
impl From<(i32, i32)> for Point {
    fn from((x, y): (i32, i32)) -> Self {
        Point { x, y }
    }
}
