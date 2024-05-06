use std::error::Error;

// use git2::{CertificateCheckStatus, RemoteCallbacks};
use jni::{
    objects::{JClass, JString},
    sys::jstring,
    JNIEnv,
};
use wasmtime::{Caller, Engine, Linker, Module, Store};
// use tracing::{debug, error, Subscriber};
// use tracing_subscriber::Registry;

pub fn add(left: usize, right: usize) -> usize {
    left + right
}

#[macro_use]
extern crate log;

extern crate android_logger;

#[no_mangle]
extern "C" fn Java_bilabila_gamebot_host_MainActivity_test(
    mut env: JNIEnv,
    class: JClass,
    input: JString,
) -> jstring {
    // let subscriber = Registry::default();
    // use tracing_subscriber::layer::SubscriberExt;
    // let subscriber = subscriber.with(tracing_android::layer("com.example").unwrap());
    // tracing::subscriber::set_global_default(subscriber).unwrap();

    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Trace)
            .with_tag("mytag")
            .with_filter(
                android_logger::FilterBuilder::new()
                    .parse("debug,hello::crate=trace")
                    .build(),
            ),
    );

    fn f(input: String) -> Result<String, Box<dyn Error>> {
        std::env::set_var("SSL_CERT_DIR", "/system/etc/security/cacerts");
        // error!("dddddddd");
        let url = "https://github.com/alexcrichton/git2-rs";
        // unsafe {
        //     // let _ = git2::opts::set_ssl_cert_dir("/system/etc/security/cacerts");
        //     let r = git2::opts::set_ssl_cert_dir("/system/etc/security/cacerts");
        //     debug!("{r:?}");
        //     debug!("26");
        // }
        let ans = String::new();

        // let mut callbacks = RemoteCallbacks::new();
        // callbacks.certificate_check(|_, _| Ok(CertificateCheckStatus::CertificateOk));
        // let mut fo = git2::FetchOptions::new();
        // fo.remote_callbacks(callbacks);
        // let mut builder = git2::build::RepoBuilder::new();
        // builder.fetch_options(fo);
        // let repo = builder.clone(url, input.as_ref())?;
        // let ans = repo.path().to_str().unwrap().to_string();

        // let repo = match Repository::clone(url, input) {
        //     Ok(repo) => repo,
        //     Err(e) => return Err(Box::new(e)),
        // };
        // let body = reqwest::blocking::get("https://www.rust-lang.org")?.text()?;
        // let ans = ans + &body;

        // let ans = String::from("1");
        Ok(ans)
    }

    //     use mlua::prelude::*;
    //
    //     fn main() -> LuaResult<String> {
    //         let lua = Lua::new();
    //
    //         let map_table = lua.create_table()?;
    //         map_table.set(1, "one")?;
    //         map_table.set("two", 2)?;
    //
    //         lua.globals().set("map_table", map_table)?;
    //
    //         lua.load("for k,v in pairs(map_table) do print(k,v) end")
    //             .exec()?;
    //         let x = lua.load("1 + 1").eval::<i32>()?;
    //         let x = lua.load(r#"
    // --!strict
    // f = function(a: number) : number
    //     return a+1
    // end
    // return f(100)
    //         "#).eval::<i32>()?;
    //
    //         Ok(x.to_string())
    //     }
    //     let x = main();
    //     error!("{:?}", x);

    // use wasmer::{imports, Instance, Module, Store, Value};
    //
    // fn main() -> Result<(), Box<dyn Error>> {
    //     let module_wat = r#"
    // (module
    //   (type $t0 (func (param i32) (result i32)))
    //   (func $add_one (export "add_one") (type $t0) (param $p0 i32) (result i32)
    //     get_local $p0
    //     i32.const 1
    //     i32.add))
    // "#;
    //
    // let mut store = Store::default();
    //     let module = Module::new(&store, &module_wat)?;
    //     // The module doesn't import anything, so we create an empty import object.
    //     let import_object = imports! {};
    //     let instance = Instance::new(&mut store, &module, &import_object)?;
    //
    //     let add_one = instance.exports.get_function("add_one")?;
    //     let result = add_one.call(&mut store, &[Value::I32(42)])?;
    //     assert_eq!(result[0], Value::I32(43));
    //
    //     Ok(())
    // }
    // use rustpython_vm as vm;
    //
    // error!("127");
    // fn main() -> vm::PyResult<()> {
    //     vm::Interpreter::without_stdlib(Default::default()).enter(|vm| {
    //         let scope = vm.new_scope_with_builtins();
    //         let source = r#"print("Hello World!")"#;
    //         let code_obj = vm
    //             .compile(source, vm::compiler::Mode::Exec, "<embedded>".to_owned())
    //             .map_err(|err| vm.new_syntax_error(&err, Some(source)))?;
    //
    //         vm.run_code_obj(code_obj, scope)?;
    //
    //         Ok(())
    //     })
    // }
    // let x = main();
    //
    // error!("128{x:?}");

    //   use boa_engine::{Context, JsResult, Source};
    //
    //   fn main() -> JsResult<()> {
    //       let js_code = r#"
    //     let two = 1 + 1;
    //     let definitely_not_four = two + "2";
    //
    //     definitely_not_four
    // "#;
    //
    //       // Instantiate the execution context
    //       let mut context = Context::default();
    //
    //       // Parse the source code
    //       let result = context.eval(Source::from_bytes(js_code))?;
    //
    //       println!("{}", result.display());
    //
    //       Ok(())
    //   }
    //   let x = main();
    //
    //   error!("128{x:?}");

    fn main() -> wasmtime::Result<()> {
        let engine = Engine::default();

        // Modules can be compiled through either the text or binary format
        let wat = r#"
        (module
            (import "host" "host_func" (func $host_hello (param i32)))

            (func (export "hello")
                i32.const 3
                call $host_hello)
        )
    "#;
        let module = Module::new(&engine, wat)?;

        // Host functionality can be arbitrary Rust functions and is provided
        // to guests through a `Linker`.
        let mut linker = Linker::new(&engine);
        linker.func_wrap(
            "host",
            "host_func",
            |caller: Caller<'_, u32>, param: i32| {
                println!("Got {} from WebAssembly", param);
                println!("my host state is: {}", caller.data());
            },
        )?;

        // All wasm objects operate within the context of a "store". Each
        // `Store` has a type parameter to store host-specific data, which in
        // this case we're using `4` for.
        let mut store: Store<u32> = Store::new(&engine, 4);

        // Instantiation of a module requires specifying its imports and then
        // afterwards we can fetch exports by name, as well as asserting the
        // type signature of the function with `get_typed_func`.
        let instance = linker.instantiate(&mut store, &module)?;
        let hello = instance.get_typed_func::<(), ()>(&mut store, "hello")?;

        // And finally we can call the wasm!
        hello.call(&mut store, ())?;

        Ok(())
    }
    let x = main();
    error!("{x:?}");

    let path = String::from(env.get_string(&input).unwrap());
    let out = format!("{:?}", f(path));
    env.new_string(out).unwrap().into_raw()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);
    }
}
