package gamebot.host

//import gamebot.host.presentation.main.MainState
//import gamebot.host.presentation.main.MainViewModel
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import gamebot.host.presentation.component.SectionContent
import gamebot.host.presentation.component.SectionRow
import gamebot.host.presentation.component.SimpleNavHost
import gamebot.host.presentation.component.SimpleScaffold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

@Serializable
data class MainState(
    val afterBootstrap: Boolean = false,
    val githubMirror: String = "",
    @Transient val guestList: List<Guest> = emptyList(),
    @Transient val downloadList: List<Download> = emptyList()
)


@Serializable
data class Guest(
    val name: String,
    val description: String,
    val tag: List<String>,
    val icon: String,
    val repo: String
)

@Serializable
sealed class DownloadState {
    data class Started(val progress: Float, val bytePerSecond: Float) : DownloadState()
    data object Stopped : DownloadState()
    data object Completed : DownloadState()
}

@Serializable
data class Download(
    val url: String,
    val path: String,
    val state: DownloadState
//    val isStopped: Boolean = false,
//    val progress: Float = 0f,
//    val bytePerSecond: Float = 0f
)


@OptIn(ExperimentalSerializationApi::class)
class MainViewModel(
    cacheDir: String,
    val startDownloadJob: (String, String) -> Int,
    val stopDownloadJob: (String) -> Unit
) : ViewModel() {
    val scope = CoroutineScope(Dispatchers.Default)

    private val _uiState = MutableStateFlow(MainState())
    val uiState = _uiState.asStateFlow()
    private val stateFile = File(cacheDir + "/main_ui_state.json")

    init {
        d(92, stateFile, stateFile.exists())
        if (stateFile.exists()) {
            try {
                val state: MainState = Json.decodeFromStream(stateFile.inputStream())

                _uiState.update { state }
            } catch (e: Throwable) {
                d("fail to load ui state", e)
            }

        }

        val state = _uiState.value
        if (!state.afterBootstrap) {
            val abi = Build.SUPPORTED_ABIS[0]
            val url =
                state.githubMirror + "https://github.com/tkkcc/GameBot/releases/download/v0.0.1/libhost_$abi.so"
            val path = "/data/local/tmp/libhost.so"
            startDownload(url, path)
        }
    }

    fun saveState() {
        try {
            Json.encodeToStream(_uiState.value, stateFile.outputStream())
        } catch (e: Exception) {
            d("fail to save ui state", e)
        }
    }


    fun startDownload(url: String, path: String) {
        scope.launch {
            _uiState.update {
                val x = it.downloadList.toMutableList()
                val download = Download(
                    url = url,
                    path = path,
                    state = DownloadState.Started(0f, 0f)
                )
                val index = x.indexOfFirst { it.path == path }

                if (index == -1) {
                    x.add(download)
                } else {
                    x[index] = download
                }
                it.copy(
                    downloadList = x
                )
            }

            val code = startDownloadJob(url, path)

            _uiState.update {
                val x = it.downloadList.toMutableList()
                val index = x.indexOfFirst { it.path == path }
                var state = x[index].state
                if (state is DownloadState.Started) {
                    if (code == 0) {
                        state = DownloadState.Completed
                    } else {
                        state = DownloadState.Stopped
                    }
                }
                x[index] = x[index].copy(state = state)
                it.copy(
                    downloadList = x
                )
            }
        }
    }

    fun updateDownload(path: String, progress: Float, bytePerSecond: Float) {
        _uiState.update {
            val x = it.downloadList.toMutableList()
            val index = x.indexOfFirst { it.path == path }
            x[index] = x[index].copy(
                state = DownloadState.Started(progress, bytePerSecond)
            )
            it.copy(
                downloadList = x
            )
        }
    }


    fun stopDownload(path: String) {
        stopDownloadJob(path)
        _uiState.update {
            val x = it.downloadList.toMutableList()
            val index = x.indexOfFirst { it.path == path }
            x[index] = x[index].copy(
                state = DownloadState.Stopped
            )
            it.copy(
                downloadList = x
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUI(navController: NavController, viewModel: MainViewModel) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Home") }, actions = {
                IconButton(onClick = {
                    navController.navigate(Screen.Download)
                }) {
                    Icon(Icons.Default.Download, "download")
                }
            })
        },

        ) { padding ->

        LazyColumn(modifier = Modifier.padding(padding)) {
            items(state.value.guestList, key = { it.name }) {
                SectionRow({
                    navController.navigate(Screen.Guest(name = it.name))
                }) {
                    SectionContent(it.name, it.description, it.icon)
                }
            }
        }
    }

}

@Composable
fun DownloadUI(navController: NavController, viewModel: MainViewModel) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    SimpleScaffold(navController, "Download", scrollable = false) {
        LazyColumn {
            items(state.value.downloadList, key = { it.path }) {
                SectionRow {
                    val name = File(it.path).name
                    SectionContent(name, it.state.toString())
                    IconButton(onClick = {
                        if (it.state == DownloadState.Stopped) {
                            viewModel.startDownload(it.url, it.path)
                        } else if (it.state is DownloadState.Started) {
                            viewModel.stopDownload(it.path)
                        }
                    }) {
                        if (it.state == DownloadState.Stopped) {
                            Icon(Icons.Default.RestartAlt, "restart")
                        } else if (it.state is DownloadState.Started) {
                            Icon(Icons.Default.Stop, "stop")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuestUI(navController: NavController, viewModel: MainViewModel, guest: Screen.Guest) {
    SimpleScaffold(navController, guest.name) {

    }
}


@Composable
fun MainUI(
    viewModel: MainViewModel
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    AnimatedContent(state.value.afterBootstrap) {
        if (it) {
            SimpleNavHost(navController, startDestination = Screen.Home) {
                composable<Screen.Home> {
                    HomeUI(navController, viewModel)
                }
                composable<Screen.Guest> {
                    val guest = it.toRoute<Screen.Guest>()
                    GuestUI(navController, viewModel, guest)
                }
                composable<Screen.Download> {
                    DownloadUI(navController, viewModel)
                }
            }
        } else {
            DownloadUI(navController, viewModel)
        }
    }
}


sealed class Screen {
    @Serializable
    data object Home

    @Serializable
    data class Guest(
        val name: String
    )

    @Serializable
    data object Download
}


