use std::{path::PathBuf, time::Instant};

use gamebot::d;
use ncnn::{self, Mat, Net};

pub fn test_ncnn_paddleocr() {
    let mut opt = ncnn::Option::new();
    d!(opt.get_num_threads());
    // opt.set_num_threads(4);
    // opt.set_vulkan_compute(true);

    let mut net = Net::new();
    net.set_option(&opt);
    net.load_param("/data/local/tmp/rec.ncnn.param");
    net.load_model("/data/local/tmp/rec.ncnn.bin");

    let i0 = image::ImageReader::open("/data/local/tmp/longsingleline.png")
        .unwrap()
        .decode()
        .unwrap();

    let resized = image::imageops::resize(
        &i0.to_rgb8(),
        i0.width() * 48 / i0.height(),
        48,
        image::imageops::FilterType::Triangle,
    );
    let mut in0 = Mat::from_pixels(
        resized.into_raw().as_slice(),
        ncnn::MatPixelType::RGB,
        (i0.width() * 48 / i0.height()) as _,
        48,
        None,
    )
    .unwrap();

    // let m = 255.0 / 2.0;
    // in0.substract_mean_normalize(&[m, m, m], &[m, m, m]);
    // https://github.com/7rah/paddleocr-rust-ncnn/blob/master/src/helper.rs#L62
    let mean = [0.485 * 255.0, 0.456 * 255.0, 0.406 * 255.0];
    let norm = [
        1.0 / 0.229 / 255.0,
        1.0 / 0.224 / 255.0,
        1.0 / 0.225 / 255.0,
    ];
    in0.substract_mean_normalize(&mean, &norm);

    // let in0 = Mat::new_3d(48, 224, 3, None);
    let mut out = Mat::new();
    let mut ex = net.create_extractor();
    ex.input("in0", &in0);
    ex.extract("out0", &mut out);
    d!("{}x{}x{}", out.c(), out.h(), out.w());

    let x = out.to_slice::<f32>();
    // d!(x.len());
    let index = x.chunks(out.w() as _).map(|p| {
        p.iter()
            .enumerate()
            .max_by(|x, y| x.1.total_cmp(y.1))
            .map(|x| x.0)
            .unwrap_or_default()
    });
    // let i2: Vec<_> = index.clone().collect();
    // d!(x[i2[0]], x[0], x[1], x[4], &i2[0..10]);
    // d!(index.len());
    let charset = PathBuf::from("/data/local/tmp/ppocr_keys_v1.txt");
    let charset = std::fs::read(charset).unwrap();
    let charset = String::from_utf8(charset).unwrap();

    let charset: Vec<_> = charset
        .split('\n')
        .map(|x| x.chars().nth(0).unwrap())
        .collect();
    // d!(charset.len());
    // d!(&charset[..4]);
    let ans: String = index
        .filter(|&i| i > 0 && i - 1 < charset.len())
        .map(|i| charset[i - 1])
        .collect();
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
        let ans: String = index
            .filter(|&i| i > 0 && i - 1 < charset.len())
            .map(|i| charset[i - 1])
            .collect();
        d!(ans);
    }
    d!(start.elapsed().as_millis() / 10);
}
