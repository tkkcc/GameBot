use std::error::Error;

use git2::Repository;
use jni::{
    objects::{JClass, JString},
    sys::jstring,
    JNIEnv,
};

pub fn add(left: usize, right: usize) -> usize {
    left + right
}

#[no_mangle]
extern "C" fn Java_bilabila_gamebot_host_MainActivity_test(
    mut env: JNIEnv,
    class: JClass,
    input: JString,
) -> jstring {
    fn f(input: String) -> Result<String, Box<dyn Error>> {
        let url = "https://github.com/alexcrichton/git2-rs";
        let repo = match Repository::clone(url, input) {
            Ok(repo) => repo,
            Err(e) => return Err(Box::new(e)),
        };
        // let body = reqwest::blocking::get("https://www.rust-lang.org")?.text()?;
        // let body="body".into;
        let ans = repo.path().to_string_lossy().to_string();
        // let ans = repo.path().to_str().unwrap().to_string();

        Ok(ans)
    }

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
