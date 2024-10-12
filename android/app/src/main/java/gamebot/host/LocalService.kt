package gamebot.host

//import CallbackMsg
//import ai.onnxruntime.OnnxTensor
//import ai.onnxruntime.OrtEnvironment
//import ai.onnxruntime.OrtSession
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.text.TextRecognition
//import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import Component
import LocalUIEvent
import UIEvent
import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
//import com.ketch.Ketch
import gamebot.host.presentation.CenterView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.concurrent.thread

@Composable
fun MemoryMonitor() {
    var memory by remember { mutableStateOf(0L) }
    var memoryTotal by remember { mutableStateOf(0L) }
    val context = LocalContext.current
    LaunchedEffect(true) {
        val memoryInfo = ActivityManager.MemoryInfo()
        val manager = (context.getSystemService(ACTIVITY_SERVICE) as ActivityManager)

        while (true) {
            manager.getMemoryInfo(memoryInfo)
            memory = (memoryInfo.availMem) / 1000 / 1000
            memoryTotal = (memoryInfo.totalMem) / 1000 / 1000
            kotlinx.coroutines.delay(500)
        }
    }
    Text("memory free: $memory MB, total: $memoryTotal MB")
}


data class ConfigUI(
    val layout: MutableState<Component> = mutableStateOf(Component.Empty()),
    val event: MutableState<Channel<UIEvent>> = mutableStateOf(Channel())
)


