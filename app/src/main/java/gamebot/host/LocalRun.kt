package gamebot.host

import Container
import android.content.ComponentName
import android.content.Context
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
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.ComposeView
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
import gamebot.host.IGameKeeperShizukuService
import gamebot.host.domain.Runner
import gamebot.host.loader.SaveLoadString
import gamebot.host.presentation.NavigationView
import gamebot.host.presentation.StringView
import gamebot.host.presentation.ThemeView
import gamebot.host.presentation.detail.LoadingRunner
import gamebot.host.presentation.showErrorView
import gamebot.host.presentation.showThemeView
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import dev.rikka.tools.refine.Refine
import gamebot.host.BuildConfig
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.shizuku.Shizuku.UserServiceArgs
import kotlin.concurrent.thread


@Keep
class LocalRun(
    var activity: AppCompatActivity,
    var restart: () -> Unit,

    val rootServiceClass: Class<*>,
    val shizukiServiceClass: Class<*>,
) : ILocalRun.Stub() {
    companion object {
        val TAG = "GameKeeper"
    }

    lateinit var connection: RootServiceConnection
    var localRun: ILocalRun = this
    lateinit var remoteRun: IRemoteRun
    lateinit var remoteService: IGameKeeperShizukuService
    lateinit var container: Container

    inner class RootServiceConnection() : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "RootServiceConnection onConnected")


            // why we use Messenger instead of plain AIDL:
            // the plugin run is inside RemoteHandler, that is AIDLService
            // it's needs to control ui via LocalHandler,
            // if using plain AIDL we need to define a callback interface
            // using Messenger is easier to let remote control local
            remoteService = IGameKeeperShizukuService.Stub.asInterface(service)

            remoteRun = IRemoteRun.Stub.asInterface(remoteService.remoteRun)
            container = Container(activity, localRun, remoteRun)

            val localRunBinder = this@LocalRun.asBinder()
            remoteRun.setLocalRunBinder(localRunBinder)
            remoteRun.start()

            Log.d(TAG, remoteRun.overrideDisplaySize.toString())
            Log.d(TAG, remoteRun.overrideDisplayDensity.toString())
            Log.d(TAG, remoteRun.physicalDisplayDensity.toString())
