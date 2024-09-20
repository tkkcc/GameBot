use crate::color::{
    ColorPoint, ColorPointGroup, ColorPointGroupIn, ImageIn, Point, Region, Tolerance,
};

#[derive(Default, Debug)]
pub struct Screenshot {
    pub width: u32,
    pub height: u32,
    // pub pixel_stride: i32,
    // pub row_stride: i32,
    pub data: &'static [u8],
    // pub data: Vec<u8>,
}

impl Screenshot {
    pub fn find_color_point(
        &self,
        &ColorPoint {
            x,
            y,
            red,
            blue,
            green,
        }: &ColorPoint,
    ) -> Option<Point> {
        if x >= self.width || y >= self.height {
            return None;
        }
        let i = ((y * self.width + x) * 4) as usize;
        if self.data[i].abs_diff(red) > 0
            || self.data[i + 1].abs_diff(green) > 0
            || self.data[i + 2].abs_diff(blue) > 0
        {
            return None;
        }
        Some((x, y).into())
    }

    pub fn find_color_point_group(&self, cpg: &ColorPointGroup) -> Option<Point> {
        if cpg.group.is_empty() {
            return None;
        }
        let tolerance = (cpg.tolerance * 255.0) as u8;
        for cp in &cpg.group {
            if cp.x >= self.width || cp.y >= self.height {
                return None;
            }
            let i = ((cp.y * self.width + cp.x) * 4) as usize;
            if self.data[i].abs_diff(cp.red) > tolerance
                || self.data[i + 1].abs_diff(cp.green) > tolerance
                || self.data[i + 2].abs_diff(cp.blue) > tolerance
            {
                return None;
            }
        }
        Some((&cpg.group[0]).into())
    }

    pub fn find_color_point_group_in(&self, cpg: &ColorPointGroupIn) -> Option<Point> {
        self.find_all_color_point_group_in(cpg, 1).first().cloned()
    }

    pub fn region(&self) -> Region {
        Region {
            left: 0,
            width: self.width,
            top: 0,
            height: self.height,
        }
    }

    pub fn find_all_color_point_group_in(
        &self,
        cpg: &ColorPointGroupIn,
        max_num: usize,
    ) -> Vec<Point> {
        // we visit all valid localtion: iter on delta x and y, move via color point x + delta x
        let mut ans = vec![];
        if cpg.group.is_empty() {
            return ans;
        }
        let region = &cpg.region;

        if !self.region().contains(region) {
            return ans;
        }

        let mut t = u32::MAX;
        let mut l = u32::MAX;
        let mut b = 0;
        let mut r = 0;
        for cp in &cpg.group {
            l = l.min(cp.x);
            r = r.max(cp.x);
            t = t.min(cp.y);
            b = b.max(cp.y);
        }

        // cpg cant be in region
        if region.width < r - l || region.height < b - t {
            return ans;
        }

        let tolerance = (cpg.tolerance * 255.0) as u8;

        for dy in (region.top as i32 - t as i32)..(region.bottom() as i32 - b as i32) {
            'outer: for dx in (region.left as i32 - l as i32)..(region.right() as i32 - r as i32) {
                for cp in &cpg.group {
                    let x = (cp.x as i32 + dx) as u32;
                    let y = (cp.y as i32 + dy) as u32;
                    let i = ((y * self.width + x) * 4) as usize;
                    if self.data[i].abs_diff(cp.red) > tolerance
                        || self.data[i + 1].abs_diff(cp.green) > tolerance
                        || self.data[i + 2].abs_diff(cp.blue) > tolerance
                    {
                        continue 'outer;
                    }
                }

                ans.push(Point {
                    x: (cpg.group[0].x as i32 + dx),
                    y: (cpg.group[0].y as i32 + dy),
                });

                if ans.len() >= max_num {
                    return ans;
                }
            }
        }
        ans
    }

    pub fn find_image_in(&self, img: &ImageIn) -> Option<Point> {
        self.find_all_image_in(img, 1).first().cloned()
    }

    pub fn find_all_image_in(
        &self,
        ImageIn {
            img,
            region,
            tolerance,
        }: &ImageIn,
        max_num: usize,
    ) -> Vec<Point> {
        let mut ans = vec![];

        if !self.region().contains(region)
            || img.width() > region.width
            || img.height() > region.height
        {
            return ans;
        }

        let ih = img.height();
        let iw = img.width();
        let img = img.as_raw().as_slice();
        let base = (ih * iw) as f64 * 3.0;

        for y in region.top..region.bottom() - ih {
            'outer: for x in region.left..region.right() - iw {
                let mut loss = 0f64;
                for iy in 0..ih {
                    for ix in 0..iw {
                        let i = ((iy * iw + ix) * 4) as usize;
                        let j = ((y * self.width + x) * 4) as usize;

                        let r = img[i].abs_diff(self.data[j]) as f64;
                        let g = img[i + 1].abs_diff(self.data[j + 1]) as f64;
                        let b = img[i + 2].abs_diff(self.data[j + 2]) as f64;
                        let a = img[i + 3] as f64 / 255.0;

                        match tolerance {
                            Tolerance::MAE(limit) => {
                                loss += a * (r + g + b) / base;
                                if loss > (*limit as f64) {
                                    continue 'outer;
                                }
                            }
                            Tolerance::MSE(limit) => {
                                loss += a * (r.powi(2) + g.powi(2) + b.powi(2)) / base;
                                if loss > (*limit as f64) {
                                    continue 'outer;
                                }
                            }
                            Tolerance::MAX(limit) => {
                                loss = loss.max(a * r.max(g).max(b));
                                if loss > (*limit) as f64 {
                                    continue 'outer;
                                }
                            }
                        }
                    }
                }
                ans.push((x, y).into());
                if ans.len() >= max_num {
                    return ans;
                }
            }
        }

        ans
    }
}
