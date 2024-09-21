package gamebot.host

import Nodeshot
import RemoteService
import RemoteService.TouchAction
import Screenshot
import android.app.ActivityManager.RunningAppProcessInfo
import android.os.Binder
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.KeyEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Host(val remoteService: RemoteService, val localService: ILocalService, val token: Int) {
    init{
        val x = runningAppProcess()
//        Log.e("gamebot", "recent task list: ${x.size}")
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

    fun stopConfigUI() {
        localService.stopConfigUI(token)
    }

    fun sendEmptyConfigUIEvent() {
        localService.sendEmptyConfigUIEvent(token)
    }

    fun takeNodeshot(): Nodeshot = remoteService.takeNodeshot()
    fun takeScreenshot(): Screenshot = remoteService.takeScreenshot()
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

    @Serializable
    data class ActivityName(
        @SerialName("package")
        val packageName: String = "",
        @SerialName("class")
        val className: String = ""
    )


    fun currentTask(): ActivityName {
        return remoteService.activityManager.getRecentTasks(1, 0)
            .firstOrNull()?.topActivity?.run {
                ActivityName(
                    packageName,
                    className
                )
            } ?: ActivityName()
    }

    fun recentTaskList(): List<ActivityName> {
        return remoteService.activityManager.getRecentTasks(Int.MAX_VALUE, 0).map {
            it.topActivity?.run {
                ActivityName(
                    packageName,
                    className
                )
            } ?: ActivityName()
        }.toList()
    }

    fun runningAppProcess() {
        Log.e("gamebot", RunningAppProcessInfo.IMPORTANCE_FOREGROUND.toString())
        Log.e("gamebot", RunningAppProcessInfo.IMPORTANCE_VISIBLE.toString())
        for (runningAppProcess in remoteService.activityManager.runningAppProcesses) {
//            Log.e("gamebot", "118 " + runningAppProcess.toString())
            Log.e("gamebot","119 " + runningAppProcess.processName)
            runningAppProcess.pkgList.forEach {
                Log.e("gamebot","120 " + it)
            }
//            Log.e("gamebot","120 " + .toString())
            Log.e("gamebot","121 " + runningAppProcess.importance)
        }
    }

}