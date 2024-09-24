pub mod mnist {
    include!(concat!(env!("OUT_DIR"), "/model/mnist.rs"));
}
// pub mod ddddocr {
//     include!(concat!(env!("OUT_DIR"), "/model/common.rs"));
// }
pub mod squeeze {
    include!(concat!(env!("OUT_DIR"), "/model/squeezenet1.rs"));
}
