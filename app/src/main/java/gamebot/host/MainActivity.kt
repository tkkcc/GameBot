package gamebot.host

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.topjohnwu.superuser.ipc.RootService
import gamebot.host.LocalRun.Companion.TAG
import gamebot.host.loader.GameKeeperShizukuService
import gamebot.host.loader.Git
import java.io.File
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {
    init {
        System.loadLibrary("rust")
    }

    external fun test(x: String): String

    private fun fetchRepo(): Result<Unit> = runCatching {
        val path = File(cacheDir, "repo").absolutePath
        Git.clone(
            "https://e.coding.net/bilabila/gamekeeper/star_rail_cn.git",
            File(cacheDir, "repo").absolutePath
        ).getOrThrow()
        Git.pull(path).getOrThrow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // test jni call

        val path = File(cacheDir, "repo")
        path.mkdirs()

        thread {
            val out = test(path.absolutePath)
            Log.e("", out)
            test()
        }

        val text = mutableStateOf("...")
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        TextButton(onClick = {
                            thread {
                                text.value = "running..."
                                val result = fetchRepo()
                                text.value = result.toString()
                                result.getOrThrow()
                            }
                        }) {
                            Text("tap")
                        }
                        Text(text = text.value)
                    }
                }
            }
        }
    }

    private fun test() {
        val context = this@MainActivity
        context.setContent {
            Text("what1")
        }
        class GameBotService(context: Context) : IRemoteService.Stub() {
            override fun setLocalRunBinder(binder: IBinder?) {
                TODO("Not yet implemented")
            }

            override fun start() {
                Log.e("137", "start in remote service")
            }
        }

        // let's connect to service, and let service call toast function
        class GameBotRootService : RootService() {
            override fun onBind(intent: Intent): IBinder = GameBotService(this)
        }

        var remoteService: IRemoteService

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.d(TAG, "RootServiceConnection onConnected")
                remoteService = IRemoteService.Stub.asInterface(service)
                remoteService.start()
                // why we use Messenger instead of plain AIDL:
                // the plugin run is inside RemoteHandler, that is AIDLService
                // it's needs to control ui via LocalHandler,
                // if using plain AIDL we need to define a callback interface
                // using Messenger is easier to let remote control local
//                remoteService = IGameKeeperShizukuService.Stub.asInterface(service)
//
//                remoteRun = IRemoteRun.Stub.asInterface(remoteService.remoteRun)
//                container = Container(activity, localRun, remoteRun)
//
//                val localRunBinder = this@LocalRun.asBinder()
//                remoteRun.setLocalRunBinder(localRunBinder)
//                remoteRun.start()
//
//                Log.d(TAG, remoteRun.overrideDisplaySize.toString())
//                Log.d(TAG, remoteRun.overrideDisplayDensity.toString())
//                Log.d(TAG, remoteRun.physicalDisplayDensity.toString())
//            remoteRun.test()

            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d("137", "onServiceDisconnected")
            }
        }

        fun tryRootMode() {
            runOnUiThread {
                val intent = Intent(this@MainActivity, GameBotRootService::class.java)
                RootService.bind(intent, connection)
            }
        }

        tryRootMode()

    }


}


