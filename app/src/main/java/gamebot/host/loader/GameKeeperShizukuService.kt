package gamebot.host.loader

import android.content.Context
import android.os.IBinder
import android.util.Log
import gamebot.host.GameKeeperActivity
import gamebot.host.GameKeeperActivity.Companion.TAG
import gamebot.host.IGameKeeperShizukuService
import dalvik.system.DexClassLoader
import java.io.File


class GameKeeperShizukuService(val context: Context) : IGameKeeperShizukuService.Stub() {

    companion object {
        val CODE_CACHE_DIR = "/data/local/tmp/gamekeeper/code_cache"
    }

    override fun destroy() {
        Log.d(TAG, "shizukuService destroy")
        System.exit(0)
    }

    override fun exit() {
        Log.d(TAG, "shizukuService exit")
        destroy()
    }


    override fun getRemoteRun(): IBinder {

//        Log.d(TAG, "dex, exist?  1")

        // in android 13, context.externalCacheDir cause security exception
        // and we can't pass string into here, so we make an assumption,
        // we also ensure at GameKeeperActivity onCreate, if it's wrong, it will show to user
        // tested on android 7.0 emulator, android 13 coloros
        // TODO: can this be exact，use contentProvider or aidl to pass this value
        val contextExternalCacheDir =
            "/storage/emulated/0/Android/data/${context.packageName}/cache"
        val dex = File(File(contextExternalCacheDir, "task"), "classes.dex").absolutePath

        val cache = File(CODE_CACHE_DIR, "task").absolutePath
        File(cache).mkdirs()

        val loader = DexClassLoader(
            dex, cache, null, this.javaClass.classLoader
        )
        val taskClass = loader.loadClass(GameKeeperActivity.TASK_REMOTE_RUN_CLASS)

        val x = taskClass.getConstructor(Context::class.java).newInstance(context)
//        for (m in taskClass.declaredMethods) {
//            Log.d(TAG, m.toString())
//        }
        val getBinder = taskClass.getDeclaredMethod("getBinder")
        return getBinder.invoke(x) as IBinder
    }
}