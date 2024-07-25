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
import android.os.ParcelFileDescriptor
import android.os.ServiceManager
import android.os.SystemClock
import android.util.Log
import android.view.IWindowManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.rikka.tools.refine.Refine
import gamebot.host.RemoteRun.Companion.CACHE_DIR
import gamebot.host.RemoteRun.Companion.TAG
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.system.exitProcess

class RemoteService(val context: Context) : IRemoteService.Stub() {

    val native = Native()
    lateinit var localService: ILocalService

    private lateinit var uiAutomationHidden: UiAutomationHidden
    private lateinit var uiAutomationConnection: UiAutomationConnection
    private lateinit var uiAutomation: UiAutomation
    private val handlerThread = HandlerThread("GameBotHandlerThread")
    lateinit var mWm: IWindowManager

    override fun destroy() {
        Log.e("", "destroy in remote service")
        exitProcess(0)
    }

    override fun callback(msg: String) {
        TODO("Not yet implemented")
    }

    fun connectUiAutomation() {
        Log.e("137", "connectUiAutomation")
        handlerThread.start()

        uiAutomationConnection = UiAutomationConnection()
        uiAutomationHidden =
            UiAutomationHidden(handlerThread.looper, uiAutomationConnection)

        Log.d(TAG, "UiAutomation connect")

        uiAutomationHidden.connect(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)

        Log.d(TAG, "UiAutomation connect 172")

        uiAutomation = Refine.unsafeCast(uiAutomationHidden)
    }

    var cacheDir: String = CACHE_DIR

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

    fun click(x: Float, y: Float) {
        //A MotionEvent is a type of InputEvent.
        //The event time must be the current uptime.
        val eventTime = SystemClock.uptimeMillis()

        //A typical click event triggered by a user click on the touchscreen creates two MotionEvents,
        //first one with the action KeyEvent.ACTION_DOWN and the 2nd with the action KeyEvent.ACTION_UP
        val motionDown = MotionEvent.obtain(
            eventTime, eventTime, KeyEvent.ACTION_DOWN, x, y, 0
        )
        //We must set the source of the MotionEvent or the click doesn't work.
        motionDown.source = InputDevice.SOURCE_TOUCHSCREEN
        uiAutomation.injectInputEvent(motionDown, true)
        val motionUp = MotionEvent.obtain(
            eventTime, eventTime, KeyEvent.ACTION_UP, x, y, 0
        )
        motionUp.source = InputDevice.SOURCE_TOUCHSCREEN
        uiAutomation.injectInputEvent(motionUp, true)

        // TODO do i really need this?
        //Recycle our events back to the system pool.
        motionUp.recycle()
        motionDown.recycle()
    }

    fun touchDown() {

    }

    fun touchUp() {

    }

    fun getPhysicalDisplaySize(): Point = Point().apply {
        mWm.getInitialDisplaySize(0, this)
    }

    fun getOverrideDisplaySize(): Point = Point().apply {
        mWm.getBaseDisplaySize(0, this)
    }

    fun getPhysicalDisplayDensity(): Int = mWm.getInitialDisplayDensity(0)


    fun getOverrideDisplayDensity(): Int = mWm.getBaseDisplayDensity(0)

    fun setOverrideDisplaySize(point: Point) {
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

    fun getRotation(): Int = if (Build.VERSION.SDK_INT < 26) {
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

        val dst = File(cacheDir, "tmp.png")
        dst.parentFile?.mkdirs()
        FileOutputStream(dst).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 25, stream)
            Log.d(TAG, "save to file $dst")
            bitmap.recycle()
        }

        return bitmap

        // TODO save to a rust shared memory?
    }


    fun testUIAutomation() {
        var startTime = System.currentTimeMillis()
        thread {

            runCatching {
                while (true) {
                    refreshScreenshot()
//                            click(100f, 100f)
                    Thread.sleep(1000)
//                    break
                }
            }.onFailure {
                Log.e(TAG, "", it)
            }
        }
    }

    fun init() {
        // we can connect to app's database in shell uid?
        // no, it's hang

        // for android >=13, after clear uid, it's just shell / root uid
        Binder.clearCallingIdentity()

        mWm = IWindowManager.Stub.asInterface(
            ServiceManager.checkService(
                Context.WINDOW_SERVICE
            )
        )

        connectUiAutomation()
        grantOverlayPermission()

    }

    override fun setLocalRunBinder(binder: IBinder?) {
        localService = ILocalService.Stub.asInterface(binder)
    }

    override fun start() {
        Log.e("137", "start in remote service")
//                localService.toast("ok")
        init()
        testUIAutomation()
        localService.test()

        val repo = File(cacheDir, "repo")
        val toNative = object {
            val cache_dir = cacheDir
            fun toast(msg: String) {
                localService.toast(msg)
            }

            fun showUI(layout: String, state: String) {

            }

            fun updateConfigUI(layout: ByteArray) {
                sendLargeData(layout).use { pfd ->
                    localService.updateConfigUI(pfd)
                }
            }

            fun waitConfigUIEvent(): ByteArray {
                return localService.waitConfigUIEvent().use { pfd ->
                    ParcelFileDescriptor.AutoCloseInputStream(pfd).readBytes()
                }
            }
        }
        toNative.javaClass.declaredMethods.forEach {
            Log.e("", it.toString())
        }
        toNative.javaClass.declaredFields.forEach {
            Log.e("", it.toString())
        }
        thread {
            native.start(toNative)
        }
        Log.e("", "rust call finish")
    }
}
