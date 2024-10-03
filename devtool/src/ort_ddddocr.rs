use std::{fs::File, io::BufReader, num::NonZero, path::PathBuf, time::Instant};

use gamebot::d;
use ort::{inputs, NNAPIExecutionProvider, Session, SessionOutputs, XNNPACKExecutionProvider};

pub fn test_ort_ddddocr() {
    let i0 = image::ImageReader::open("/data/local/tmp/longsingleline.png")
        // let i0 = image::ImageReader::open("/data/local/tmp/79.png")
        .unwrap()
        .decode()
        .unwrap();

    let resized = image::imageops::resize(
        &i0.grayscale().to_luma8(),
        i0.width() * 64 / i0.height(),
        64,
        image::imageops::FilterType::Triangle,
    );

    let mut input = ndarray::Array4::from_shape_fn(
        (1, 1, 64, (i0.width() * 64 / i0.height()) as _),
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
        .commit_from_file("/data/local/tmp/ddddocr.onnx")
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

    let charset = "/data/local/tmp/charset.json";
    let charset: Vec<String> =
        serde_json::from_reader(BufReader::new(File::open(charset).unwrap())).unwrap();

    // let out: String = out
    //     .into_iter()
    //     .map(|i| charset[i as usize].clone())
    //     .collect();
    // let charset = PathBuf::from("/data/local/tmp/char.txt");
    // let charset = std::fs::read(charset).unwrap();
    // let charset = String::from_utf8(charset).unwrap();

    let ans: String = index.map(|i| charset[i].clone()).collect();
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
        // let ans: String = index
        //     .filter(|&i| i > 0 && i - 1 < charset.len())
        //     .map(|i| charset[i - 1])
        //     .collect();
        // d!(ans);
    }
    d!(start.elapsed().as_millis() / 10);
}
