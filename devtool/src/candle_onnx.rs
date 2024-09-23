// #[cfg(feature = "mkl")]
// extern crate intel_mkl_src;
//
// #[cfg(feature = "accelerate")]
// extern crate accelerate_src;

use candle_core::{DType, Device, Tensor};
use candle_core::{IndexOp, D};
use clap::{Parser, ValueEnum};
use gamebot::d;
use log::error;

#[derive(Clone, Copy, Debug, ValueEnum)]
enum Which {
    SqueezeNet,
    EfficientNet,
}

#[derive(Parser)]
struct Args {
    #[arg(long)]
    image: String,

    #[arg(long)]
    model: Option<String>,

    /// The model to be used.
    #[arg(value_enum, long, default_value_t = Which::SqueezeNet)]
    which: Which,
}

pub fn test_candle_onnx() -> anyhow::Result<()> {
    // let args = Args::parse();
    let args = Args {
        image: "/data/local/tmp/grace_hopper.jpg".into(),
        model: Some("/data/local/tmp/squeezenet1.1-7.onnx".into()),
        // model: None,
        which: Which::SqueezeNet,
    };
    let image = candle_examples::imagenet::load_image224(args.image)?;
    let image = match args.which {
        Which::SqueezeNet => image,
        Which::EfficientNet => image.permute((1, 2, 0))?,
    };

    d!("loaded image {image:?}");

    let model = match args.model {
        Some(model) => std::path::PathBuf::from(model),
        None => match args.which {
            Which::SqueezeNet => hf_hub::api::sync::Api::new()?
                .model("lmz/candle-onnx".into())
                .get("squeezenet1.1-7.onnx")?,
            Which::EfficientNet => hf_hub::api::sync::Api::new()?
                .model("onnx/EfficientNet-Lite4".into())
                .get("efficientnet-lite4-11.onnx")?,
        },
    };

    let model = candle_onnx::read_file(model)?;
    let graph = model.graph.as_ref().unwrap();
    let mut inputs = std::collections::HashMap::new();
    inputs.insert(graph.input[0].name.to_string(), image.unsqueeze(0)?);
    let mut outputs = candle_onnx::simple_eval(&model, inputs)?;
    let output = outputs.remove(&graph.output[0].name).unwrap();
    let prs = match args.which {
        Which::SqueezeNet => candle_nn::ops::softmax(&output, D::Minus1)?,
        Which::EfficientNet => output,
    };
    let prs = prs.i(0)?.to_vec1::<f32>()?;

    // Sort the predictions and take the top 5
    let mut top: Vec<_> = prs.iter().enumerate().collect();
    top.sort_by(|a, b| b.1.partial_cmp(a.1).unwrap());
    let top = top.into_iter().take(5).collect::<Vec<_>>();

    // Print the top predictions
    for &(i, p) in &top {
        error!(
            "{:50}: {:.2}%",
            candle_examples::imagenet::CLASSES[i],
            p * 100.0
        );
    }

    Ok(())
}
