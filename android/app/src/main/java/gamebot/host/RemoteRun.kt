package gamebot.host

import android.app.UiAutomation
import android.app.UiAutomationConnection
import android.app.UiAutomationHidden
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.os.Binder
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.os.ServiceManager
import android.os.SystemClock
import android.util.Log
import android.view.IWindowManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.Keep
import gamebot.host.debug.DebugServer
import gamebot.host.domain.Runner
import dalvik.system.DexClassLoader
import dev.rikka.tools.refine.Refine
import gamebot.host.loader.JsonStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import kotlin.concurrent.thread
import kotlin.math.roundToInt

@Keep
class RemoteRun(val context: Context) : IRemoteRun.Stub() {
    companion object {
        val TAG = "GameKeeper"

        const val ROOT_DIR = "/data/local/tmp/gamekeeper"
        const val CACHE_DIR = "$ROOT_DIR/cache"
        const val CODE_CACHE_DIR = "$ROOT_DIR/code_cache"

        enum class RemoteEvent {
            Start,
            Stop,
            Destroy,
            SetOverrideDisplaySize,
            StartTask,
            GetOverrideDisplaySize, ;

            companion object {
            }

        }

        init {
            Log.d(TAG, "RemoteHandler companion init")


        }
    }


    private lateinit var debugServer: DebugServer
    private lateinit var taskThread: Thread
    private lateinit var uiAutomationHidden: UiAutomationHidden
    private lateinit var uiAutomationConnection: UiAutomationConnection
    private lateinit var uiAutomation: UiAutomation
    private val handlerThread = HandlerThread("GameKeeperHandlerThread")


    lateinit var mWm: IWindowManager
    val state = JsonStore("$ROOT_DIR/state.json")
    lateinit var localMessenger: Messenger

