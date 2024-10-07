package gamebot.host

import android.util.Log

fun d(vararg msg: Any, exception: Exception? = null) {
    Log.e("gamebot", msg.joinToString { it.toString() }, exception)
}
