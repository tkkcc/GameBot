use std::{error::Error, sync::Arc};

use jni::{
    objects::{JByteArray, JByteBuffer, JObject, JObjectArray, JString},
    strings::JavaStr,
    JNIEnv,
};
use serde::Serialize;

use crate::{
    activity::{ActivityInfo, AppProcessInfo, PackageInfo},
    node::{ANode, Node},
    screenshot::Screenshot,
    ui::{Element, UIEvent},
};

pub(crate) struct Proxy {
    env: JNIEnv<'static>,
    host: &'static JObject<'static>,
}

impl Proxy {
    pub(crate) fn new(env: JNIEnv<'static>, host: &'static JObject<'static>) -> Self {
        Self { env, host }
    }

    pub(crate) fn take_nodeshot(&mut self) -> Vec<ANode> {
        self.env
            .with_local_frame(32, |env| -> std::result::Result<_, Box<dyn Error>> {
                let ans = env
                    .call_method(&self.host, "takeNodeshot", "()LNodeshot;", &[])
                    .unwrap();
                let obj = ans.l().unwrap();
                let data: JByteBuffer = env
                    .get_field(&obj, "data", "Ljava/nio/ByteBuffer;")
                    .unwrap()
                    .l()
                    .unwrap()
                    .into();
                // let data_raw: JObjectArray = env
                //     .get_field(&obj, "data_raw", "[LNodeInfo;")
                //     .unwrap()
                //     .l()
                //     .unwrap()
                //     .into();
                let reference: JObjectArray = env
                    .get_field(
                        &obj,
                        "reference",
                        "[Landroid/view/accessibility/AccessibilityNodeInfo;",
                    )
                    .unwrap()
                    .l()
                    .unwrap()
                    .into();

                let addr = env.get_direct_buffer_address(&data).unwrap();
                let capacity = env.get_direct_buffer_capacity(&data).unwrap();
                let data = unsafe { std::slice::from_raw_parts(addr, capacity) };

                // most time consuming part, but cbor or get_field(unchecked) not help
                let data: Vec<Arc<Node>> = serde_json::from_slice(&data).unwrap();
                let data: Vec<ANode> = data.into_iter().map(ANode).collect();
                for (i, x) in data.iter().enumerate() {
                    if i != 0 {
                        *x.parent.borrow_mut() = Arc::downgrade(&data[x.parent_idx]);
                    }

                    *x.children.borrow_mut() =
                        x.children_idx.iter().map(|&i| data[i].clone()).collect();

                    x.obj.borrow_mut().replace({
                        let o = env.get_object_array_element(&reference, i as _).unwrap();
                        let o = env.new_global_ref(o).unwrap();
                        o
                    });
                }

                Ok(data)
            })
            .unwrap()
    }

    pub(crate) fn set_config_ui<State: Serialize>(&mut self, ui: Element<State>) {
        let byte = serde_json::to_vec(&ui).unwrap();
        let value = self.env.byte_array_from_slice(&byte).unwrap();
        self.env
            .call_method(&self.host, "updateConfigUI", "([B)V", &[(&value).into()])
            .unwrap();
        self.env.delete_local_ref(value);
    }

    pub(crate) fn wait_config_ui_event<State>(&mut self) -> Vec<UIEvent<State>> {
        let event = self
            .env
            .call_method(&self.host, "waitConfigUIEvent", "()[B", &[])
            .unwrap();

        let event: JByteArray = event.l().unwrap().into();
        let event = self.env.convert_byte_array(event).unwrap();
        // d!(String::from_utf8(event.clone()).unwrap());
        serde_json::from_slice(&event).unwrap()
    }

    pub(crate) fn toast(&mut self, msg: &str) {
        let msg: JObject = self.env.new_string(&msg).unwrap().into();
        self.env
            .call_method(
                self.host,
                "toast",
                "(Ljava/lang/String;)V",
                &[(&msg).into()],
            )
            .unwrap();
        self.env.delete_local_ref(msg);
    }

    pub(crate) fn take_screenshot(&mut self) -> Screenshot {
        self.env
            .with_local_frame(4, |env| -> Result<Screenshot, Box<dyn std::error::Error>> {
                let screenshot = env
                    .call_method(self.host, "takeScreenshot", "()LScreenshot;", &[])
                    .unwrap()
                    .l()
                    .unwrap();
                let width = env
                    .get_field(&screenshot, "width", "I")
                    .unwrap()
                    .i()
                    .unwrap()
                    .try_into()
                    .unwrap();
                let height = env
                    .get_field(&screenshot, "height", "I")
                    .unwrap()
                    .i()
                    .unwrap()
                    .try_into()
                    .unwrap();
                // let pixel_stride = env
                //     .get_field(&screenshot, "pixelStride", "I")
                //     .unwrap()
                //     .i()
                //     .unwrap();
                // let row_stride = env
                //     .get_field(&screenshot, "rowStride", "I")
                //     .unwrap()
                //     .i()
                //     .unwrap();

                let data: JByteBuffer = env
                    .get_field(&screenshot, "data", "Ljava/nio/ByteBuffer;")
                    .unwrap()
                    .l()
                    .unwrap()
                    .into();

                let addr = env.get_direct_buffer_address(&data).unwrap();
                let capacity = env.get_direct_buffer_capacity(&data).unwrap();
                let data = unsafe { std::slice::from_raw_parts(addr, capacity) };
                Ok(Screenshot {
                    width,
                    height,
                    // pixel_stride,
                    // row_stride,
                    data,
                })
            })
            .unwrap()
    }

