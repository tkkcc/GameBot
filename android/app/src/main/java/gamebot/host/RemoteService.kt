//import kotlin.Pair
import android.app.UiAutomation
import android.app.UiAutomationConnection
import android.app.UiAutomationHidden
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManagerHidden
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.ServiceManager
import android.os.SystemClock
import android.util.Log
import android.view.IWindowManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceControlHidden
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
import dev.rikka.tools.refine.Refine
import gamebot.host.ILocalService
import gamebot.host.IRemoteService
import gamebot.host.RemoteRun.Companion.CACHE_DIR
import gamebot.host.RemoteRun.Companion.TAG
import gamebot.host.sendLargeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt
import kotlin.system.exitProcess

class RemoteService(val context: Context) : IRemoteService.Stub() {

    //    val native = Native()
    companion object {
        init {
            System.loadLibrary("rust")
        }
    }

    lateinit var localService: ILocalService

    private lateinit var uiAutomationHidden: UiAutomationHidden
    private lateinit var uiAutomationConnection: UiAutomationConnection
    private lateinit var uiAutomation: UiAutomation
    private val uiAutomationThread = HandlerThread("GameBotUiAutomationThread")
    private val imageReaderThread = HandlerThread("GameBotImageReaderThread")
    lateinit var mWm: IWindowManager

    override fun destroy() {
        Log.e("", "destroy in remote service")
        exitProcess(0)
    }

    override fun callback(msg: String) {
        TODO("Not yet implemented")
    }

    external fun startGuest(name: String, host: RemoteService)
    override fun startGuest(name: String) {
        Binder.clearCallingIdentity()
        startGuest(name, this)
    }

    external override fun stopGuest(name: String)


    fun toast(msg: String) {
        localService.toast(msg)
    }

    fun toast2(msg: String) {
//        localService.toast(msg)
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

    //            fun takeScreenNode(): Pair<ByteArray, ArrayList<AccessibilityNodeInfo>> {
//                val pair = this@RemoteService.takeScreenNode()
//                return Pair(pair.first, ArrayList(pair.second))
//            }
    fun takeScreenNode(): ScreenNode {
        val (info, infoRef) = takeScreenNodeRaw()

        Log.e("", "screen node size: ${info.size}, ${infoRef.size}")

        val stream = ByteArrayOutputStream();
        Json.encodeToStream(info, stream)

        return ScreenNode(stream.toByteArray(), infoRef.toTypedArray())
    }

    fun click(x: Int, y: Int) {
        // TODO this will block user input?
        val t=SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(
            t,
            t,
            MotionEvent.ACTION_DOWN,
            x.toFloat(),
            y.toFloat(),
            0
        )
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        uiAutomation.injectInputEvent(event, false)
        event.action = MotionEvent.ACTION_UP
        uiAutomation.injectInputEvent(event, false)
        event.recycle()
    }

    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(ACTION_CLICK)
    }

    fun takeScreenshot(): Screenshot {
        // sync update
        if (isScreenshotInvalid()) {
            Log.e("", "sync update screenshot")
            updateScreenshot()
        } else {
//            Log.e("","async update screenshot")

        }

        // async update
        requestUpdateScreenshot.trySend(Unit)

        // cached
        return screenshot
    }


