package gamebot.host

import android.util.Log

class TTT() {

    companion object {
        fun t(){
            Log.e("", "t")
        }

    }
    init {
        System.loadLibrary("rust")
    }
    external fun test(x: String): String

}