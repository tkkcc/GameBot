use gamebot::{
    api::{wait, wait_millis},
    d,
};
use image::GenericImageView;
use ndarray::{s, Array, Array4, Axis};
use ort::{inputs, SessionOutputs};

pub fn test_ort_onnx() {
    use ort::{GraphOptimizationLevel, Session};

    let res = 224;
    let img = image::ImageReader::open("/data/local/tmp/grace_hopper.jpg".to_string())
        .unwrap()
        .decode()
        .unwrap()
        .resize_to_fill(
            res as u32,
            res as u32,
            image::imageops::FilterType::Triangle,
        );
    // let img = img.to_rgb8();
    // let data = img.into_raw();
    let input = ndarray::Array4::from_shape_fn((1, 3, 224, 224), |(_, c, y, x)| {
        (img.get_pixel(x as u32, y as u32)[c] as f32 / 255.0 - 0.5) / 0.5
    });

    // for pixel in img.pixels() {
    //     let x = pixel.0 as _;
    //     let y = pixel.1 as _;
    //     let [r, g, b, _] = pixel.2 .0;
    //     input[[0, 0, y, x]] = (r as f32) / 255.;
    //     input[[0, 1, y, x]] = (g as f32) / 255.;
    //     input[[0, 2, y, x]] = (b as f32) / 255.;
    // }

    d!(24);
    wait(2);
    let model = Session::builder();
    // if let Err(err) = model {
    //     // d!(err);
    // }
    // .with_optimization_level(GraphOptimizationLevel::Level3)
    // .unwrap()
    // .with_intra_threads(4)
    // .unwrap();
    // .commit_from_file("/data/local/tmp/squeezenet1.1-7.onnx")
    // .unwrap();

    d!(25);
    return;

    // let outputs: SessionOutputs = model
    //     .run(inputs!["images" => input.view()].unwrap())
    //     .unwrap();
    // let output = outputs["output0"]
    //     .try_extract_tensor::<f32>()
    //     .unwrap()
    //     .into_owned();
}
