#[cfg(feature = "fetch-models")]
use std::fmt::Write;
use std::{any::Any, marker::PhantomData, path::Path, ptr::NonNull, sync::Arc};

use super::SessionBuilder;
use crate::{
	environment::get_environment,
	error::{Error, ErrorCode, Result},
	execution_providers::apply_execution_providers,
	memory::Allocator,
	ortsys,
	session::{dangerous, InMemorySession, Input, Output, Session, SharedSessionInner}
};

impl SessionBuilder {
	/// Downloads a pre-trained ONNX model from the given URL and builds the session.
	#[cfg(feature = "fetch-models")]
	#[cfg_attr(docsrs, doc(cfg(feature = "fetch-models")))]
	pub fn commit_from_url(self, model_url: impl AsRef<str>) -> Result<Session> {
		let mut download_dir = ort_sys::internal::dirs::cache_dir()
			.expect("could not determine cache directory")
			.join("models");
		if std::fs::create_dir_all(&download_dir).is_err() {
			download_dir = std::env::current_dir().expect("Failed to obtain current working directory");
		}

		let url = model_url.as_ref();
		let model_filename = <sha2::Sha256 as sha2::Digest>::digest(url).into_iter().fold(String::new(), |mut s, b| {
			let _ = write!(&mut s, "{:02x}", b);
			s
		});
		let model_filepath = download_dir.join(model_filename);
		let downloaded_path = if model_filepath.exists() {
			tracing::info!(model_filepath = format!("{}", model_filepath.display()).as_str(), "Model already exists, skipping download");
			model_filepath
		} else {
			tracing::info!(model_filepath = format!("{}", model_filepath.display()).as_str(), url = format!("{url:?}").as_str(), "Downloading model");

			let resp = ureq::get(url).call().map_err(|e| Error::new(format!("Error downloading to file: {e}")))?;

			let len = resp
				.header("Content-Length")
				.and_then(|s| s.parse::<usize>().ok())
				.expect("Missing Content-Length header");
			tracing::info!(len, "Downloading {} bytes", len);

			let mut reader = resp.into_reader();

			let f = std::fs::File::create(&model_filepath).expect("Failed to create model file");
			let mut writer = std::io::BufWriter::new(f);

			let bytes_io_count = std::io::copy(&mut reader, &mut writer).map_err(Error::wrap)?;
			if bytes_io_count == len as u64 {
				model_filepath
			} else {
				return Err(Error::new(format!("Failed to download entire model; file only has {bytes_io_count} bytes, expected {len}")));
			}
		};

		self.commit_from_file(downloaded_path)
	}

	/// Loads an ONNX model from a file and builds the session.
	pub fn commit_from_file<P>(mut self, model_filepath_ref: P) -> Result<Session>
	where
		P: AsRef<Path>
	{
		let model_filepath = model_filepath_ref.as_ref();
		if !model_filepath.exists() {
			return Err(Error::new_with_code(ErrorCode::NoSuchFile, format!("File at `{}` does not exist", model_filepath.display())));
		}

		let model_path = crate::util::path_to_os_char(model_filepath);

		let env = get_environment()?;
		apply_execution_providers(&self, env.execution_providers.iter().cloned())?;

		if env.has_global_threadpool {
			ortsys![unsafe DisablePerSessionThreads(self.session_options_ptr.as_ptr())?];
		}

		let mut session_ptr: *mut ort_sys::OrtSession = std::ptr::null_mut();
		ortsys![unsafe CreateSession(env.env_ptr.as_ptr(), model_path.as_ptr(), self.session_options_ptr.as_ptr(), &mut session_ptr)?; nonNull(session_ptr)];

		let session_ptr = unsafe { NonNull::new_unchecked(session_ptr) };

		let allocator = match &self.memory_info {
			Some(info) => {
				let mut allocator_ptr: *mut ort_sys::OrtAllocator = std::ptr::null_mut();
				ortsys![unsafe CreateAllocator(session_ptr.as_ptr(), info.ptr.as_ptr(), &mut allocator_ptr)?; nonNull(allocator_ptr)];
				unsafe { Allocator::from_raw_unchecked(allocator_ptr) }
			}
			None => Allocator::default()
		};

		// Extract input and output properties
		let num_input_nodes = dangerous::extract_inputs_count(session_ptr)?;
		let num_output_nodes = dangerous::extract_outputs_count(session_ptr)?;
		let inputs = (0..num_input_nodes)
			.map(|i| dangerous::extract_input(session_ptr, &allocator, i))
			.collect::<Result<Vec<Input>>>()?;
		let outputs = (0..num_output_nodes)
			.map(|i| dangerous::extract_output(session_ptr, &allocator, i))
			.collect::<Result<Vec<Output>>>()?;

		let extras = self.operator_domains.drain(..).map(|d| Box::new(d) as Box<dyn Any>).collect();

		Ok(Session {
			inner: Arc::new(SharedSessionInner {
				session_ptr,
				allocator,
				_extras: extras,
				_environment: env
			}),
			inputs,
			outputs
		})
	}

