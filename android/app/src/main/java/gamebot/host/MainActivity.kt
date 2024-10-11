package gamebot.host

//import com.github.only52607.compose.window.ComposeFloatingWindow
//import com.xrubio.overlaytest.overlay.OverlayService
import MyLifecycleOwner
import RemoteService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.MemoryFile
import android.os.MemoryFileHidden
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import dev.rikka.tools.refine.Refine
import gamebot.host.RemoteRun.Companion.TAG
import gamebot.host.loader.Git
import gamebot.host.overlay.Overlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.shizuku.Shizuku.UserServiceArgs
import java.io.File
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class MainActivity : ComponentActivity() {
    companion object {
        // this will be called during Activity reinit, otherwise UiAutomation connection will fail
        // when debug, we remove orientation from android:configChanges and make rotations
        // in practice, i guess this will be used on oom killer
        var cleanPreviousRemoteService = {}
    }

    init {
//        System.loadLibrary("rust")
        Log.e("", "init ")
    }

//    external fun test(x: String): String

    //    override fun onStop() {
//        super.onStop()
//        cleanPreviousRemoteService()
//    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.e("", applicationInfo.nativeLibraryDir)
        thread {
//             Log.e("", "isTestThreadAlive()"+isTestThreadAlive())

            val path = File(cacheDir, "repo")
            path.mkdirs()
//            d("git clone start: $path")
//            Git.clone("http://www.modelscope.cn/bilabila/test1.git", path.absolutePath)
//            Git.clone("https://e.coding.net/bilabila/gamekeeper/star_rail_cn.git", path.absolutePath)
//            d("git clone finish: $path")

//            val out = test(path.absolutePath)
//            Log.e("", out)

            test()
//            Log.e("", out + "..2")

        }

        val text = mutableStateOf("...")

        val view = ComposeView(this)
        lateinit var floatingWindow: ComposeFloatingWindow
        setContentView(view)
        view.setContent {
            val context = LocalContext.current
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        TextButton(onClick = {
                            thread {
                                text.value = "running..."
                            }
//                            startService(Intent(context, OverlayService::class.java))
                            if (floatingWindow.showing) {
                                floatingWindow.hide()
                            } else {
                                floatingWindow.show()
                            }
                        }) {
                            Text("tap")
                        }
                        Text(text = text.value)
                    }
                }
            }
        }

//        startService(Intent(this, OverlayService::class.java))

        fun showOverlay() {
            val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                // https://developer.android.com/reference/android/view/WindowManager.LayoutParams
                // alt: WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                // WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                        or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM

                ,
                PixelFormat.TRANSLUCENT
            )

            val composeView = ComposeView(this)
            composeView.setContent {
                Overlay(onClick = {
                    Log.w("OverlayService", "*** Logging something from the overlay service")
                    Toast.makeText(applicationContext, "Hey!", Toast.LENGTH_SHORT).show()
                })
            }

            // Trick The ComposeView into thinking we are tracking lifecycle
            val viewModelStoreOwner = object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore
                    get() = ViewModelStore()
            }
            val lifecycleOwner = MyLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            composeView.setViewTreeLifecycleOwner(lifecycleOwner)
            composeView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            // This is required or otherwise the UI will not recompose
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            windowManager.addView(composeView, params)
        }
