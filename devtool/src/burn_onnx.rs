use std::env::args;

use burn::{
    backend::ndarray::NdArray,
    backend::Wgpu,
    data::dataset::{vision::MnistDataset, Dataset},
    prelude::Backend,
    tensor::Tensor,
};

use gamebot::d;
use log::error;

// use crate::model::mnist::Model;
use crate::model::ddddocr::Model;

const IMAGE_INX: usize = 42; // <- Change this to test a different image

pub fn test_burn_onnx() {
    // Get image index argument (first) from command line

    // let image_index = if let Some(image_index) = args().nth(1) {
    //     println!("Image index: {}", image_index);
    //     image_index
    //         .parse::<usize>()
    //         .expect("Failed to parse image index")
    // } else {
    //     println!("No image index provided; Using default image index: {IMAGE_INX}");
    //     IMAGE_INX
    // };
    d!("40");
    let image_index = 15;

    assert!(image_index < 10000, "Image index must be less than 10000");

    // type Backend = Wgpu<f32>;
    type Backend = NdArray<f32>;

    d!("45");
    // Get a default device for the backend
    let device = <Backend as burn::tensor::backend::Backend>::Device::default();

    d!("46");

    // Create a new model and load the state
    let model: Model<Backend> = Model::default();
    // let model: Model<Backend> = Model::from_file("/data/local/tmp/mnist", &Default::default());
    let model: Model<Backend> = Model::from_file("/data/local/tmp/common", &Default::default());
    d!("47");

    // Load the MNIST dataset and get an item
    let dataset = MnistDataset::test();
    d!("50");
    let item = dataset.get(image_index).unwrap();
    d!("55");

    // Create a tensor from the image data
    let image_data = item.image.iter().copied().flatten().collect::<Vec<f32>>();
    let mut input =
        Tensor::<Backend, 1>::from_floats(image_data.as_slice(), &device).reshape([1, 1, 28, 28]);

    // Normalize the input
    input = ((input / 255) - 0.1307) / 0.3081;

    // Run the model on the input
    let output = model.forward(input);

    // Get the index of the maximum value
    let arg_max = output.argmax(1).into_scalar() as u8;

    // Check if the index matches the label
    assert!(arg_max == item.label);

    error!("Success!");
    error!("Predicted: {}", arg_max);
    error!("Actual: {}", item.label);
    error!("See the image online, click the link below:");
    error!("https://huggingface.co/datasets/ylecun/mnist/viewer/mnist/test?row={image_index}");
}
