use burn_import::onnx::{ModelGen, RecordType};

fn main() {
    ModelGen::new()
        .input("/home/bilabila/mnist.onnx")
        .out_dir("model/")
        .run_from_script();
}