    fun connectUiAutomation() {
        Log.e("137", "connectUiAutomation")
        uiAutomationThread.start()

        uiAutomationConnection = UiAutomationConnection()
        uiAutomationHidden = UiAutomationHidden(uiAutomationThread.looper, uiAutomationConnection)

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

//    fun takeSceenNode() {
//        val x = refreshScreenNode()
//        val ans = uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText("1")
//    }

    @Serializable
    data class Rect(
        val left: UInt,
        val top: UInt,
        val right: UInt,
        val bottom: UInt,
    )

    @Serializable
    data class NodeInfo(
        val id: String = "",
        val region: Rect = Rect(0u, 0u, 0u, 0u),
        val text: String = "",
        @SerialName("class") val className: String = "",
        @SerialName("package") val packageName: String = "",
        val description: String = "",
        val checkable: Boolean = false,
        val clickable: Boolean = false,
        @SerialName("long_clickable") val longClickable: Boolean = false,
        val focusable: Boolean = false,
        val scrollable: Boolean = false,
        val visible: Boolean = false,
        val checked: Boolean = false,
        val enabled: Boolean = false,
        val focused: Boolean = false,
        val selected: Boolean = false,
        val parent: Int = 0,
        val children: MutableList<Int> = mutableListOf(),
        val index: Int = 0
    ) {
        companion object {
            fun from(node: AccessibilityNodeInfo): NodeInfo {
                val nodeInfo = NodeInfo(
                    id = node.viewIdResourceName ?: "",
                    className = (node.className ?: "").toString(),
                    packageName = (node.packageName ?: "").toString(),
                    text = (node.text ?: "").toString()
                )
                if (false && nodeInfo.text.contains("Gallery")) {
                    node.performAction(ACTION_CLICK)
                    val x = ACTION_CLICK
                }
                return nodeInfo
            }

        }

        fun index(index: Int): NodeInfo {
            return this.copy(index = index)
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    fun takeScreenNodeRaw(): Pair<List<NodeInfo>, List<AccessibilityNodeInfo>> {
        val root = uiAutomation.rootInActiveWindow
        val info = mutableListOf<NodeInfo>()
        val infoRef = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        if (root == null) {
            return Pair(info, infoRef)
        }
        queue.add(Pair(root, -1))

        while (queue.isNotEmpty()) {
            val (node, parentIdx) = queue.removeFirst()
            val nodeIdx = info.size

            if (parentIdx >= 0) {
                info[parentIdx].children.add(nodeIdx)
            }

            info.add(NodeInfo.from(node).index(nodeIdx))
            infoRef.add(node)

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let {
                    queue.add(Pair(it, nodeIdx))
                }
            }
        }


        return Pair(info, infoRef)
    }

    fun measureSceenNodeSearchSpeed() {
        while (true) {


            var start = System.currentTimeMillis()
            val window = uiAutomation.rootInActiveWindow
            Log.e("findRoot", "findRoot ${System.currentTimeMillis() - start}ms")
            start = System.currentTimeMillis()
//            val out = window.findAccessibilityNodeInfosByText("1")
            val out = takeScreenNode()
            val jsonans = Json.encodeToString(out)
            Log.e(
                "findText", "findText ${System.currentTimeMillis() - start}ms, ${out.first.size} ${
                    jsonans
                }"
            )
            start = System.currentTimeMillis()

            sleep(33)
        }

    }

//    fun click(x: Float, y: Float) {
//        //A MotionEvent is a type of InputEvent.
//        //The event time must be the current uptime.
//        val eventTime = SystemClock.uptimeMillis()
//
//        //A typical click event triggered by a user click on the touchscreen creates two MotionEvents,
//        //first one with the action KeyEvent.ACTION_DOWN and the 2nd with the action KeyEvent.ACTION_UP
//        val motionDown = MotionEvent.obtain(
//            eventTime, eventTime, KeyEvent.ACTION_DOWN, x, y, 0
//        )
//        //We must set the source of the MotionEvent or the click doesn't work.
//        motionDown.source = InputDevice.SOURCE_TOUCHSCREEN
//        uiAutomation.injectInputEvent(motionDown, true)
//        val motionUp = MotionEvent.obtain(
//            eventTime, eventTime, KeyEvent.ACTION_UP, x, y, 0
//        )
//        motionUp.source = InputDevice.SOURCE_TOUCHSCREEN
//        uiAutomation.injectInputEvent(motionUp, true)
//
//        // TODO do i really need this?
//        //Recycle our events back to the system pool.
//        motionUp.recycle()
//        motionDown.recycle()
//    }

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

    var byteBuffer = ByteBuffer.allocateDirect(0)

    fun refreshScreenshot() {

        val startTime = System.currentTimeMillis()
        //        Log.d(
        //            TAG, "callinguid? ${Binder.getCallingUid()} ${Binder.getCallingPid()} " +
        //                    "${android.os.Process.SYSTEM_UID}"
        //        )

        val size = getOverrideDisplaySize()
        val physical = getPhysicalDisplaySize()
        //        Log.d(TAG, "$size $physical ${size==physical} ${size.equals(physical)}")
        val startTime2 = System.currentTimeMillis()

//        Log.e("", "size, ${size}, physical ${physical}")
        // android >= 9.0 时，takeScreenshot尊重overrideDisplaySize
        var bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 28 || size == physical) {
//            Log.d(TAG, "screenshot using outer api")
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


        if (Build.VERSION.SDK_INT >= 26 && bitmap.config == Bitmap.Config.HARDWARE) {
            Log.e("", "hardware bitmap")
            val swBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()
            swBitmap.setHasAlpha(false)
            bitmap = swBitmap
        }

        if (byteBuffer.capacity() < bitmap.byteCount) {
            byteBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        }
        byteBuffer.position(0)
        bitmap.copyPixelsToBuffer(byteBuffer)

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

        bitmap.recycle()
    }

    var screenshot = Screenshot(
        width = 0,
        height = 0,
        data = ByteBuffer.allocateDirect(0),
        pixelStride = 0,
        rowStride = 0,
        rotation = 0,
//        timestamp = 0
    )

    val requestUpdateScreenshot = Channel<Unit>(Channel.CONFLATED)


    val screenshotTimestamp = AtomicLong(0)
    fun updateScreenshotTimestamp() {
        screenshotTimestamp.set(System.currentTimeMillis())
    }

    fun isScreenshotInvalid(): Boolean {
        return (System.currentTimeMillis() - screenshotTimestamp.get()) > 100 // 15fps => 66.6ms
    }

    fun saveScreenshot(img: Image) {
        val buf = img.planes[0].buffer
        val plane = img.planes[0]
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding: Int = rowStride - pixelStride * img.width


        Log.e("", "bitmap size ${img.width + rowPadding / pixelStride} x ${img.height}")


        val byteArray = ByteArray(100000)

        val bitmap = Bitmap.createBitmap(
            img.width + rowPadding / pixelStride,
            img.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(plane.buffer)
        val dst = File(cacheDir, "tmp.png")
        dst.parentFile?.mkdirs()
        FileOutputStream(dst).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 25, stream)
            Log.d(TAG, "save to file $dst")
            bitmap.recycle()
        }
    }

