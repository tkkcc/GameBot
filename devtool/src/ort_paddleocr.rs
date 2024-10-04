use std::{num::NonZero, path::PathBuf, time::Instant};

use gamebot::d;
use image::{GenericImage, Rgb};
use ort::{inputs, NNAPIExecutionProvider, Session, SessionOutputs, XNNPACKExecutionProvider};

pub fn test_ort_paddleocr() {
    // let i0 = image::ImageReader::open("/data/local/tmp/longsingleline.png")
    let i0 = image::ImageReader::open("/data/local/tmp/79.png")
        .unwrap()
        .decode()
        .unwrap();

    let resized = image::imageops::resize(
        &i0.to_rgb8(),
        i0.width() * 48 / i0.height(),
        48,
        image::imageops::FilterType::Triangle,
    );

    let mut input = ndarray::Array4::from_shape_fn(
        (1, 3, 48, (i0.width() * 48 / i0.height()) as _),
        |(_, c, y, x)| (resized.get_pixel(x as u32, y as u32)[c] as f32 / 255.0 - 0.5) / 0.5,
    );

    let model = Session::builder()
        .unwrap()
        .with_optimization_level(ort::GraphOptimizationLevel::Level3)
        .unwrap()
        // .with_execution_providers([XNNPACKExecutionProvider::default()
        //     .with_intra_op_num_threads(NonZero::new(1).unwrap())
        //     .build()])
        // .unwrap()
        .with_intra_threads(std::thread::available_parallelism().unwrap().into())
        .unwrap()
        .commit_from_file("/data/local/tmp/rec.onnx")
        .unwrap();

    let outputs: SessionOutputs = model
        .run(inputs![model.inputs[0].name.to_owned() => input.view()].unwrap())
        .unwrap();
    let output = &outputs[model.outputs[0].name.to_owned()];
    let (shape, data) = output.try_extract_raw_tensor::<f32>().unwrap();

    let index = data.chunks(*shape.last().unwrap() as _).map(|p| {
        p.iter()
            .enumerate()
            .max_by(|x, y| x.1.total_cmp(y.1))
            .map(|x| x.0)
            .unwrap_or_default()
    });
    let charset = PathBuf::from("/data/local/tmp/ppocr_keys_v1.txt");
    let charset = std::fs::read(charset).unwrap();
    let charset = String::from_utf8(charset).unwrap();

    let charset: Vec<_> = charset
        .split('\n')
        .map(|x| x.chars().nth(0).unwrap())
        .collect();
    let ans: String = index
        .filter(|&i| i > 0 && i - 1 < charset.len())
        .map(|i| charset[i - 1])
        .collect();
    d!(ans);

    let start = Instant::now();
    for i in 0..10 {
        let outputs: SessionOutputs = model
            .run(inputs![model.inputs[0].name.to_string() => input.view()].unwrap())
            .unwrap();
        let output = &outputs[model.outputs[0].name.to_owned()];
        let out = output.try_extract_raw_tensor::<f32>().unwrap();
        let w = *output.shape().unwrap().last().unwrap();
        // d!(x.len());
        let index = out.1.chunks(w as _).map(|p| {
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

pub fn test_ort_paddleocr_multiline() {
    // let i0 = image::ImageReader::open("/data/local/tmp/longsingleline.png")
    let mut i0 = image::ImageReader::open("/data/local/tmp/multiline.png")
        .unwrap()
        .decode()
        .unwrap()
        .to_rgb8();

    let (h, w) = (i0.height(), i0.width());
    let nh = (h + 31) / 32 * 32;
    let nw = (w + 31) / 32 * 32;
    if nh != h || nw != w {
        let mut pad: image::RgbImage = image::RgbImage::from_pixel(nw, nh, Rgb([0, 0, 0]));
        pad.copy_from(&i0, 0, 0).unwrap();
        i0 = pad;
    }

    let mut input = ndarray::Array4::from_shape_fn((1, 3, nh as _, nw as _), |(_, c, y, x)| {
        (i0.get_pixel(x as u32, y as u32)[c] as f32 / 255.0 - 0.5) / 0.5
    });

    let model = Session::builder()
        .unwrap()
        .with_optimization_level(ort::GraphOptimizationLevel::Level3)
        .unwrap()
        // .with_execution_providers([XNNPACKExecutionProvider::default()
        //     .with_intra_op_num_threads(std::thread::available_parallelism().unwrap())
        //     .build()])
        // .unwrap()
        .with_intra_threads(std::thread::available_parallelism().unwrap().into())
        .unwrap()
        .commit_from_file("/data/local/tmp/det.onnx")
        .unwrap();

    let outputs: SessionOutputs = model
        .run(inputs![model.inputs[0].name.to_owned() => input.view()].unwrap())
        .unwrap();
    let output = &outputs[model.outputs[0].name.to_owned()];
    let (shape, data) = output.try_extract_raw_tensor::<f32>().unwrap();

    let start = Instant::now();
    for i in 0..10 {
        let outputs: SessionOutputs = model
            .run(inputs![model.inputs[0].name.to_string() => input.view()].unwrap())
            .unwrap();
        let output = &outputs[model.outputs[0].name.to_owned()];
        // let out = output.try_extract_raw_tensor::<f32>().unwrap();
    }
    d!(start.elapsed().as_millis() / 10);
}
