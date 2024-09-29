use std::{env, fs::File, io::BufReader, path::PathBuf, time::Instant};

use candle_core::{DType, Device, IndexOp, Tensor};
use candle_nn::{
    conv2d, linear, lstm, ops::sigmoid, Conv2d, Conv2dConfig, Linear, Module, VarBuilder, LSTM, RNN,
};
use clap::{arg, Parser};
use gamebot::d;
use image::GenericImageView;

struct Down {
    m: Conv2d,
}
impl Down {
    fn load(vb: VarBuilder, ci: usize, co: usize) -> candle_core::Result<Self> {
        let cfg = Conv2dConfig {
            padding: 1,
            stride: 2,
            ..Default::default()
        };
        Ok(Self {
            m: conv2d(ci, co, 3, cfg, vb.pp("m"))?,
        })
    }
}

impl Module for Down {
    fn forward(&self, xs: &Tensor) -> candle_core::Result<Tensor> {
        let x = &self.m.forward(&xs)?;
        x * sigmoid(x)?
    }
}
struct Res {
    m0: Conv2d,
    m1: Conv2d,
}
impl Res {
    fn load(vb: VarBuilder, ci: usize, c: usize) -> candle_core::Result<Self> {
        let cfg = Conv2dConfig {
            padding: 1,
            ..Default::default()
        };
        Ok(Self {
            m0: conv2d(ci, c, 3, cfg, vb.pp("m0"))?,
            m1: conv2d(c, ci, 1, Default::default(), vb.pp("m1"))?,
        })
    }
}
impl Module for Res {
    fn forward(&self, xs: &Tensor) -> candle_core::Result<Tensor> {
        let mut x = self.m0.forward(xs)?;
        x = (&x * sigmoid(&x)?)?;
        x = self.m1.forward(&x)?;
        xs + x
    }
}

struct OCR {
    m: Vec<Box<dyn Module>>,
    lstm: LSTM,
    lstm_reverse: LSTM,
    fc: Linear,
}
impl OCR {
    fn load(vb: VarBuilder) -> candle_core::Result<Self> {
        let mut i = 0;
        let mut name = || -> String {
            i += 1;
            format!("m.{}", i - 1)
        };
        let c = 512;
        Ok(Self {
            m: vec![
                Box::new(Down::load(vb.pp(name()), 1, 24)?),
                Box::new(Res::load(vb.pp(name()), 24, 24)?),
                Box::new(Res::load(vb.pp(name()), 24, 24)?),
                Box::new(Down::load(vb.pp(name()), 24, 96)?),
                Box::new(conv2d(96, 48, 1, Default::default(), vb.pp(name()))?),
                Box::new(Res::load(vb.pp(name()), 48, 192)?),
                Box::new(Res::load(vb.pp(name()), 48, 192)?),
                Box::new(Res::load(vb.pp(name()), 48, 192)?),
                Box::new(Down::load(vb.pp(name()), 48, 192)?),
                Box::new(conv2d(192, 64, 1, Default::default(), vb.pp(name()))?),
                Box::new(Res::load(vb.pp(name()), 64, 256)?),
                Box::new(Res::load(vb.pp(name()), 64, 256)?),
                Box::new(Res::load(vb.pp(name()), 64, 256)?),
            ],
            lstm: lstm(c, c, Default::default(), vb.pp("lstm"))?,
            lstm_reverse: lstm(c, c, Default::default(), vb.pp("lstm_reverse"))?,
            fc: linear(c * 2, 8210, vb.pp("fc"))?,
        })
    }
}

impl Module for OCR {
    fn forward(&self, xs: &Tensor) -> candle_core::Result<Tensor> {
        let mut x = xs.clone();
        for (_, m) in self.m.iter().enumerate() {
            x = m.forward(&x)?;
        }
        let (b, c, h, w) = x.dims4()?;
        x = x.reshape((b, c * h, w))?;
        // let inp_sequence = x.chunk(w, 2)?;

        let mut state = vec![self.lstm.zero_state(b)?];
        for i in 0..w {
            state.push(self.lstm.step(&x.i((.., .., i))?, &state.last().unwrap())?);
        }
        let mut state_reverse = vec![self.lstm_reverse.zero_state(b)?];
        for i in (0..w).rev() {
            state_reverse.push(
                self.lstm_reverse
                    .step(&x.i((.., .., i))?, &state_reverse.last().unwrap())?,
            );
        }
        let h = state
            .into_iter()
            .skip(1)
            .zip(state_reverse.into_iter().skip(1).rev())
            .map(|(a, b)| Tensor::cat(&[a.h, b.h], 1))
            .collect::<candle_core::Result<Vec<_>>>()?;
        // dbg!(h[0].shape(), h.len());
        // panic!();
        x = Tensor::stack(&h, 0)?;
        x = self.fc.forward(&x)?;

        // Ok(Tensor::randn(0., 1., (39, 1, 8024), &Device::Cpu)?)
        Ok(x)
    }
}

fn test_ocr_speed(i0: &Tensor, net: &OCR) {
    net.forward(i0);
    let start = Instant::now();
    for i in 0..10 {
        net.forward(i0);
    }
    d!(start.elapsed().as_millis() / 10);
}

#[derive(Parser)]
struct Arg {
    i0: PathBuf,
    #[arg(long)]
    test_speed: bool,
}

pub fn test_ddddocr_candle() -> anyhow::Result<()> {
    // let arg = Arg::parse();
    let arg = Arg {
        // i0: PathBuf::from("/data/local/tmp/longsingleline.png"),
        i0: PathBuf::from("/data/local/tmp/79.png"),
        test_speed: true,
    };
    let i0 = arg.i0;
    let i0 = image::ImageReader::open(i0)
        .unwrap()
        .decode()
        .unwrap()
        .to_rgb8();

    let h = i0.height();
    let w = i0.width();

    let mut i0 = Tensor::from_vec(i0.into_raw(), (h as usize, w as usize, 3), &Device::Cpu)?
        .permute((2, 0, 1))?
        .unsqueeze(0)?;
    i0 = i0
        .to_dtype(DType::F32)?
        .mean_keepdim(1)?
        .interpolate2d(64, (w * 64 / h) as usize)?;
    i0 = (((i0 / 255.0)? - 0.5)? / 0.5)?;

    let weight = PathBuf::from("/data/local/tmp/ddddocr.safetensors");

    let vb = unsafe { VarBuilder::from_mmaped_safetensors(&[weight], DType::F32, &Device::Cpu)? };

    // let varmap = VarMap::new();
    // let vb = VarBuilder::from_varmap(&varmap, DType::F32, &Device::Cpu);

    let net = OCR::load(vb)?;
    let out = net.forward(&i0)?;
    if arg.test_speed {
        test_ocr_speed(&i0, &net);
    }

    // let map = varmap.data().lock().unwrap();
    // for (k, v) in map.iter() {
    //     dbg!(k, v.shape());
    // }
    // drop(map);

    // let out: Vec<u32> = out.argmax(2)?.squeeze(1)?.to_vec1()?;

    // let charset = PathBuf::from_iter(&[
    //     env!("CARGO_MANIFEST_DIR"),
    //     "..",
    //     "python",
    //     "ddddocr_pytorch",
    //     "charset.json",
    // ]);
    // let charset: Vec<String> = serde_json::from_reader(BufReader::new(File::open(charset)?))?;
    //
    // let out: String = out
    //     .into_iter()
    //     .map(|i| charset[i as usize].clone())
    //     .collect();
    // println!("{}", out);
    Ok(())
}
