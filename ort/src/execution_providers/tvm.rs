use crate::{
	error::{Error, Result},
	execution_providers::{ExecutionProvider, ExecutionProviderDispatch},
	session::SessionBuilder
};

#[cfg(all(not(feature = "load-dynamic"), feature = "tvm"))]
extern "C" {
	fn OrtSessionOptionsAppendExecutionProvider_Tvm(options: *mut ort_sys::OrtSessionOptions, opt_str: *const std::os::raw::c_char) -> ort_sys::OrtStatusPtr;
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TVMExecutorType {
	GraphExecutor,
	VirtualMachine
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TVMTuningType {
	AutoTVM,
	Ansor
}

#[derive(Debug, Default, Clone)]
pub struct TVMExecutionProvider {
	/// Executor type used by TVM. There is a choice between two types, `GraphExecutor` and `VirtualMachine`. Default is
	/// [`TVMExecutorType::VirtualMachine`].
	pub executor: Option<TVMExecutorType>,
	/// Path to folder with set of files (`.ro-`, `.so`/`.dll`-files and weights) obtained after model tuning.
	pub so_folder: Option<String>,
	/// Whether or not to perform a hash check on the model obtained in the `so_folder`.
	pub check_hash: Option<bool>,
	/// A path to a file that contains the pre-computed hash for the ONNX model located in the `so_folder` for checking
	/// when `check_hash` is `Some(true)`.
	pub hash_file_path: Option<String>,
	pub target: Option<String>,
	pub target_host: Option<String>,
	pub opt_level: Option<usize>,
	/// Whether or not all model weights are kept on compilation stage, otherwise they are downloaded on each inference.
	/// `true` is recommended for best performance and is the default.
	pub freeze_weights: Option<bool>,
	pub to_nhwc: Option<bool>,
	pub tuning_type: Option<TVMTuningType>,
	/// Path to AutoTVM or Ansor tuning file which gives specifications for given model and target for the best
	/// performance.
	pub tuning_file_path: Option<String>,
	pub input_names: Option<String>,
	pub input_shapes: Option<String>
}

impl TVMExecutionProvider {
	#[must_use]
	pub fn build(self) -> ExecutionProviderDispatch {
		self.into()
	}
}

impl From<TVMExecutionProvider> for ExecutionProviderDispatch {
	fn from(value: TVMExecutionProvider) -> Self {
		ExecutionProviderDispatch::new(value)
	}
}

impl ExecutionProvider for TVMExecutionProvider {
	fn as_str(&self) -> &'static str {
		"TvmExecutionProvider"
	}

	#[allow(unused, unreachable_code)]
	fn register(&self, session_builder: &SessionBuilder) -> Result<()> {
		#[cfg(any(feature = "load-dynamic", feature = "tvm"))]
		{
			super::get_ep_register!(OrtSessionOptionsAppendExecutionProvider_Tvm(options: *mut ort_sys::OrtSessionOptions, opt_str: *const std::os::raw::c_char) -> ort_sys::OrtStatusPtr);
			let mut option_string = Vec::new();
			if let Some(check_hash) = self.check_hash {
				option_string.push(format!("check_hash:{}", if check_hash { "True" } else { "False" }));
			}
			if let Some(executor) = self.executor {
				option_string.push(format!(
					"executor:{}",
					match executor {
						TVMExecutorType::GraphExecutor => "graph",
						TVMExecutorType::VirtualMachine => "vm"
					}
				));
			}
			if let Some(freeze_weights) = self.freeze_weights {
				option_string.push(format!("freeze_weights:{}", if freeze_weights { "True" } else { "False" }));
			}
			if let Some(hash_file_path) = self.hash_file_path.as_ref() {
				option_string.push(format!("hash_file_path:{hash_file_path}"));
			}
			if let Some(input_names) = self.input_names.as_ref() {
				option_string.push(format!("input_names:{input_names}"));
			}
			if let Some(input_shapes) = self.input_shapes.as_ref() {
				option_string.push(format!("input_shapes:{input_shapes}"));
			}
			if let Some(opt_level) = self.opt_level {
				option_string.push(format!("opt_level:{opt_level}"));
			}
			if let Some(so_folder) = self.so_folder.as_ref() {
				option_string.push(format!("so_folder:{so_folder}"));
			}
			if let Some(target) = self.target.as_ref() {
				option_string.push(format!("target:{target}"));
			}
			if let Some(target_host) = self.target_host.as_ref() {
				option_string.push(format!("target_host:{target_host}"));
			}
			if let Some(to_nhwc) = self.to_nhwc {
				option_string.push(format!("to_nhwc:{}", if to_nhwc { "True" } else { "False" }));
			}
			let options_string = std::ffi::CString::new(option_string.join(",")).unwrap_or_else(|_| unreachable!());
			return crate::error::status_to_result(unsafe {
				OrtSessionOptionsAppendExecutionProvider_Tvm(session_builder.session_options_ptr.as_ptr(), options_string.as_ptr())
			});
		}

		Err(Error::new(format!("`{}` was not registered because its corresponding Cargo feature is not enabled.", self.as_str())))
	}
}
