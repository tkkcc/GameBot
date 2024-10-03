use std::{fs::File, io::BufReader, path::PathBuf, time::Instant};

use gamebot::d;
use ncnn::{self, Mat, Net};

pub fn test_ncnn_ddddocr() {
    let mut opt = ncnn::Option::new();
    // d!(opt.get_num_threads());
    // opt.set_num_threads(4);
    // opt.set_vulkan_compute(true);

    let mut net = Net::new();
    net.set_option(&opt);
    net.load_param("/data/local/tmp/ddddocr.ncnn.param");
    net.load_model("/data/local/tmp/ddddocr.ncnn.bin");

    // let i0 = image::ImageReader::open("/data/local/tmp/longsingleline.png")
    let i0 = image::ImageReader::open("/data/local/tmp/79.png")
        .unwrap()
        .decode()
        .unwrap();

    let resized = image::imageops::resize(
        &i0.grayscale().to_luma8(),
        i0.width() * 64 / i0.height(),
        64,
        image::imageops::FilterType::Triangle,
    );
    let mut in0 = Mat::from_pixels(
        resized.into_raw().as_slice(),
        ncnn::MatPixelType::GRAY,
        (i0.width() * 64 / i0.height()) as _,
        64,
        None,
    )
    .unwrap();

    let mean = [0.5 * 255.0];
    let norm = [1.0 / 0.5 / 255.0];
    in0.substract_mean_normalize(&mean, &norm);

    let mut out = Mat::new();
    let mut ex = net.create_extractor();
    ex.input("in0", &in0);
    ex.extract("out0", &mut out);
    d!("{}x{}x{}", out.c(), out.h(), out.w());

    let x = out.to_slice::<f32>();
    let index = x.chunks(out.w() as _).map(|p| {
        p.iter()
            .enumerate()
            .max_by(|x, y| x.1.total_cmp(y.1))
            .map(|x| x.0)
            .unwrap_or_default()
    });
    let charset = "/data/local/tmp/charset.json";
    let charset: Vec<String> =
        serde_json::from_reader(BufReader::new(File::open(charset).unwrap())).unwrap();

    let ans: String = index.map(|i| charset[i].clone()).collect();
    d!(ans);

    let start = Instant::now();
    for i in 0..10 {
        let mut ex = net.create_extractor();
        ex.input("in0", &in0);
        ex.extract("out0", &mut out);

        let x = out.to_slice::<f32>();
        // d!(x.len());
        let index = x.chunks(out.w() as _).map(|p| {
            p.iter()
                .enumerate()
                .max_by(|x, y| x.1.total_cmp(y.1))
                .map(|x| x.0)
                .unwrap_or_default()
        });
        // let ans: String = index
        //     .filter(|&i| i > 0 && i - 1 < charset.len())
        //     .map(|i| charset[i - 1])
        //     .collect();
        // d!(ans);
    }
    d!(start.elapsed().as_millis() / 10);
}
