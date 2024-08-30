package gamebot.host.loader

import android.app.Activity
import android.content.Context

//  need to pass activity, can't use in remote service
class PrefStore {
    companion object {
        fun save(activity: Activity, key: String, value: String) {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
            with(sharedPref.edit()) {
                putString(key, value)
                apply()
            }
        }

        fun load(activity: Activity, key: String, default: String = ""): String {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return default
            return sharedPref.getString(key, default)!!
        }
    }
}