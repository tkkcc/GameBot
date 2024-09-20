package gamebot.host

import Nodeshot
import RemoteService
import RemoteService.*
import Screenshot
import android.os.Binder
import android.os.ParcelFileDescriptor
import android.view.KeyEvent

class Host(val remoteService: RemoteService, val localService: ILocalService, val token: Int) {
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
}