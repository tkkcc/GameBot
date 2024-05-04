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
    let path = String::from(env.get_string(&input).unwrap());
    let url = "https://github.com/alexcrichton/git2-rs";
    let repo = match Repository::clone(url, path) {
        Ok(repo) => repo,
        Err(e) => panic!("failed to clone: {}", e),
    };
    env.new_string(repo.path().to_str().unwrap())
        .unwrap()
        .into_raw()
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
