use std::{error::Error, sync::OnceLock};

use jni::{
    objects::{GlobalRef, JObject},
    JNIEnv, JavaVM,
};

use super::proxy::Proxy;

static STRING_CLASS: OnceLock<GlobalRef> = OnceLock::new();
static NODE_CLASS: OnceLock<GlobalRef> = OnceLock::new();

#[derive(Debug)]
pub(crate) struct Store {
    vm: JavaVM,
    host_ref: GlobalRef,
}
pub static STORE: OnceLock<Store> = OnceLock::new();
impl Store {
    pub(crate) fn store() -> &'static Store {
        STORE.get().unwrap()
    }
    pub(crate) fn init(env: &mut JNIEnv, obj: &JObject) -> Result<(), Box<dyn Error>> {
        if STORE.get().is_some() {
            return Ok(());
        }

        let c = env.find_class("java/lang/String").unwrap();
        let c = env.new_global_ref(c).unwrap();
        // let c2: &JObject = c.as_ref();
        let _ = STRING_CLASS.set(c);

        let c = env
            .find_class("android/view/accessibility/AccessibilityNodeInfo")
            .unwrap();
        let c = env.new_global_ref(c).unwrap();
        let _ = NODE_CLASS.set(c);

        let vm = env.get_java_vm()?;
        let obj_ref = env.new_global_ref(obj)?;
        let _ = STORE
            .set(Store {
                vm,
                host_ref: obj_ref,
            })
            .unwrap();
        // .map_err(|_| Error::msg("Store set fail"))?;
        Ok(())
    }

    pub(crate) fn proxy() -> Proxy {
        let store = STORE.get().unwrap();
        let env = store.vm.attach_current_thread_permanently().unwrap();
        let host = store.host_ref.as_obj();

        Proxy::new(env, host)
    }
}
