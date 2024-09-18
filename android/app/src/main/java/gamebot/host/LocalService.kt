package gamebot.host

import CallbackMsg
import Component
import LocalCallback
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
import gamebot.host.presentation.CenterView
import initConfigUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

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

class LocalService(
    val context: ComponentActivity,
) : ILocalService.Stub() {
    val configUI: MutableState<Component> = mutableStateOf(Component.Column())
    var configUIEvent = mutableStateOf(Channel<CallbackMsg>())
    var byteBuffer = ByteBuffer.allocateDirect(0)

    init {

        context.runOnUiThread {

            initConfigUI(context, configUI, configUIEvent)
        }


    }
//            val ui = ComposeUI(context as ComponentActivity, configUI)

    override fun toast(text: String) {
        context.runOnUiThread {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    lateinit var remoteService: IRemoteService

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
                MemoryMonitor()
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
                    }
                }) {
                    Text("stop devtool")
                }

                val coroutine = rememberCoroutineScope()
                CompositionLocalProvider(LocalCallback provides { id, value ->
                    coroutine.launch {
                        configUIEvent.value.send(CallbackMsg(id, value))
                    }
                }) {
                    configUI.value.Render()
                }
            }
        }
    }

    override fun updateConfigUI(pfd: ParcelFileDescriptor) {
        val stream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
        val component: Component = Json.decodeFromStream(stream)
        configUI.value = component
        configUIEvent.value = Channel<CallbackMsg>()
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
    override fun waitConfigUIEvent(): ParcelFileDescriptor {
//        Log.e("", "waitConfigUIEvent in localservice")
        val channel = configUIEvent.value
        val event =
            runBlocking {
                buildList<CallbackMsg> {
                    try {
                        add(channel.receive())
                        while (!channel.isEmpty) {
                            add(channel.receive())
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        clear()
                    }
                }
            }

        val y = Json.encodeToString(event)
//        Log.e("", "waitConfigUIEvent in3 localservice， ${y}")

        val stream = ByteArrayOutputStream()
        Json.encodeToStream(ListSerializer(CallbackMsg.serializer()), event, stream)
        return sendLargeData(stream.toByteArray())
    }

    override fun stopConfigUIEvent() {
        configUI.value = Component.Empty()
        configUIEvent.value.close()
    }

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun addFloatingView(gravity: Int = Gravity.BOTTOM or Gravity.START): ComposeView {
        val layoutFlag: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params =
            WindowManager.LayoutParams(
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

        val textView =
            ComposeView(context).apply {
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
        val viewModelStoreOwner =
            object : ViewModelStoreOwner {
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
}
