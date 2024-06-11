package bilabila.gamebot.host

import android.app.Application
import android.util.Log
import com.jakewharton.threetenabp.AndroidThreeTen

class GameKeeperApplication:Application() {
    override fun onCreate() {
        Log.d("GameKeeper", "application onCreate")
        super.onCreate()
        AndroidThreeTen.init(this);


    }
}