    pub(crate) fn touch_down(&mut self, x: f32, y: f32, id: i32) {
        self.env
            .call_method(
                &self.host,
                "touchDown",
                "(FFI)V",
                &[x.into(), y.into(), id.into()],
            )
            .unwrap();
    }
    pub(crate) fn touch_up(&mut self, x: f32, y: f32, id: i32) {
        self.env
            .call_method(
                &self.host,
                "touchUp",
                "(FFI)V",
                &[x.into(), y.into(), id.into()],
            )
            .unwrap();
    }
    pub(crate) fn touch_move(&mut self, x: f32, y: f32, id: i32) {
        self.env
            .call_method(
                &self.host,
                "touchMove",
                "(FFI)V",
                &[x.into(), y.into(), id.into()],
            )
            .unwrap();
    }

    pub(crate) fn click_recent(&mut self) {
        self.env
            .call_method(&self.host, "clickRecent", "()V", &[])
            .unwrap();
    }

    pub(crate) fn node_action(&mut self, obj: &JObject, i: i32) {
        self.env
            .call_method(obj, "performAction", "(I)Z", &[i.into()])
            .unwrap();
    }

    pub(crate) fn send_empty_config_ui_event(&mut self) {
        self.env
            .call_method(self.host, "sendEmptyConfigUIEvent", "()V", &[])
            .unwrap();
    }

    pub(crate) fn current_activity(&mut self) -> ActivityInfo {
        let obj: JString = self
            .env
            .call_method(self.host, "currentActivity", "()Ljava/lang/String;", &[])
            .unwrap()
            .l()
            .unwrap()
            .into();
        let x: String = JavaStr::from_env(&self.env, &obj).unwrap().into();
        self.env.delete_local_ref(obj);
        serde_json::from_str(&x).unwrap()
    }

    pub(crate) fn running_activity_list(&mut self) -> Vec<ActivityInfo> {
        let obj: JString = self
            .env
            .call_method(
                self.host,
                "runningActivityList",
                "()Ljava/lang/String;",
                &[],
            )
            .unwrap()
            .l()
            .unwrap()
            .into();
        let x: String = JavaStr::from_env(&self.env, &obj).unwrap().into();
        self.env.delete_local_ref(obj);
        serde_json::from_str(&x).unwrap()
    }

    pub(crate) fn running_app_process_list(&mut self) -> Vec<AppProcessInfo> {
        let obj: JString = self
            .env
            .call_method(
                self.host,
                "runningAppProcessList",
                "()Ljava/lang/String;",
                &[],
            )
            .unwrap()
            .l()
            .unwrap()
            .into();
        let x: String = JavaStr::from_env(&self.env, &obj).unwrap().into();
        self.env.delete_local_ref(obj);
        serde_json::from_str(&x).unwrap()
    }

    pub(crate) fn installed_package_list(&mut self) -> Vec<PackageInfo> {
        let obj: JString = self
            .env
            .call_method(
                self.host,
                "installedPackageList",
                "()Ljava/lang/String;",
                &[],
            )
            .unwrap()
            .l()
            .unwrap()
            .into();
        let x: String = JavaStr::from_env(&self.env, &obj).unwrap().into();
        self.env.delete_local_ref(obj);
        serde_json::from_str(&x).unwrap()
    }

    pub(crate) fn activity_list(&mut self, package: &str) -> Vec<String> {
        let name: JObject = self.env.new_string(package).unwrap().into();
        let obj: JString = self
            .env
            .call_method(
                self.host,
                "activityList",
                "(Ljava/lang/String;)Ljava/lang/String;",
                &[(&name).into()],
            )
            .unwrap()
            .l()
            .unwrap()
            .into();

        let x: String = JavaStr::from_env(&self.env, &obj).unwrap().into();

        self.env.delete_local_ref(obj);
        self.env.delete_local_ref(name);
        serde_json::from_str(&x).unwrap()
    }

    pub(crate) fn launch_activity(&mut self, package: &str) -> String {
        let name: JObject = self.env.new_string(package).unwrap().into();
        let obj: JString = self
            .env
            .call_method(
                self.host,
                "launchActivity",
                "(Ljava/lang/String;)Ljava/lang/String;",
                &[(&name).into()],
            )
            .unwrap()
            .l()
            .unwrap()
            .into();

        let x: String = JavaStr::from_env(&self.env, &obj).unwrap().into();

        self.env.delete_local_ref(obj);
        x
    }
}
