//import kotlin.Pair
//import kotlinx.coroutines.time.sample
import android.app.ActivityManager
import android.app.UiAutomation
import android.app.UiAutomationConnection
import android.app.UiAutomationHidden
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManagerHidden
import android.hardware.display.VirtualDisplay
import android.hardware.input.IInputManager
import android.media.Image
import android.media.ImageReader
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ServiceManager
import android.os.SystemClock
import android.view.IWindowManager
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.SurfaceControlHidden
import android.view.accessibility.AccessibilityNodeInfo
import dev.rikka.tools.refine.Refine
import gamebot.host.BuildConfig
import gamebot.host.Host
import gamebot.host.ILocalService
import gamebot.host.IRemoteService
import gamebot.host.d
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.system.exitProcess

interface ProgressListener {
    fun onUpdate(percent: Float, bytePerSecond: Float)
}

@Serializable
data class ClonedGuest(
    val url: String,
    val branch: String,
    val name: String,
)

@Serializable
data class RemoteServiceState(
    val clonedGuest: List<ClonedGuest> = emptyList()
)

class RemoteService(val context: Context) : IRemoteService.Stub() {
    companion object {
        val remoteCache = "/data/local/tmp/gamebot"

        init {
            File(remoteCache).mkdirs()
        }
    }

    init {
//        loadState()
    }

    override fun autoStartExist(): Boolean {
        return File(remoteCache + "/guest/autostart").exists()
    }

    external fun gitClone(
        url: String,
        branch: String,
        path: String,
        progressListener: ProgressListener? = null
    ): String

    external fun gitPull(
        url: String,
        branch: String,
        path: String,
        progressListener: ProgressListener? = null
    ): String


    external fun initHostLogger()

    lateinit var localService: ILocalService
    lateinit var activityManager: ActivityManager
    lateinit var packageManager: PackageManager
    lateinit var windowManager: IWindowManager
    lateinit var inputManager: IInputManager

    lateinit var uiAutomationHidden: UiAutomationHidden
    lateinit var uiAutomationConnection: UiAutomationConnection
    lateinit var uiAutomation: UiAutomation

    private val uiAutomationThread = HandlerThread("GameBotUiAutomationThread")
    private val imageReaderThread = HandlerThread("GameBotImageReaderThread")

    private val hostMap = mutableMapOf<String, Host>()

    override fun destroy() {
        d("destroy in remote service")
        exitProcess(0)
    }

    override fun callback(msg: String) {
        TODO("Not yet implemented")
    }

    @Synchronized
    fun hostOf(name: String): Host {
        // we won't remove key from hostMap
        // because host is stored on Guest OnceLock
        return hostMap.getOrPut(name, {
//                 d( "create host")
//            val used = hostMap.values.map { it.token }.toSet()
//            var i = used.size
//            for (j in used.indices) {
//                if (!used.contains(j)) {
//                    i = j
//                    break
//                }
//            }
            Host(this, localService, name)
        })
    }

    external fun startGuest(name: String, host: Host): String

    override fun startGuest(name: String): String {
        Binder.clearCallingIdentity()
        initHost()
        return startGuest(name, hostOf(name))
    }

    external fun stopGuest(name: String, host: Host)

    override fun stopGuest(name: String) {
        Binder.clearCallingIdentity()
        initHost()
        stopGuest(name, hostOf(name))
    }

    var hostInited = false

    @Synchronized
    fun initHost() {
        if (hostInited) {
            return
        }
        if (BuildConfig.DEBUG) {
            System.loadLibrary("host")
        } else {
            System.load(remoteCache + "/libhost.so")
        }
        initHostLogger()
        hostInited = true
    }


    val screenshotStateFlow = MutableStateFlow(0L)
    val nodeshotStateFlow = MutableStateFlow(0L)
    var nodeshotCache: Nodeshot = Nodeshot(ByteBuffer.allocateDirect(0), emptyArray(), 0)