//            remoteRun.test()

        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "RootServiceConnection onDisconnected")
        }
    }

    @Keep
    fun destroy() {
//        runCatching {
//            remoteService.destroy()
//        }
//        System.exit(0)

        remoteRun.stop()
    }

    @Keep
    fun start() {
        connection = RootServiceConnection()
        Log.d(TAG, "in task entry")


        val LAST_SAVED_MODE_KEY = "lastUsedServiceMode"
        fun loadLastUsedMode(): String {
            return SaveLoadString.load(activity, LAST_SAVED_MODE_KEY)
        }

        fun saveLastUsedMode(mode: String) {
            SaveLoadString.save(activity, LAST_SAVED_MODE_KEY, mode)
        }


        fun tryRoot(onSuccess: () -> Unit, onFailure: (e: Throwable) -> Unit) {
            Shell.getShell(null) { shell ->
                if (shell.isRoot) {
                    onSuccess()

                    activity.runOnUiThread {
                        val intent = Intent(activity, rootServiceClass)
                        RootService.bind(intent, connection)
                    }

                } else {
                    onFailure(Exception())
                }
            }
        }

        fun tryShizuku(onSuccess: () -> Unit, onFailure: (error: Throwable) -> Unit) {

            Log.d(TAG, "try shizuku 1")

            Shizuku.addRequestPermissionResultListener(
                object : OnRequestPermissionResultListener {
                    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {

                        Shizuku.removeRequestPermissionResultListener(this)
                        Log.d(TAG, "onRequestPermissionResult")

                        if (grantResult != PERMISSION_GRANTED) {
                            onFailure(Exception())
                            return
                        }
                        onSuccess()

                        val userServiceArgs = UserServiceArgs(
                            ComponentName(
                                BuildConfig.APPLICATION_ID,
                                shizukiServiceClass.getName()
                            )
                        )
                            .daemon(false)
                            .processNameSuffix("service")
                            .debuggable(BuildConfig.DEBUG)
                            .version(BuildConfig.VERSION_CODE)

                        Shizuku.bindUserService(userServiceArgs, connection)
                    }
                })
            runCatching {
//                Shizuku.requestPermission(0)
            }.onFailure(onFailure)
        }



        activity.runOnUiThread {
            var errorMsg = ""
            val mode2strategy = linkedMapOf(
                "root" to ::tryRoot,
                "shizuku" to ::tryShizuku
            )
            val allMode = mode2strategy.keys.toList().let {
                if (loadLastUsedMode() == "shizuku") {
                    it.reversed()
                } else {
                    it
                }
            }
            val mode = allMode[0]
            val strategy = mode2strategy[mode]!!
            strategy({
                saveLastUsedMode(mode)
            }) {
                Log.d(TAG, "", it)
                errorMsg += it.stackTraceToString()

                // TODO i don't want to use this callback hell, but i can't turn this into coroutine:
                //  https://github.com/RikkaApps/Shizuku-API/issues/51
                val mode = allMode[1]
                val strategy = mode2strategy[mode]!!
                strategy({
                    saveLastUsedMode(mode)
                }) {
                    Log.d(TAG, "", it)
                    errorMsg += "\n" + it.stackTraceToString()
                    showErrorView(activity, "no root or shizuku permission", errorMsg, restart)
                }
            }
        }
    }

    override fun call() {
        TODO("Not yet implemented")
    }

    override fun toast(text: String) {

        Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
    }

    override fun startTask() {

        val startTime = System.currentTimeMillis()
        toast("startTask")
//        val date = "abc"
//        val byteArray = date.encodeToByteArray()
        val byteArray = ByteArray(1000 * 1000 * 5)
        val memoryFile = MemoryFile("name?", byteArray.size)
        memoryFile.writeBytes(byteArray, 0, 0, byteArray.size)
        val memoryFileHidden: MemoryFileHidden = Refine.unsafeCast(memoryFile)
        val fd = memoryFileHidden.fileDescriptor
        val pfd = ParcelFileDescriptor.dup(fd)
        remoteRun.testLargeFileTransfer(pfd)
        pfd.close()
        memoryFile.close()
        val currentTime = System.currentTimeMillis()
        Log.d(TAG, "duration ${currentTime - startTime}ms")
        // TODO: start schedule logic

        // schedule start task
        val type = "star_rail_cn"

//        remoteRun.runTask(type)
        //        TODO("Not yet implemented")
    }

    val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun addFloatingView(
        gravity: Int = Gravity.BOTTOM or Gravity.LEFT
    ): ComposeView {

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

            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//            0,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = gravity


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

        val textView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        textView.setViewTreeLifecycleOwner(lifecycleOwner)
        textView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        val viewModelStore = ViewModelStore()
        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore
                get() = viewModelStore
        }
        textView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        windowManager.addView(textView, params)
        return textView

    }

    var hideRunnerFloatScreen: () -> Unit = {}

    fun showRunnerFloatScreen(type: String) {
        var runner: Runner = LoadingRunner()

        thread {
            runner = container.fetchLoadRunner(type).onFailure {
                Log.d(TAG, it.stackTraceToString())
            }.getOrThrow()
        }.join()

        activity.runOnUiThread {
            val view = addFloatingView()
            view.setContent {
                runner.FloatScreen(container)
            }
            hideRunnerFloatScreen = {
                hideRunnerFloatScreen = {}
                runCatching {
                    windowManager.removeView(view)
                }.onFailure {
                    Log.d(TAG, it.stackTraceToString())
                }
            }
        }
    }



    var hideRestartButton: () -> Unit = {}
    fun showRestartButton() {
        activity.runOnUiThread {
            val view = addFloatingView()
            view.setContent {
                ThemeView(activity = activity, maxSize = false) {
                    FilledIconButton(restart) {
                        Icon(Icons.Default.Refresh, "back")
                    }
                }
            }
            hideRestartButton = {
                hideRestartButton = {}
                runCatching {
                    windowManager.removeView(view)
                }.onFailure {
                    Log.d(TAG, it.stackTraceToString())
                }
            }
        }
    }


    override fun showTask() {

        showRestartButton()
//        showRunnerFloatScreen("star_rail_cn")
//        hideRestartButton()

        showThemeView(activity) {
            StringView {
                NavigationView(container, localRun)
            }
        }
    }

    fun showError(head: String, body: String) {
        showErrorView(activity, head, body, restart)
    }

}