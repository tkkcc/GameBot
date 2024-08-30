package gamebot.host

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import gamebot.host.loader.GameKeeperRootService
import gamebot.host.loader.GameKeeperShizukuService
import gamebot.host.loader.Git
import gamebot.host.loader.PrefStore
import gamebot.host.presentation.showErrorView
import gamebot.host.presentation.showLoadingView
import com.topjohnwu.superuser.Shell
import dalvik.system.DexClassLoader
import java.io.File
import kotlin.concurrent.thread


class GameKeeperActivity : AppCompatActivity() {
    companion object {

        const val TASK_REPO_URL = "https://e.coding.net/bilabila/gamekeeper/task.git"
        const val TASK_LOCAL_RUN_CLASS = "com.gamekeeper.task.LocalRun"
        const val TASK_REMOTE_RUN_CLASS = "com.gamekeeper.task.RemoteRun"
        const val TAG = "GameKeeper"

        init {

            Log.d(TAG, "$TAG companion init")
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(Long.MAX_VALUE)
            )
        }

        @Throws(Exception::class)
        fun abi(): String {
//            TODO
            return "x86"
            val abis = Build.SUPPORTED_ABIS
            for (abi in abis) {
                if (abi in listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")) {
                    return abi
                }
            }
            throw Exception("not support abi:" + abis.toString())
        }
    }


    var taskThread: Thread? = null

    override fun onDestroy() {
        Log.d(TAG, "activity destroy begin")
        runCatching {
            destroyTask()
        }.onFailure {
            Log.d(TAG, "destroy onCreate", it)
        }
        taskThread?.interrupt()
        super.onDestroy()
        Log.d(TAG, "activity destroy end")
    }

    fun restart() {
        destroyTask()

        // for DexClassLoader and jni memory leak
        val intent = Intent(this, GameKeeperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        System.exit(0)
    }

    fun saveConfig(key: String, value: String) {
        return PrefStore.save(this, key, value)
    }

    fun loadConfig(key: String, default: String = ""): String {
        return PrefStore.load(this, key, default)
    }


    fun fetchTask(): Result<Unit> {
        val repo: String = TASK_REPO_URL
        val branch: String = abi()

        // the dex is shared to RemoteService,
        // as shell user can't access cacheDir,
        // we use externalCachedDir, usually on sdcard
        val path: String = File(externalCacheDir, "task").absolutePath
        return fetchRepo(repo, branch, path)
    }

    fun fetchRepo(
        repo: String,
        branch: String,
        path: String,
    ): Result<Unit> = runCatching {
        Log.d(TAG, "fetchRepo")
        // clone or fetch
        val key = "repo_cloned_$path"

        Log.d(TAG, "80 config value: $key ${loadConfig(key)} ")
        // if cloned, fetch and reset
        if (loadConfig(key) == "true") {
            Git.pull( path).getOrThrow()
            return Result.success(Unit)
        }
        // if not cloned, clone
        Git.clone(repo, branch, path).getOrThrow()
        saveConfig(key, "true")


        // no way to gc in jgit or git2
        // reinit after X old commit
        if (Git.count(path).getOrThrow() > 50) {
            saveConfig(key, "")
        }
    }

    lateinit var taskInstance: Any
    lateinit var taskClass: Class<*>

    @Throws
    fun destroyTask() {
        val taskDestroyFunc = taskClass.getDeclaredMethod("destroy")
        taskDestroyFunc.invoke(
            taskInstance,
        )
    }

    @Throws
    fun loadTask() {
        // we ensure the assumption for externalCacheDir
        if (externalCacheDir!!.absolutePath != "/storage/emulated/0/Android/data/$packageName/cache") {
            throw Exception("your device has special externalCacheDir: $externalCacheDir")
        }
        // load dex from disk
        val dex = File(File(externalCacheDir, "task"), "classes.dex").also{
            it.setReadOnly() // for android 14
        }.absolutePath

        val cache = File(codeCacheDir, "task").absolutePath
        File(cache).mkdirs()
        val loader = DexClassLoader(
            dex, cache, null, this.javaClass.classLoader
        )
        taskClass = loader.loadClass(TASK_LOCAL_RUN_CLASS)
        taskInstance = taskClass.getConstructor(
            AppCompatActivity::class.java, Function0::class.java,
            Class::class.java, Class::class.java,
        ).newInstance(
            this, ::restart,
            GameKeeperRootService::class.java,
            GameKeeperShizukuService::class.java
        )
//        for (method in taskClass.declaredMethods) {
//            Log.d(TAG, method.toString())
//        }
        taskClass.getDeclaredMethod("start").invoke(taskInstance)

    }


    fun toast(info: String) {
        runOnUiThread {
            Toast.makeText(this, info, Toast.LENGTH_LONG).show()
        }
    }

    fun openPackage(name: String) {
        packageManager.getLaunchIntentForPackage(name)?.apply {
//            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }?.let {
            startActivity(it)
        }
    }

    fun openPackageSetting(name: String) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:" + name)
        })
    }

    fun openIntent(data: String) {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setData(Uri.parse(data))
        })
    }

    fun installApk(path: String) {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(path), "application/vnd.android.package-archive")
        })
    }

    // scaffold + snackbarHost in dex need following keep, even when r8 off
    fun keep(){
        setContent{
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                }){
//                Text("a")
            }

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
//        GameKeeperActivity.externalCacheDir = externalCacheDir!!.absolutePath


        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
//        Log.d(TAG, externalCacheDir.toString())
//        Log.d(TAG, GameKeeperActivity.externalCacheDir.toString())
//        return

        taskThread = thread {

            showLoadingView(this)

            runCatching {
//                return@runCatching
                fetchTask().getOrThrow()
//                throw Exception("what")
                loadTask()
            }.onFailure {
                showErrorView(this, "load task fail", it.stackTraceToString(), ::restart)
            }
        }
    }
}