    @Synchronized
    fun updateScreenshot() {
        // do nothing if no one need new screenshot
        val img = imageReader.acquireLatestImage()
        if (img == null) {
//            Log.e("", "no image")

            updateScreenshotTimestamp()
            return
        }

//        Log.e("", "new image got")
        val buf = img.planes[0].buffer
        if (screenshot.width != imageReader.width || screenshot.height != imageReader.height) {
            val data = ByteBuffer.allocateDirect(buf.capacity())
            data.put(buf)
            screenshot = Screenshot(
                width = imageReader.width,
                height = imageReader.height,
                data = data,
                pixelStride = img.planes[0].pixelStride,
                rowStride = img.planes[0].rowStride,
                rotation = 0,
            )
        } else {
            // there can be a time we are writing screenshot and user use the updating one
            screenshot.data.position(0)
            screenshot.data.put(buf)
        }
        updateScreenshotTimestamp()
        img.close()
    }

    lateinit var imageReader: ImageReader
    lateinit var display: IBinder
    lateinit var virtualDisplay: VirtualDisplay
    fun initDisplayProjection() {

        val screenSize = getPhysicalDisplaySize()
        imageReaderThread.start()
        imageReader =
            ImageReader.newInstance(screenSize.x, screenSize.y, PixelFormat.RGBA_8888, 2)
        imageReader.setOnImageAvailableListener({
//            Log.e("","new screenshot available")

            runBlocking(Dispatchers.Default) {
                requestUpdateScreenshot.receive()
            }
//            Log.e("","update screenshot from callback")
            updateScreenshot()
        }, Handler(imageReaderThread.looper))

        // is android Image plane align row stride to times of 4?
        byteBuffer = ByteBuffer.allocateDirect(screenSize.x * screenSize.y * 4)

//        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        context.startService(Intent(context,MediaProjection::class.java))

        runCatching {
            virtualDisplay = DisplayManagerHidden.createVirtualDisplay(
                "GameBotDisplay", screenSize.x, screenSize.y, 0, imageReader.surface
            )

        }.onFailure {
            Log.e("", "496", it)
//            throw NotImplementedError()
            val secure =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && "S" != Build.VERSION.CODENAME)
            display = SurfaceControlHidden.createDisplay("GameBotDisplay", secure)
//            display = SurfaceControlHidden.getBuiltInDisplay(0)
            SurfaceControlHidden.openTransaction()
            try {
                SurfaceControlHidden.setDisplaySurface(display, imageReader.surface)
                SurfaceControlHidden.setDisplayProjection(
                    display,
                    0,
                    Rect(0, 0, screenSize.x, screenSize.y),
                    Rect(0, 0, screenSize.x, screenSize.y)
                )
                SurfaceControlHidden.setDisplayLayerStack(display, 0)
            } catch (e: Exception) {
                Log.e("", "497", e)
            } finally {
                SurfaceControlHidden.closeTransactionSync()
            }
        }
    }

    fun initAll() {
        // we can connect to app's database in shell uid?
        // no, it's hang

        // for android >=13, after clear uid, it's just shell / root uid
        val token = clearCallingIdentity()


        mWm = IWindowManager.Stub.asInterface(
            ServiceManager.checkService(
                Context.WINDOW_SERVICE
            )
        )


        connectUiAutomation()

        // TODO some device don't have grant binary, can we use UiAutomation's api?
//        grantOverlayPermission()
//        Binder.restoreCallingIdentity(token)

//        click(0,0)
        Log.e("137", "after init , uid = ${getCallingUid()}")
    }

    override fun setLocalRunBinder(binder: IBinder?) {
        localService = ILocalService.Stub.asInterface(binder)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun start() {
        Looper.prepare()

        Log.e("", "start in remote service, uid = ${getCallingUid()}")

        initAll()
        initDisplayProjection()
        localService.test()
        this.javaClass.declaredMethods.forEach {
            Log.e("", "790 ${it}")
        }

        click(200,200)
        Log.e("", "rust call finish")
    }
}
