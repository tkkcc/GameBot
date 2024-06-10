package bilabila.gamebot.host

import android.os.Bundle
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
import bilabila.gamebot.host.loader.Git
import java.io.File
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {
    init {
        System.loadLibrary("rust")
    }

    external fun test(x: String): String

    private fun fetchRepo() : Result<Unit> = runCatching {
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
        val path = File(cacheDir, "repo")
        path.mkdirs()
        thread {
            val out = test(path.absolutePath)
            Log.e("", out)
        }
        val text = mutableStateOf("...")
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        TextButton(onClick = {
                            thread {
                                val result = fetchRepo()
                                text.value =result.toString()
                                result.getOrThrow()
                            }
                        }) {
                            Text("tap")
                        }
                        Text(text=text.value)
                    }
                }
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}