//        showOverlay()
//        floatingWindow = ComposeFloatingWindow(applicationContext)
//        floatingWindow.setContent {
//            MaterialTheme {
////                Scaffold() { padding ->
//                Column() {
////                        TextButton(onClick = {
////                            thread {
////                                text.value = "running..."
////
////                            }
////
////                        }) {
////                            Text("tap")
////                        }
////                        Text(text = text.value)
//                    var text by remember {
//                        mutableStateOf("aaa")
//                    }
//                    TextField(text, {
//                        text = it
//                    })
//                }
////                }
//            }
//
//        }
//        floatingWindow.show()

    }


    @OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class)
    @SuppressLint("SetJavaScriptEnabled")
    private fun test() {
        cleanPreviousRemoteService()


        runOnUiThread {
//            setContent {
//                var text by remember {
//                    mutableStateOf("bbb")
//                }
//                TextField(text, {
//                    text = it
//                })
//            }
//            val floatingWindow = ComposeFloatingWindow(applicationContext)
//            floatingWindow.setContent {
//                var text by remember {
//                    mutableStateOf("aaa")
//                }
//                TextField(text, {
//                    text = it
//                })
////                FloatingActionButton(
////                    modifier = Modifier.dragFloatingWindow(),
////                    onClick = {
////                        Log.d("", "tap floating button")
////                    }) {
////                    Icon(Icons.Filled.Call, "Call")
////                }
//            }
//            floatingWindow.show()
//            setContent {
//                MaterialTheme {
//                    Scaffold(
//                        modifier = Modifier.fillMaxSize(), containerColor = Color.White
//                    ) { innerPadding ->
//                        Column(modifier = Modifier.padding(innerPadding)) {
//                            var text by remember {
//                                mutableStateOf("text")
//                            }
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Text(text = text)
//                                TextField(value = text, onValueChange = {
//                                    Log.d("", "value changed to " + it)
//                                    text = it
//                                })
//                                IconButton({
////                                    toast("button")
//                                    text += "1"
//                                }) {
//                                    Icon(Icons.Default.Android, "back")
//                                }
//                            }
////
////                            val state = rememberWebViewState("http://192.168.0.178:8080/")
////
////                            WebView(state, onCreated = {
////                                it.settings.javaScriptEnabled = true
////                                /** Instantiate the interface and set the context.  */
////                                class WebAppInterface(private val mContext: Context) {
////
////                                    /** Show a toast from the web page.  */
////                                    @JavascriptInterface
////                                    fun showToast(toast: String) {
////                                        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
////                                    }
////                                }
////                                it.addJavascriptInterface(
////                                    WebAppInterface(this@MainActivity), "Android"
////                                )
////                            })
//                        }
//                    }
//                }
//            }
        }

        lateinit var remoteService: IRemoteService
        val localService: ILocalService = LocalService(this@MainActivity)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.e("137", "RootServiceConnection onConnected")

                remoteService = IRemoteService.Stub.asInterface(service)
                cleanPreviousRemoteService = {
                    runCatching {
                        remoteService.destroy()
                    }
                    Thread.sleep(1000)
                }
                remoteService.setLocalRunBinder(localService.asBinder())
                (localService as LocalService).remoteService = remoteService
                remoteService.start()

//                previousRemoteService = remoteService
//
//                Log.e("137", "::tmpService is init?"+::tmpService.isInitialized.toString())

//                remoteService.destroy()
                Log.e("137", "RootServiceConnection onConnected end")
                // why we use Messenger instead of plain AIDL:
                // the plugin run is inside RemoteHandler, that is AIDLService
                // it's needs to control ui via LocalHandler,
                // if using plain AIDL we need to define a callback interface
                // using Messenger is easier to let remote control local
//                remoteService = IGameKeeperShizukuService.Stub.asInterface(service)
//
//                remoteRun = IRemoteRun.Stub.asInterface(remoteService.remoteRun)
//                container = Container(activity, localRun, remoteRun)
//
//                val localRunBinder = this@LocalRun.asBinder()
//                remoteRun.setLocalRunBinder(localRunBinder)
//                remoteRun.start()
//
//                Log.d(TAG, remoteRun.overrideDisplaySize.toString())
//                Log.d(TAG, remoteRun.overrideDisplayDensity.toString())
//                Log.d(TAG, remoteRun.physicalDisplayDensity.toString())
//                remoteRun.test()

            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.e("137", "onServiceDisconnected")
            }
        }

        fun tryRootMode(): Result<Unit> = runCatching {
            val shell = Shell.getShell()
            if (!shell.isRoot) {
                throw Exception("no root")
            }
            class GameBotRootService : RootService() {
                override fun onBind(intent: Intent): IBinder = RemoteService(this)
            }

            val intent = Intent(this@MainActivity, GameBotRootService::class.java)
            runOnUiThread {
                RootService.bind(intent, connection)
            }
        }

        fun tryShizukuMode(): Result<Unit> = runCatching {
            val grantResult = runBlocking {
                withContext(Dispatchers.Main) {
                    suspendCoroutine { continuation ->
                        Shizuku.addRequestPermissionResultListener(object :
                            OnRequestPermissionResultListener {
                            override fun onRequestPermissionResult(
                                requestCode: Int, grantResult: Int
                            ) {
                                Shizuku.removeRequestPermissionResultListener(this)
                                continuation.resume(grantResult)
                            }
                        })
                        Shizuku.requestPermission(0)
                    }
                }
            }
            if (grantResult != PERMISSION_GRANTED) {
                throw Exception("no shizuku")
            }
            val userServiceArgs = UserServiceArgs(
                ComponentName(
                    BuildConfig.APPLICATION_ID, RemoteService::class.java.name
                )
            ).daemon(false).processNameSuffix("service").debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)

            Shizuku.bindUserService(userServiceArgs, connection)
        }


        tryRootMode().onFailure {
            tryShizukuMode()
        }
        Log.d(TAG, "tryRootMode end")

    }

    fun restart() {
//        destroyTask()
//
        // for DexClassLoader and jni memory leak
        val intent = Intent(this, MainActivity::class.java)

//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        finish()

        startActivity(intent)
        System.exit(0)
    }
}

// transfer over 1M via AIDL
fun sendLargeData(byteArray: ByteArray):ParcelFileDescriptor {

    val memoryFile = MemoryFile(null, byteArray.size)
    memoryFile.writeBytes(byteArray, 0, 0, byteArray.size)
    val memoryFileHidden: MemoryFileHidden = Refine.unsafeCast(memoryFile)

    val fd = memoryFileHidden.fileDescriptor

    val pfd = ParcelFileDescriptor.dup(fd)

    // close before will only do ref-1
    memoryFile.close()

    return pfd
//    Log.e("","after memory closed")

//    memoryFile.close()
}

//fun receiveLargeData(func: (ParcelFileDescriptor)->Unit): ByteArray {
//
//    return ParcelFileDescriptor.AutoCloseInputStream(pfd).readBytes()
//}