    fun connectUiAutomation() {

        handlerThread.start()

        uiAutomationConnection = UiAutomationConnection()
        uiAutomationHidden = UiAutomationHidden(handlerThread.looper, uiAutomationConnection)

        Log.d(TAG, "UiAutomation connect")
//        if (true) {
//            runCatching {
//                uiAutomationHidden.connect(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
//            }.onFailure {
//                Log.d(TAG, "connect 1", it)
//
//            }.onSuccess {
//                Log.d(TAG, "connect 1 success")
//            }
//            Thread.sleep(1000)
//            runCatching {
//                uiAutomationHidden.disconnect()
//            }.onFailure {
//                Log.d(TAG, "disconnect 1", it)
//
//            }.onSuccess {
//
//                Log.d(TAG, "disconnect 1 success")
//            }
//
//        }
//        Thread.sleep(1000)


//            runCatching {
//                uiAutomationHidden.disconnect()
//            }.onFailure {
//                Log.d(TAG, "connectUiAutomation disconnect fail", it)
//            }
//        runCatching {
//
//            Log.d(TAG, "connectUiAutomation after connect")
//        }.onFailure {
//            Log.d(TAG, "connect 2 fail ", it)
//        }.onSuccess {
//            Log.d(TAG, "connect 2 success ")
//        }
//        Log.d(
//            TAG, "callinguid on connect ${Binder.getCallingUid()} ${Binder.getCallingPid()} " +
//                    "${android.os.Process.SYSTEM_UID}"
//        )
        uiAutomationHidden.connect(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        uiAutomation = Refine.unsafeCast(uiAutomationHidden)
    }

    var cacheDir: String = CACHE_DIR


    init {
        Log.d(TAG, "RemoteHandler instance init")
        init()
    }

    fun loadRunner(type: String): Runner {
        val contextExternalCacheDir =
            "/storage/emulated/0/Android/data/${context.packageName}/cache"
        val dex = File(
            File(contextExternalCacheDir, type),
            "classes.dex"
        ).also { it.setReadOnly() }.absolutePath

        val CODE_CACHE_DIR = "/data/local/tmp/gamekeeper/code_cache"
        val cache = File(CODE_CACHE_DIR, type).absolutePath
        File(cache).mkdirs()

        // load dex from disk
        val loader = DexClassLoader(
            dex, cache, null, this.javaClass.classLoader
        )
        val taskClass = loader.loadClass("com.gamekeeper.$type.MainRunner")
        return taskClass.getConstructor().newInstance() as Runner
    }

    fun init() {
        // we can connect to app's database in shell uid?
        // no, it's hang

//        container = Container(context)
//        context.applicationContext

        // for android >=13, after clear uid, it's just shell / root uid
        Binder.clearCallingIdentity()

        mWm = IWindowManager.Stub.asInterface(
            ServiceManager.checkService(
                Context.WINDOW_SERVICE
            )
        )

        connectUiAutomation()
        grantOverlayPermission()

        val element = Json.parseToJsonElement(
            """
        {
            "name": "kotlinx.serialization",
            "forks": [{"votes": 42}, {"votes": 9000}, {}],
                 "n":{
              "a":2
            }
        }
    """
        )
        val element2 = Json.parseToJsonElement(
            """
        {
            "name": "kotlinx.serialization1",
            "forks": [{"votes": 42}, {"votes": 9000}, {}],
            "n":{
              "a":1
            }
        }
    """
        )
        val element3 = Json.parseToJsonElement(
            """
        {
            "forks": [{"votes": 42}, {"votes": 9000}, []],
            "n":{
              "a":4,
              "b":[]
            }
        }
    """
        )

        val diff = jsonDiff(element, element2)
        diff?.let {

            Log.d(TAG, it.toString())
            Log.d(TAG, jsonApplyDiff(element3, it).toString())
        }

//        val sum = element
//            .jsonObject["forks"]!!
//            .jsonArray.sumOf { it.jsonObject["votes"]?.jsonPrimitive?.int ?: 0 }
//        element.jsonObject.entries.forEach {
//            it.value.jsonPrimitive
//        }
        recursiveJsonLog(element)
//        Log.d(TAG, "sum is $sum")

//        setStandardDisplay()
    }

    fun recursiveJsonLog(element: JsonElement) {
        when (element) {
            is JsonObject -> {
                element.jsonObject.entries.forEach {
                    Log.d(TAG, it.key + " : ")
                    recursiveJsonLog(it.value)
                }
            }

            is JsonArray -> {
                element.jsonArray.forEach {
                    recursiveJsonLog(it)
                }
            }

            is JsonPrimitive -> {
                Log.d(TAG, element.toString())
            }
        }

    }

    fun jsonApplyDiff(old: JsonElement, diff: JsonElement): JsonElement {
        if (diff is JsonObject && old is JsonObject) {
            return buildJsonObject {
                old.entries.forEach { oldEntry ->
                    val key = oldEntry.key
                    val value = oldEntry.value
                    put(key, value)
                }

                diff.entries.forEach { diffEntry ->
                    val key = diffEntry.key
                    val value = diffEntry.value
                    old[key]?.let { oldValue ->
                        put(key, jsonApplyDiff(oldValue, value))
                    } ?: run {
                        put(key, value)
                    }
                }
            }

        }
        return diff
    }

    fun jsonDiff(old: JsonElement, new: JsonElement): JsonElement? {
        if (new is JsonObject && old is JsonObject) {
            return buildJsonObject {
                new.entries.forEach { newEntry ->
                    val key = newEntry.key
                    val value = newEntry.value
                    old.get(key)?.let { oldValue ->
                        jsonDiff(oldValue, value)?.let {
                            put(key, it)
                        }
                    } ?: run {
                        put(key, value)
                    }
                }
            }
        }
        if (new != old) {
            return new
        }

        return null
    }

    fun touchDown() {

    }

    fun touchUp() {

    }

    fun getPhysicalDisplaySize(): Point = Point().apply {
        mWm.getInitialDisplaySize(0, this)
    }

    override fun getOverrideDisplaySize(): Point = Point().apply {
        mWm.getBaseDisplaySize(0, this)
    }

    override fun getPhysicalDisplayDensity(): Int = mWm.getInitialDisplayDensity(0)

    lateinit var localRun: ILocalRun
    override fun setLocalRunBinder(binder: IBinder?) {
        localRun = ILocalRun.Stub.asInterface(binder)
    }

    override fun getOverrideDisplayDensity(): Int = mWm.getBaseDisplayDensity(0)

    override fun setOverrideDisplaySize(point: Point) {
        mWm.setForcedDisplaySize(0, point.x, point.y)
    }

    fun setOverrideDensitySize(density: Int) {
        // android >= 7.1

        if (Build.VERSION.SDK_INT >= 25) {
            // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/nougat-mr1-dev/services/core/java/com/android/server/wm/WindowManagerService.java
            mWm.setForcedDisplayDensityForUser(0, density, 0)
            Log.d(TAG, "setForcedDisplayDensityForUser success")
        } else {
            // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/nougat-dev/services/core/java/com/android/server/wm/WindowManagerService.java

            mWm.setForcedDisplayDensity(0, density)
            Log.d(TAG, "setForcedDisplayDensity success")
        }
    }

    fun setStandardDisplay() {
        // 720p + 320dpi
        val physical = getPhysicalDisplaySize()
        val target = if (physical.x > physical.y) {
            Point(1280, 720)
        } else {
            Point(720, 1280)
        }
        setOverrideDisplaySize(target)
        setOverrideDensitySize(320)
    }


    // https://juejin.cn/s/runtime.getruntime().exec%20get%20output%20kotlin
    fun getCommandOutput(command: String): String {
        val process = Runtime.getRuntime().exec(command)
        val inputStream = process.inputStream
        val errorStream = process.errorStream
        val inputReader = BufferedReader(InputStreamReader(inputStream))
        val errorReader = BufferedReader(InputStreamReader(errorStream))
        val output = StringBuilder()
        var line: String? = inputReader.readLine()
        while (line != null) {
            output.append(line).append("\n")
            line = inputReader.readLine()
        }
        line = errorReader.readLine()
        while (line != null) {
            output.append(line).append("\n")
            line = errorReader.readLine()
        }
        return output.toString()
    }

    fun grantOverlayPermission() {

        // it's only once, so we don't use hidden api
        // it's blocking, otherwise showOverlay can fail
        getCommandOutput("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
    }

    fun getRotation(): Int =
        if (Build.VERSION.SDK_INT < 26) {
            mWm.rotation
        } else {
            mWm.defaultDisplayRotation
        }

    fun refreshScreenshot(): Bitmap {
        val startTime = System.currentTimeMillis()
//        Log.d(
//            TAG, "callinguid? ${Binder.getCallingUid()} ${Binder.getCallingPid()} " +
//                    "${android.os.Process.SYSTEM_UID}"
//        )

        val size = getOverrideDisplaySize()
        val physical = getPhysicalDisplaySize()
//        Log.d(TAG, "$size $physical ${size==physical} ${size.equals(physical)}")
        val startTime2 = System.currentTimeMillis()
        // android >= 9.0 时，takeScreenshot尊重overrideDisplaySize
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 28 || size == physical) {
            Log.d(TAG, "screenshot using outer api")
            uiAutomation.takeScreenshot()
        } else {
            // android < 9.0时，takeScreenshot存在黑边，要控制尺寸并裁剪

//            Log.d(TAG, "before0 ${size} ${physical}")

            var width = size.x
            var height = size.y
            val scaleHeight = size.x.toFloat() / size.y > physical.x.toFloat() / physical.y
            if (scaleHeight) {
                height = (width.toFloat() / physical.x * physical.y).roundToInt()
                if (height % 2 != 0) height += 1
            } else {
                width = (height.toFloat() / physical.y * physical.x).roundToInt()
                if (width % 2 != 0) width += 1
            }


            uiAutomationConnection.takeScreenshot(width, height).let { image ->
//                Log.d(TAG, "${getRotation()} ${Surface.ROTATION_90} ")
//                val nowTime = System.currentTimeMillis()
//                Log.d(TAG, "after ${image.width} ${image.height} ${nowTime - startTime}")

                val angle = getRotation() * -90f
                val matrix = Matrix().apply {
                    this.postRotate(angle)
                }
                val dstBmp = if (scaleHeight) {
                    Bitmap.createBitmap(
                        image, 0, (height - size.y) / 2, size.x, size.y, matrix, false
                    )
                } else {
                    Bitmap.createBitmap(
                        image, (width - size.x) / 2, 0, size.x, size.y, matrix, false
                    )
                }
                dstBmp.setHasAlpha(false)

//                val dstBmp = Bitmap.createBitmap(image)

//                dstBmp.recycle()
//                image.recycle()
                dstBmp
            }
        }

        Log.d(
            TAG,
            "screenshot time ${System.currentTimeMillis() - startTime}ms ${System.currentTimeMillis() - startTime2}ms"
        )

//        val dst = File(cacheDir, "tmp.png")
//        dst.parentFile?.mkdirs()
//        FileOutputStream(dst).use { stream ->
//            bitmap.compress(Bitmap.CompressFormat.PNG, 25, stream)
//            Log.d(TAG, "save to file $dst")
//            bitmap.recycle()
//        }

        return bitmap

        // TODO save to a rust shared memory?
    }


    fun testUIAutomation() {
        var startTime = System.currentTimeMillis()
        taskThread = thread {

            runCatching {
                while (true) {
                    refreshScreenshot()
//                    click(100f,100f)
                    Thread.sleep(1000)
//                    break
                }
            }.onFailure {
                Log.d(TAG, it.stackTraceToString())
            }
        }
    }

    override fun test() {
        Binder.clearCallingIdentity()
        Log.d(TAG, "before thread ${Binder.getCallingUid()}")
        thread {
            Log.d(TAG, "after thread ${Binder.getCallingUid()}")
            testUIAutomation()
        }
    }


    override fun start() {
        Binder.clearCallingIdentity()
        localRun.showTask()

//        testUIAutomation()
        debugServer = DebugServer(this)
        debugServer.start()
    }

    override fun stop() {
        debugServer.stop()
    }

    override fun runTask(type: String) {
        val runner = loadRunner(type)
        runner.runTask(this)
    }

    override fun testLargeFileTransfer(pfd: ParcelFileDescriptor) {
        val fileDescriptor = pfd.fileDescriptor
        val fis = FileInputStream(fileDescriptor)
        val data = fis.readBytes()
        Log.d(TAG, "got data size ${data.size}")
        pfd.close()
    }

    fun getBinder(): IBinder {
        return IRemoteRun.Stub.asInterface(this).asBinder()
    }

    fun refreshScreenNode(): List<AccessibilityNodeInfo> {
        val root = uiAutomation.rootInActiveWindow
        val ans = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root ?: let { return ans })
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            ans.add(node)
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let {
                    queue.add(it)
                }
//                queue.add(node.getChild(i))
            }
        }

        return ans
    }

}