	/// Load an ONNX graph from memory and commit the session
	/// For `.ort` models, we enable `session.use_ort_model_bytes_directly`.
	/// For more information, check [Load ORT format model from an in-memory byte array](https://onnxruntime.ai/docs/performance/model-optimizations/ort-format-models.html#load-ort-format-model-from-an-in-memory-byte-array).
	///
	/// If you wish to store the model bytes and the [`InMemorySession`] in the same struct, look for crates that
	/// facilitate creating self-referential structs, such as [`ouroboros`](https://github.com/joshua-maros/ouroboros).
	pub fn commit_from_memory_directly(mut self, model_bytes: &[u8]) -> Result<InMemorySession<'_>> {
		// Enable zero-copy deserialization for models in `.ort` format.
		self.add_config_entry("session.use_ort_model_bytes_directly", "1")?;
		self.add_config_entry("session.use_ort_model_bytes_for_initializers", "1")?;

		let session = self.commit_from_memory(model_bytes)?;

		Ok(InMemorySession { session, phantom: PhantomData })
	}

	/// Load an ONNX graph from memory and commit the session.
	pub fn commit_from_memory(mut self, model_bytes: &[u8]) -> Result<Session> {
		let mut session_ptr: *mut ort_sys::OrtSession = std::ptr::null_mut();

		let env = get_environment()?;
		apply_execution_providers(&self, env.execution_providers.iter().cloned())?;

		if env.has_global_threadpool {
			ortsys![unsafe DisablePerSessionThreads(self.session_options_ptr.as_ptr())?];
		}

		let model_data = model_bytes.as_ptr().cast::<std::ffi::c_void>();
		let model_data_length = model_bytes.len();
		ortsys![
			unsafe CreateSessionFromArray(env.env_ptr.as_ptr(), model_data, model_data_length as _, self.session_options_ptr.as_ptr(), &mut session_ptr)?;
			nonNull(session_ptr)
		];

		let session_ptr = unsafe { NonNull::new_unchecked(session_ptr) };

		let allocator = match &self.memory_info {
			Some(info) => {
				let mut allocator_ptr: *mut ort_sys::OrtAllocator = std::ptr::null_mut();
				ortsys![unsafe CreateAllocator(session_ptr.as_ptr(), info.ptr.as_ptr(), &mut allocator_ptr)?; nonNull(allocator_ptr)];
				unsafe { Allocator::from_raw_unchecked(allocator_ptr) }
			}
			None => Allocator::default()
		};

		// Extract input and output properties
		let num_input_nodes = dangerous::extract_inputs_count(session_ptr)?;
		let num_output_nodes = dangerous::extract_outputs_count(session_ptr)?;
		let inputs = (0..num_input_nodes)
			.map(|i| dangerous::extract_input(session_ptr, &allocator, i))
			.collect::<Result<Vec<Input>>>()?;
		let outputs = (0..num_output_nodes)
			.map(|i| dangerous::extract_output(session_ptr, &allocator, i))
			.collect::<Result<Vec<Output>>>()?;

		let extras = self.operator_domains.drain(..).map(|d| Box::new(d) as Box<dyn Any>).collect();

		let session = Session {
			inner: Arc::new(SharedSessionInner {
				session_ptr,
				allocator,
				_extras: extras,
				_environment: env
			}),
			inputs,
			outputs
		};
		Ok(session)
	}
}
