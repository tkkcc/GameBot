package gamebot.host

import Nodeshot
import RemoteService
import RemoteService.TouchAction
import Screenshot
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE_PRE_26
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING_PRE_28
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.os.Binder
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Host(val remoteService: RemoteService, val localService: ILocalService, val token: Int) {
    //    init {
//        val x = runningAppProcess()
//        Log.e("gamebot", "recent task list: ${x.size}")
//    }
    val scope = CoroutineScope(Dispatchers.Default)

    init {

        Log.e("gamebot", "host scope ${scope.isActive}")
    }

    fun toast(msg: String) {
        localService.toast(msg)
    }

    fun updateConfigUI(layout: ByteArray) {
        sendLargeData(layout).use { pfd ->
            localService.updateConfigUI(token, pfd)
        }
    }

    fun waitConfigUIEvent(): ByteArray {
        return localService.waitConfigUIEvent(token).use { pfd ->
            ParcelFileDescriptor.AutoCloseInputStream(pfd).readBytes()
        }
    }


    fun waitAccessibilityEvent() {
        remoteService.uiAutomation.setOnAccessibilityEventListener { }
    }

    fun stopConfigUI() {
        localService.stopConfigUI(token)
    }

    fun sendEmptyConfigUIEvent() {
        localService.sendEmptyConfigUIEvent(token)
    }

    fun takeNodeshot(): Nodeshot = remoteService.takeNodeshot()
    fun takeScreenshot(): Screenshot = remoteService.takeScreenshot()
    fun waitScreenshotAfter(timestamp: Long, timeout: Long) =
        remoteService.waitScreenshotAfter(timestamp, timeout,scope)

    fun waitNodeshotAfter(timestamp: Long, timeout: Long) =
        remoteService.waitNodeshotAfter(timestamp, timeout,scope)

    fun emitCancelToken() {
        Log.e("gamebot", "emitCancelToken")
        scope.cancel()
    }

    fun click(x: Float, y: Float) {
        touchDown(x, y, 0)
        touchUp(x, y, 0)
    }

    fun touchDown(x: Float, y: Float, id: Int) {
        remoteService.injectTouchEvent(x, y, id, TouchAction.DOWN)
    }

    fun touchMove(x: Float, y: Float, id: Int) {
        remoteService.injectTouchEvent(x, y, id, TouchAction.MOVE)
    }

    fun touchUp(x: Float, y: Float, id: Int) {
        remoteService.injectTouchEvent(x, y, id, TouchAction.UP)
    }

    fun clickRecent() {
        Binder.clearCallingIdentity()
        keyDown(KeyEvent.KEYCODE_APP_SWITCH)
        keyUp(KeyEvent.KEYCODE_APP_SWITCH)
    }

    fun clickHome() {
        Binder.clearCallingIdentity()
        keyDown(KeyEvent.KEYCODE_HOME)
        keyUp(KeyEvent.KEYCODE_HOME)
    }

    fun keyDown(keyCode: Int) {
        Binder.clearCallingIdentity()
        val event = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        remoteService.inputManager.injectInputEvent(event, 0)
    }

    fun keyUp(keyCode: Int) {
        Binder.clearCallingIdentity()
        val event = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        remoteService.inputManager.injectInputEvent(event, 0)
    }


    fun currentActivity(): String {
        val data = remoteService.activityManager.getRecentTasks(1, 0)
            .firstOrNull()?.topActivity?.run {
                ActivityInfo(
                    packageName,
                    className
                )
            } ?: ActivityInfo()
        return Json.encodeToString(data)
    }

    fun runningActivityList(): String {
        val data = remoteService.activityManager.getRunningTasks(Int.MAX_VALUE).map {
            it.topActivity?.run {
                ActivityInfo(
                    packageName,
                    className
                )
            } ?: ActivityInfo()
        }.toList()
        return Json.encodeToString(data)
    }

    fun runningAppProcessList(): String {
        val data = remoteService.activityManager.runningAppProcesses.map {
            AppProcessInfo(
                processName = it.processName,
                importance = when (it.importance) {
                    IMPORTANCE_FOREGROUND -> Importance.Foreground
                    IMPORTANCE_FOREGROUND_SERVICE -> Importance.ForegroundService
                    IMPORTANCE_SERVICE -> Importance.Service
                    IMPORTANCE_GONE -> Importance.Gone
                    IMPORTANCE_TOP_SLEEPING -> Importance.TopSleeping
                    IMPORTANCE_TOP_SLEEPING_PRE_28 -> Importance.TopSleeping
                    IMPORTANCE_VISIBLE -> Importance.Visible
                    IMPORTANCE_PERCEPTIBLE -> Importance.Perceptible
                    IMPORTANCE_PERCEPTIBLE_PRE_26 -> Importance.Perceptible
                    IMPORTANCE_BACKGROUND -> Importance.Cached
                    IMPORTANCE_CACHED -> Importance.Cached
                    IMPORTANCE_CANT_SAVE_STATE -> Importance.CantSaveState
                    IMPORTANCE_EMPTY -> Importance.Cached
                    else -> Importance.Unknown
                }
            )
        }.toList()
        return Json.encodeToString(data)
    }

    fun launchActivity(packageName: String): String {
        return remoteService.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.component?.className
        } ?: ""
    }

    fun startPackage(packageName: String) {

        remoteService.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.component?.let {
                startActivity(packageName, it.className)
            }
        }

    }

    fun startActivity(packageName: String, className: String) {
//        Binder.clearCallingIdentity()

        Log.e("gamebot", "startActivity in Host $packageName $className")
        Runtime.getRuntime().exec("am start -n ")
//        remoteService.activityManager

//        localService.startActivity(packageName, className)

//        val intent = Intent().apply {
//            setClassName(packageName, className)
//        }
//        remoteService.context.startActivity(intent)
    }

    fun installedPackageList(): String {
        Binder.clearCallingIdentity()
        val data = remoteService.packageManager.getInstalledPackages(0).map {
//            Log.e("gamebot", "package: ${it.activities?.joinToString {
//
//                it.targetActivity?.toString() ?: ""
//            }?:""}")
            PackageInfo(
                packageName = it.packageName,
                versionName = it.versionName,
//                activityList = it.activities?.map {
//                    it.name
//                }?.toList() ?: emptyList()
            )

        }.toList()
        return Json.encodeToString(data)
    }


    fun activityList(packageName: String): String {
        Binder.clearCallingIdentity()
        val data = remoteService.packageManager.getPackageInfo(
            packageName,
            GET_ACTIVITIES
        ).activities?.map {
            it.name
        }?.toList() ?: emptyList<String>()
        return Json.encodeToString(data)
    }
}