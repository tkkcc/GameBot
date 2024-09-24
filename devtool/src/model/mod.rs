pub mod mnist {
    include!(concat!(env!("OUT_DIR"), "/model/mnist.rs"));
}
pub mod ddddocr {
    include!(concat!(env!("OUT_DIR"), "/model/common.rs"));
}