class LocalService(
    val context: ComponentActivity,
) : ILocalService.Stub() {
//    val configUI: MutableState<Component> = mutableStateOf(Component.Column())
//    var configUIEvent = mutableStateOf(Channel<UIEvent>())

    val configUIList = mutableMapOf<Int, ConfigUI>()

    override fun toast(text: String) {
        context.runOnUiThread {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    lateinit var remoteService: IRemoteService

//    override fun startPackage(packageName: String) {
////        Binder.clearCallingIdentity()
//        Log.e("gamebot", "106 " + packageName + " ${Binder.getCallingUid()}" )
//        context.startActivity(context.packageManager.getLaunchIntentForPackage(packageName))
//
//    }

//    override fun startActivity(packageName: String, className: String) {
//        Log.e("gamebot", "107 " + packageName + " " + className + " ${Binder.getCallingUid()}")
//        runCatching {
//            //        Binder.clearCallingIdentity()
//            val intent = android.content.Intent().apply {
//                setClassName(packageName, className)
//            }
//            context.startActivity(intent)
//        }.onFailure {
//            Log.e("gamebot", "can't start activity", it)
//        }
//
//    }

    //    fun testmlkitocr() {
//        Log.e("gamebot", "135")
////        MlKit.initialize(context)
//        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
////        val originalBitmap = BitmapFactory.decodeFile("/data/local/tmp/multilinetext.png")
////        val resizedBitmap: Bitmap = Bitmap.createScaledBitmap(
////            originalBitmap, 128, 64, false
////        )
//
//        val originalBitmap = BitmapFactory.decodeFile("/data/local/tmp/longsingleline.png")
//        val resizedBitmap: Bitmap = Bitmap.createScaledBitmap(
//            originalBitmap, (originalBitmap.width * 48 / originalBitmap.height), 48, false
//        )
//
//        val image = InputImage.fromBitmap(resizedBitmap, 0)
//
////        val image = InputImage.fromFilePath(context, Uri.fromFile(File("/data/local/tmp/multilinetext.png"))
//
//        fun runOnce() {
//            runBlocking {
//
//                suspendCoroutine { cont ->
//
//                    val result = recognizer.process(image)
//                        .addOnSuccessListener { result ->
//                            cont.resume(Unit)
////                        val resultText = result.text
////                        for (block in result.textBlocks) {
////                            val blockText = block.text
////                            val blockCornerPoints = block.cornerPoints
////                            val blockFrame = block.boundingBox
////                            for (line in block.lines) {
////                                val lineText = line.text
////                                val lineCornerPoints = line.cornerPoints
////                                val lineFrame = line.boundingBox
////                                for (element in line.elements) {
////                                    val elementText = element.text
////                                    Log.e("gamebot", elementText)
////                                    val elementCornerPoints = element.cornerPoints
////                                    val elementFrame = element.boundingBox
////                                }
////                            }
////                        }
//                        }
//                        .addOnFailureListener { e ->
//                            // Task failed with an exception
//                            // ...
//                        }
//                }
//            }
//
//        }
//
//        runOnce()
//        val start = SystemClock.uptimeMillis()
//
//        for (i in 0..<10) {
//            runOnce()
////            Log.e("gamebot", i.toString())
////            val result = recognizer.process(image)
//        }
//        Log.e("gamebot", ((SystemClock.uptimeMillis() - start) / 10).toString())
//
//    }
//
//    fun testddddocr() {
//        val ortEnv = OrtEnvironment.getEnvironment()
//        Log.e("gamebot", "providers ${OrtEnvironment.getAvailableProviders()}")
//        val option = OrtSession.SessionOptions()
////        option.addNnapi()
////        option.addCPU(true)
////        option.addXnnpack(mutableMapOf(
////            "intra_op_num_threads" to "4"
////        ))
//        val ortSession = ortEnv.createSession("/data/local/tmp/ddddocr.onnx", option)
//
////        val originalBitmap = BitmapFactory.decodeFile("/data/local/tmp/79.png")
//        val originalBitmap = BitmapFactory.decodeFile("/data/local/tmp/longsingleline.png")
//        val resizedBitmap: Bitmap = Bitmap.createScaledBitmap(
//            originalBitmap, (originalBitmap.width * 64 / originalBitmap.height), 64, false
//        )
//        Log.e("gamebot", resizedBitmap.config.toString())
//        val byteBuffer =  ByteBuffer.allocate(resizedBitmap.byteCount)
////        resizedBitmap.copyPixelsToBuffer(byteBuffer)
//        byteBuffer.position(0)
////        val firstChannel = ByteBuffer.allocate(resizedBitmap.byteCount/4)
////        firstChannel.put(byteBuffer)
////        FloatBuffer.allocate()
////        val image = InputImage.fromBitmap(originalBitmap, 0)
////        val byteBuffer =
////        val i0 = OnnxTensor.createTensor(ortEnv, firstChannel, longArrayOf(1,1,64, resizedBitmap.width.toLong()),OnnxJavaType.UINT8)
//        val i0 = OnnxTensor.createTensor(ortEnv, byteBuffer.asFloatBuffer(), longArrayOf(1,1,64, resizedBitmap.width.toLong()))
//        val input = mutableMapOf(ortSession.inputNames.first() to i0)
//        val out = ortSession.run(input)
//        Log.e("gamebot","start")
//        val start = SystemClock.uptimeMillis()
//        for (i in 0..<10) {
//            ortSession.run(input)
//        }
//        Log.e("gamebot","time ${(SystemClock.uptimeMillis()-start)/10 }ms")
//        out.use {
//            val output = it.get(0).value
////            Log.e("gamebot", (output).size.toString())
////
//        }
//
//
//
//        ortSession.close()
//    }
//
//    fun testpaddleocr() {
//        val ortEnv = OrtEnvironment.getEnvironment()
//        Log.e("gamebot", "providers ${OrtEnvironment.getAvailableProviders()}")
//        val option = OrtSession.SessionOptions()
////        option.addXnnpack(emptyMap())
////        option.addNnapi()
////        option.addCPU(true)
////        option.addXnnpack(mutableMapOf(
////            "intra_op_num_threads" to "4"
////        ))
//        val ortSession = ortEnv.createSession("/data/local/tmp/rec.onnx", option)
//
////        val originalBitmap = BitmapFactory.decodeFile("/data/local/tmp/79.png")
//        val originalBitmap = BitmapFactory.decodeFile("/data/local/tmp/longsingleline.png")
//        val resizedBitmap: Bitmap = Bitmap.createScaledBitmap(
//            originalBitmap, (originalBitmap.width * 48 / originalBitmap.height), 48, false
//        )
//        Log.e("gamebot", resizedBitmap.config.toString())
//        val byteBuffer =  ByteBuffer.allocate(resizedBitmap.byteCount)
//
////        resizedBitmap.copyPixelsToBuffer(byteBuffer)
//        byteBuffer.position(0)
//        val buf = ByteBuffer.allocate(resizedBitmap.byteCount*3)
////        byteBuffer.position(0)
////        buf.put(byteBuffer)
////        byteBuffer.position(0)
////        buf.put(byteBuffer)
////        byteBuffer.position(0)
////        buf.put(byteBuffer)
////        buf.position(0)
//
////        val firstChannel = ByteBuffer.allocate(resizedBitmap.byteCount/4)
////        firstChannel.put(byteBuffer)
////        FloatBuffer.allocate()
////        val image = InputImage.fromBitmap(originalBitmap, 0)
////        val byteBuffer =
////        val i0 = OnnxTensor.createTensor(ortEnv, firstChannel, longArrayOf(1,1,64, resizedBitmap.width.toLong()),OnnxJavaType.UINT8)
//        val i0 = OnnxTensor.createTensor(ortEnv, buf.asFloatBuffer(), longArrayOf(1,3,48, resizedBitmap.width.toLong()))
//        val input = mutableMapOf(ortSession.inputNames.first() to i0)
//        val out = ortSession.run(input)
//        Log.e("gamebot","start")
//        val start = SystemClock.uptimeMillis()
//        for (i in 0..<10) {
//            ortSession.run(input)
//        }
//        Log.e("gamebot","time ${(SystemClock.uptimeMillis()-start)/10 }ms")
//        out.use {
//            val output = it.get(0).value
////            Log.e("gamebot", (output).size.toString())
////
//        }
//
//
//
//        ortSession.close()
//    }
//
    lateinit var t0: Thread
    override fun test() {


        context.setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            CenterView {
                Button({
                    (context as MainActivity).restart()
//                    context.startActivity(Intent(context, MainActivity::class.java))
//                    context.stopService(Intent(context, LocalService::class.java))
//                    exitProcess(0)
                }) {
                    Text("restart")
                }
//                MemoryMonitor()
                var isRunning by remember { mutableStateOf(false) }
//                var pluginRunningState =
                Text(isRunning.toString())
                Button({
                    scope.launch(Dispatchers.Default) {
                        isRunning = true
                        Log.e("", "start guest")
                        remoteService.startGuest("devtool")
                        Log.e("", "start guest done")
                        isRunning = false
                    }
                }) {
                    Text("start devtool")
                }
                Button({
                    scope.launch(Dispatchers.Default) {
                        Log.e("", "stop guest")
                        remoteService.stopGuest("devtool")
//                        startPackage("gamebot.host")
                    }
                }) {
                    Text("stop devtool")
                }

                configUIList.getOrPut(0, { ConfigUI() }).apply {
                    CompositionLocalProvider(LocalUIEvent provides { id, value ->
                        event.value.trySend(UIEvent.Callback(id, value))
                    }) {
                        layout.value.Render()
                    }
                }
            }
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    override fun updateConfigUI(token: Int, pfd: ParcelFileDescriptor) {
        val stream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
        val component: Component = Json.decodeFromStream(stream)

        configUIList.getOrPut(token, {
            ConfigUI()
        }).apply {
            layout.value = component
            event.value = Channel(8, BufferOverflow.DROP_LATEST)
        }
    }

    override fun sendEmptyConfigUIEvent(token: Int) {
        configUIList[token]?.apply {
            event.value.trySend(UIEvent.Empty)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
    override fun waitConfigUIEvent(token: Int): ParcelFileDescriptor {

        val event = configUIList[token]?.let {
            val channel = it.event.value
            buildList<UIEvent> {
                try {
                    runBlocking {
                        add(channel.receive())
                        while (!channel.isEmpty) {
                            add(channel.receive())
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    clear()
                    add(UIEvent.Exit)
                }
            }
        } ?: emptyList()


        val stream = ByteArrayOutputStream()
        Json.encodeToStream(ListSerializer(UIEvent.serializer()), event, stream)
//        Log.e("gamebot",  stream.toString())
        return sendLargeData(stream.toByteArray())
    }

    override fun clearConfigUI(token: Int) {
        configUIList[token]?.apply {
            event.value.close()
            layout.value = Component.Empty()
        }
        configUIList.remove(token)
    }

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun addFloatingView(gravity: Int = Gravity.BOTTOM or Gravity.START): ComposeView {
        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            100,
            100,
            layoutFlag,
//                    0,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
//                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
//                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
//                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//            0,
            PixelFormat.TRANSLUCENT,
        )
//                params.gravity = gravity

        class MyLifecycleOwner : SavedStateRegistryOwner {
            private var mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
            private var mSavedStateRegistryController: SavedStateRegistryController =
                SavedStateRegistryController.create(this)

            fun handleLifecycleEvent(event: Lifecycle.Event) {
                mLifecycleRegistry.handleLifecycleEvent(event)
            }

            fun performRestore(savedState: Bundle?) {
                mSavedStateRegistryController.performRestore(savedState)
            }

            override val lifecycle: Lifecycle
                get() = mLifecycleRegistry
            override val savedStateRegistry: SavedStateRegistry
                get() = mSavedStateRegistryController.savedStateRegistry
        }

        val textView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
//                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
        textView.setContent {
            var text by remember {
                mutableStateOf("a111111")
            }
            TextField(text, {
                text = it
            })
        }

        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        textView.setViewTreeLifecycleOwner(lifecycleOwner)
        textView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        val viewModelStore = ViewModelStore()
        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore
                get() = viewModelStore
        }
        textView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)

        val v = EditText(context)
        val vv = LinearLayout(context)

//                windowManager.addView(vv, params)
        val vvv = ComposeView(context)
        vvv.setViewTreeViewModelStoreOwner(context)

        windowManager.addView(textView, params)
        return textView
    }

    override fun cacheDir(): String {
        return context.cacheDir.absolutePath
    }


}
