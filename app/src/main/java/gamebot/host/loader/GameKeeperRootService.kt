package gamebot.host.loader

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService


class GameKeeperRootService : RootService() {
    override fun onBind(intent: Intent): IBinder =
        GameKeeperShizukuService(this)
}
