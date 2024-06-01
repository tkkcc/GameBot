package bilabila.gamebot.host.loader

import android.app.Activity
import android.content.Context

object SaveLoadString {
    fun saveConfig(activity:Activity, key: String, value: String) {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(key, value)
            apply()
        }
    }

    fun loadConfig(activity: Activity,  key: String, default: String = ""): String {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return default
        return sharedPref.getString(key, default)!!
    }
}