    @Synchronized
    @OptIn(ExperimentalSerializationApi::class)
    fun updateNodeshot() {
        val (info, infoRef) = takeNodeshotRaw()

        val stream = ByteArrayOutputStream()
        Json.encodeToStream(info, stream)
        val buf = ByteBuffer.allocateDirect(stream.size())
        buf.put(stream.toByteArray())

        nodeshotCache =
            Nodeshot(buf, infoRef.toTypedArray(), timestamp = SystemClock.uptimeMillis())
        nodeshotStateFlow.update {
            max(it, nodeshotCache.timestamp)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun takeNodeshot(): Nodeshot {
        val now = nodeshotCache.timestamp
        if (SystemClock.uptimeMillis() - now > 100 && requestUpdateNodeshot.isEmpty) {
            updateNodeshot()
        }

        requestUpdateNodeshot.trySend(Unit)

        return nodeshotCache
    }

    fun waitNodeshotAfter(timestamp: Long, timeout: Long, scope: CoroutineScope) {
        requestUpdateNodeshot.trySend(Unit)
//            d("waitNodeshotAfter scope $timestamp $timeout ${scope.isActive}")
        runBlocking {
            scope.launch {
//                      d( "waitNodeshotAfter $timestamp $timeout")
                withTimeoutOrNull(timeout) {
                    nodeshotStateFlow.onEach {
//                               d( "nodeshot state flow $it")
                    }.first { it > timestamp }
                }
            }.join()
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun waitScreenshotAfter(timestamp: Long, timeout: Long, scope: CoroutineScope) {
        requestUpdateScreenshot.trySend(Unit)
        runBlocking {
            scope.launch {
                withTimeoutOrNull(timeout) {
                    screenshotStateFlow.first { it > timestamp }
                }
            }.join()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun takeScreenshot(): Screenshot {
        val timestamp = screenshotCache.timestamp
        if (SystemClock.uptimeMillis() - timestamp > 100 && requestUpdateScreenshot.isEmpty) {
            updateScreenshot()
        }

        requestUpdateScreenshot.trySend(Unit)

        return screenshotCache
    }


    var lastTouchDownTime = SystemClock.uptimeMillis()
    var pointerState = mutableMapOf<Int, PointerCoords>()

    enum class TouchAction(val single: Int, val multi: Int) {
        DOWN(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN),
        UP(MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP),
        MOVE(MotionEvent.ACTION_MOVE, MotionEvent.ACTION_MOVE);
    }

    @Synchronized
    fun injectTouchEvent(
        x: Float,
        y: Float,
        id: Int,
        touchAction: TouchAction,
        eventTime: Long = 0
    ) {
        Binder.clearCallingIdentity()
        val now = if (eventTime == 0L) SystemClock.uptimeMillis() else eventTime

        pointerState.getOrPut(id, { PointerCoords() }).apply {
            this.x = x
            this.y = y
            this.size = 1.0f
            this.pressure = 1.0f
        }

        val pointerPropertiesList = pointerState.keys.map {
            PointerProperties().apply {
                this.id = it
                this.toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }.toTypedArray()

        val pointerCoordsList = pointerState.values.toTypedArray()
        if (pointerState.size == 1 && touchAction == TouchAction.DOWN) {
            lastTouchDownTime = now
        }
        val action = if (pointerState.size == 1) {
            touchAction.single
        } else {
            touchAction.multi or (id shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        }

        val event = MotionEvent.obtain(
            lastTouchDownTime,
            now,
            action,
            pointerPropertiesList.size,
            pointerPropertiesList,
            pointerCoordsList,
            0,
            0,
            1.0f,
            1.0f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )
        inputManager.injectInputEvent(event, 0)
        event.recycle()
    }

    fun connectUiAutomation() {
        d("connectUiAutomation")
        uiAutomationThread.start()

        uiAutomationConnection = UiAutomationConnection()
        uiAutomationHidden = UiAutomationHidden(uiAutomationThread.looper, uiAutomationConnection)

        d("UiAutomation connect")

        uiAutomationHidden.connect(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)

        d("UiAutomation connect 172")

        uiAutomation = Refine.unsafeCast(uiAutomationHidden)
    }

//    lateinit var cacheDir: String

//    fun fetchScreenNode(): List<AccessibilityNodeInfo> {
//        val root = uiAutomation.rootInActiveWindow
//        val ans = mutableListOf<AccessibilityNodeInfo>()
//        val queue = ArrayDeque<AccessibilityNodeInfo>()
//        queue.add(root ?: let { return ans })
//        while (queue.isNotEmpty()) {
//            val node = queue.removeFirst()
//            ans.add(node)
//            for (i in 0 until node.childCount) {
//                node.getChild(i)?.let {
//                    queue.add(it)
//                }
////                queue.add(node.getChild(i))
//            }
//        }
//
//        return ans
//    }


    @OptIn(ExperimentalSerializationApi::class)
    fun takeNodeshotRaw(): Pair<List<NodeInfo>, List<AccessibilityNodeInfo>> {
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


    fun getPhysicalDisplaySize(): Point = Point().apply {
        windowManager.getInitialDisplaySize(0, this)
    }

    fun getOverrideDisplaySize(): Point = Point().apply {
        windowManager.getBaseDisplaySize(0, this)
    }

    fun getPhysicalDisplayDensity(): Int = windowManager.getInitialDisplayDensity(0)


    fun getOverrideDisplayDensity(): Int = windowManager.getBaseDisplayDensity(0)

    fun setOverrideDisplaySize(point: Point) {
        windowManager.setForcedDisplaySize(0, point.x, point.y)
    }

    fun setOverrideDensitySize(density: Int) {
        // android >= 7.1

        if (Build.VERSION.SDK_INT >= 25) {
            // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/nougat-mr1-dev/services/core/java/com/android/server/wm/WindowManagerService.java
            windowManager.setForcedDisplayDensityForUser(0, density, 0)
            d("setForcedDisplayDensityForUser success")
        } else {
            // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/nougat-dev/services/core/java/com/android/server/wm/WindowManagerService.java

            windowManager.setForcedDisplayDensity(0, density)
            d("setForcedDisplayDensity success")
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
        windowManager.rotation
    } else {
        windowManager.defaultDisplayRotation
    }

    var uiAutomationScreenshotBuffer = ByteBuffer.allocateDirect(0)

    @Deprecated("slower than display projection way")
    fun updateScreenshotByUiAutomation() {

        val startTime = SystemClock.uptimeMillis()
        //              d(
        //           "callinguid? ${Binder.getCallingUid()} ${Binder.getCallingPid()} " +
        //                    "${android.os.Process.SYSTEM_UID}"
        //        )

        val size = getOverrideDisplaySize()
        val physical = getPhysicalDisplaySize()
        //              d( "$size $physical ${size==physical} ${size.equals(physical)}")
        val startTime2 = SystemClock.uptimeMillis()

//              d( "size, ${size}, physical ${physical}")
        // android >= 9.0 时，takeScreenshot尊重overrideDisplaySize
        var bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 28 || size == physical) {
//                   d( "screenshot using outer api")
            uiAutomation.takeScreenshot()
        } else {
            // android < 9.0时，takeScreenshot存在黑边，要控制尺寸并裁剪

            //                  d( "before0 ${size} ${physical}")

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
                //                      d( "${getRotation()} ${Surface.ROTATION_90} ")
                //                val nowTime = System.currentTimeMillis()
                //                      d("after ${image.width} ${image.height} ${nowTime - startTime}")

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
            d("hardware bitmap")
            val swBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()
            swBitmap.setHasAlpha(false)
            bitmap = swBitmap
        }

        if (uiAutomationScreenshotBuffer.capacity() < bitmap.byteCount) {
            uiAutomationScreenshotBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        }
        uiAutomationScreenshotBuffer.position(0)
        bitmap.copyPixelsToBuffer(uiAutomationScreenshotBuffer)

        d(
            "screenshot time ${SystemClock.uptimeMillis() - startTime}ms ${SystemClock.uptimeMillis() - startTime2}ms"
        )

        val dst = File(remoteCache, "tmp.png")
        dst.parentFile?.mkdirs()
        FileOutputStream(dst).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 25, stream)
            d("save to file $dst")
            bitmap.recycle()
        }

        bitmap.recycle()
    }

    var screenshotCache = Screenshot(
        width = 0,
        height = 0,
        data = ByteBuffer.allocateDirect(0),
        timestamp = 0
    )

    val requestUpdateScreenshot = Channel<Unit>(Channel.CONFLATED)
    val requestUpdateNodeshot = Channel<Unit>(Channel.CONFLATED)

    fun saveScreenshot(img: Image) {
        val buf = img.planes[0].buffer
        val plane = img.planes[0]
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding: Int = rowStride - pixelStride * img.width


        d("bitmap size ${img.width + rowPadding / pixelStride} x ${img.height}")


        val byteArray = ByteArray(100000)

        val bitmap = Bitmap.createBitmap(
            img.width + rowPadding / pixelStride,
            img.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(plane.buffer)
        val dst = File(remoteCache, "tmp.png")
        dst.parentFile?.mkdirs()
        FileOutputStream(dst).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 25, stream)
            d("save to file $dst")
            bitmap.recycle()
        }
    }

    @Synchronized
    fun updateScreenshot() {
        val img = imageReader.acquireLatestImage() ?: return

        val buf = img.planes[0].buffer
        screenshotCache.data.position(0)
        screenshotCache.data.put(buf)
        img.close()

        // update info
        if (screenshotCache.width != imageReader.width || screenshotCache.height != imageReader.height) {
            screenshotCache = screenshotCache.copy(
                width = imageReader.width,
                height = imageReader.height,
            )
        }

        screenshotCache.timestamp = SystemClock.uptimeMillis()
        screenshotStateFlow.update {
            max(it, screenshotCache.timestamp)
        }
    }

    lateinit var imageReader: ImageReader
    lateinit var display: IBinder
    lateinit var virtualDisplay: VirtualDisplay

    fun initShot() {
        uiAutomation.setOnAccessibilityEventListener {
//                 d( "741, new accessibility event")
            runBlocking(Dispatchers.Default) {
                requestUpdateNodeshot.receive()
            }
            updateNodeshot()
        }


        val screenSize = getPhysicalDisplaySize()

        screenshotCache =
            screenshotCache.copy(data = ByteBuffer.allocateDirect(screenSize.x * screenSize.y * 4))


        imageReaderThread.start()
        imageReader =
            ImageReader.newInstance(screenSize.x, screenSize.y, PixelFormat.RGBA_8888, 2)
        imageReader.setOnImageAvailableListener({
//                   d( "new screenshot available")

            runBlocking(Dispatchers.Default) {
                requestUpdateScreenshot.receive()
            }
//                   d("update screenshot from callback")
            updateScreenshot()
        }, Handler(imageReaderThread.looper))

        // is android Image plane align row stride to times of 4?
//        uiAutomationScreenshotBuffer = ByteBuffer.allocateDirect(screenSize.x * screenSize.y * 4)

//        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        context.startService(Intent(context,MediaProjection::class.java))

        runCatching {
            virtualDisplay = DisplayManagerHidden.createVirtualDisplay(
                "GameBotDisplay", screenSize.x, screenSize.y, 0, imageReader.surface
            )
//            virtualDisplay.resize()
        }.onFailure {
            d("496", it)
//            throw NotImplementedError()
            val secure =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && "S" != Build.VERSION.CODENAME)
//            if (!secure) {
//                throw NotImplementedError()
//            }
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
                d("497", e)
            } finally {
                SurfaceControlHidden.closeTransaction()
            }
        }
    }

    fun initManager() {

//        initLogger()

        // we can connect to app's database in shell uid?
        // no, it's hang

        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        packageManager = context.packageManager




        windowManager = IWindowManager.Stub.asInterface(
            ServiceManager.getService(
                Context.WINDOW_SERVICE
            )
        )

        inputManager = IInputManager.Stub.asInterface(
            ServiceManager.getService(
                Context.INPUT_SERVICE
            )
        )



        connectUiAutomation()


        // TODO some device don't have grant binary, can we use UiAutomation's api?
//        grantOverlayPermission()
//        Binder.restoreCallingIdentity(token)

//        click(0,0)
        d("after init , uid = ${getCallingUid()}")
    }


    override fun setLocalServiceBinder(binder: IBinder?) {
        localService = ILocalService.Stub.asInterface(binder)
    }

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val downloadJob = mutableMapOf<String, Job>()


    val stateFile = File(remoteCache + "/state.json")
    var state = RemoteServiceState()
    fun loadState() {
        d("remote state ${state.clonedGuest}")

        try {
            state = Json.decodeFromStream(stateFile.inputStream())
            d("remote state $state")
        } catch (e: Throwable) {
            d("failed to load remote state $stateFile", e)
        }
    }

    fun saveState() {
        try {
            Json.encodeToStream(state, stateFile.outputStream())
        } catch (e: Throwable) {
            d("failed to save $stateFile", e)
        }
    }

    @Synchronized
    fun gitCloneOrPull(
        url: String,
        branch: String,
        path: String,
        progressListener: ProgressListener?
    ): String {
        val name = File(path).name
        val guest = ClonedGuest(url, branch, name)
        val msg = if (state.clonedGuest.any { it == guest }) {
            d("use git pull $url")
            gitPull(url, branch, path, progressListener)
        } else {

            val msg = gitClone(url, branch, path, progressListener)
            d("use git clone $url, $msg")

            if (msg.isEmpty()) {
                state = state.copy(
                    clonedGuest = state.clonedGuest.toMutableList().apply {
                        removeIf {
                            it.name == guest.name
                        }
                        add(guest)
                    }
                )
                saveState()
            }
            msg
        }

        return msg
    }

    override fun startDownload(
        url: String,
        path: String,
        sha256sum: String,
        isRepo: Boolean
    ): String {
        val job = if (isRepo) {
            val branch = Build.SUPPORTED_ABIS[0]
            scope.async {
                initHost()
                val msg = gitCloneOrPull(
                    url,
                    branch,
                    path,
                    object : ProgressListener {
                        override fun onUpdate(percent: Float, bytePerSecond: Float) {
                            localService.updateDownload(path, percent, bytePerSecond)
//                            d(percent, bytePerSecond / 1000 / 1000)
                        }
                    }
                )
                if (msg.isNotEmpty()) {
                    throw Exception(msg)
                }
            }
        } else {
            downloadFile(url, path, sha256sum, scope, object : ProgressListener {
                override fun onUpdate(percent: Float, bytePerSecond: Float) {
                    localService.updateDownload(path, percent, bytePerSecond)
//                d(percent, bytePerSecond / 1000 / 1000)
                }
            })
        }

        downloadJob[path] = job

        return runBlocking {
            try {
                job.await()
                ""
            } catch (e: CancellationException) {
                "canceled"
            } catch (e: Throwable) {
                d("startDownload fail", e)
                e.localizedMessage
            }
        }
    }

    override fun stopDownload(path: String) {
        runBlocking {
            try {
                downloadJob[path]?.cancelAndJoin()
            } catch (e: Throwable) {
                d("stopDownload exception", e)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun start() {
        // for android >=13, after clear uid, it's just shell / root uid
        clearCallingIdentity()
        loadState()
        initManager()
        initShot()
    }
}


