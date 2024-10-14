package gamebot.host

//import gamebot.host.presentation.main.MainState
//import gamebot.host.presentation.main.MainViewModel
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NetworkCheck
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import gamebot.host.presentation.component.Section
import gamebot.host.presentation.component.SectionContent
import gamebot.host.presentation.component.SectionRow
import gamebot.host.presentation.component.SectionTextField
import gamebot.host.presentation.component.SimpleNavHost
import gamebot.host.presentation.component.SimpleScaffold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
    data class Stopped(val message: String) : DownloadState()
    data object Completed : DownloadState()
}

@Serializable
data class Download(
    val url: String,
    val path: String,
    val sha256sum: String,
    val state: DownloadState

//    val isStopped: Boolean = false,
//    val progress: Float = 0f,
//    val bytePerSecond: Float = 0f
) {
}


@OptIn(ExperimentalSerializationApi::class)
class MainViewModel(
    cacheDir: String,
    val startDownloadJob: (String, String, String) -> String,
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
            scope.launch {
                val abi = Build.SUPPORTED_ABIS[0]
                val url =
                    state.githubMirror + "https://github.com/tkkcc/GameBot/releases/download/v0.0.1/libhost_$abi.so"
                val path = "/data/local/tmp/libhost.so"
                val sha256sum = """
                cafeb190c6692e475a09e5cf3710daadf28322018d93cb5f4f92d37d1638b0a3  libhost_arm64-v8a.so
                784740995471fc485a4d060c080665a7970d82dc79c4373b5f6da22189133ee0  libhost_armeabi-v7a.so
                55a060f0933585aff175e2c338b4057da0856ced3d32006a9d10e2ab3caed9eb  libhost_x86.so
                2d892f15266fda8ba0e16a7687c35b011df68bd333affb78b751f0e16b86b3bd  libhost_x86_64.so
            """.trimIndent().let {
                    for (line in it.split("\n")) {
                        val (hash, name) = line.split("  ")
                        d(hash, name)
                        if (name == url.split("/").last()) {
                            return@let hash
                        }
                    }
                    throw Exception("unsupported abi $abi")
                }

                val ret = startDownload(url, path, sha256sum).await()

                if (ret.isSuccess) {
                    _uiState.update {
                        it.copy(afterBootstrap = true)
                    }
                    saveState()
                }
            }
        }
    }

    fun saveState() {
        try {
            Json.encodeToStream(_uiState.value, stateFile.outputStream())
        } catch (e: Exception) {
            d("fail to save ui state", e)
        }
    }


    fun startDownload(url: String, path: String, sha256sum: String): Deferred<Result<Unit>> =
        scope.async {
//        scope.launch {
            _uiState.update {
                val x = it.downloadList.toMutableList()
                val download = Download(
                    url = url,
                    path = path,
                    sha256sum = sha256sum,
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

            val ret = startDownloadJob(url, path, sha256sum)

            _uiState.update {
                val x = it.downloadList.toMutableList()
                val index = x.indexOfFirst { it.path == path }
                var state = x[index].state
                if (state is DownloadState.Started) {
                    if (ret.isEmpty()) {
                        state = DownloadState.Completed
                    } else {
                        state = DownloadState.Stopped(ret)
                    }
                }
                x[index] = x[index].copy(state = state)
                it.copy(
                    downloadList = x
                )
            }
            if (ret.isNotEmpty()) {
                return@async Result.failure(Exception(ret))
            }
            Result.success(Unit)
        }
//    }

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
    }

    fun updateGithubMirror(mirror: String) {
        _uiState.update {
            it.copy(githubMirror = mirror)
        }
        saveState()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadUI(
    navController: NavController,
    viewModel: MainViewModel,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    SimpleScaffold(
        navController,
        title = "Download",
        hideBackButton = !state.value.afterBootstrap,
        scrollable = false,
        actions = {
            IconButton(onClick = {
                navController.navigate(Screen.Proxy)
            }) {
                Icon(Icons.Default.NetworkCheck, "proxy")
            }
        }) {
        LazyColumn() {
            items(state.value.downloadList, key = { it.path }) {
                SectionRow {
                    val name = File(it.path).name
                    SectionContent(name, it.state.toString())
                    IconButton(onClick = {
                        if (it.state is DownloadState.Stopped) {
                            viewModel.startDownload(it.url, it.path, it.sha256sum)
                        } else if (it.state is DownloadState.Started) {
                            viewModel.stopDownload(it.path)
                        }
                    }) {
                        if (it.state is DownloadState.Stopped) {
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
fun ProxyUI(navController: NavController, viewModel: MainViewModel) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    // from https://github.com/hunshcn/gh-proxy/issues/116#issuecomment-2410678798
    val proxyList = """
        101.32.202.184:10086
        101.43.36.238:4080
        111.229.117.180:2068
        119.28.4.250
        124.156.150.245:10086
        124.156.158.242:4000
        13.230.117.137:8008
        130.162.130.196
        136.243.215.211:12345
        138.2.123.193:8090
        138.2.54.229:12580
        138.2.69.119:30001
        139.196.123.118:12345
        140.238.17.136:9980
        140.238.33.157:3000
        141.147.170.49
        150.138.79.19:12345
        152.67.215.236:12345
        152.67.215.57:8081
        152.67.219.235:8989
        152.70.36.140:88
        152.70.94.22:9080
        155.248.180.127:3000
        158.101.152.90:8123
        158.180.92.175:8000
        16.163.43.131:880
        212.50.233.214:8888
        38.207.160.46:6699
        43.129.191.251:8088
        43.132.227.252:9090
        43.133.162.210:9000
        43.154.105.8:8888
        43.154.123.246
        43.154.99.97:1112
        43.163.230.97:800
        45.149.156.201:7080
        47.109.58.212:8082
        47.236.114.62:18080
        47.245.88.61
        47.75.211.166:5080
        47.95.0.182:2333
        51.195.241.253:8080
        74.48.108.189:10088
        8.210.13.120
        8.210.153.246:9000
        8.210.207.225:8888
        82.157.146.148:9001
        94.74.100.230:9010
        a.whereisdoge.work
        admin.whereisdoge.work
        autodiscover.whereisdoge.work
        blog.whereisdoge.work
        cdn.moran233.xyz
        cf.ghproxy.cc
        cloud.whereisdoge.work
        cpanel.whereisdoge.work
        cpcalendars.whereisdoge.work
        cpcontacts.whereisdoge.work
        g.blfrp.cn
        gh-proxy.com
        gh-proxy.llyke.com
        gh.222322.xyz
        gh.6yit.com
        gh.catmak.name
        gh.con.sh
        gh.nxnow.top
        gh.pylas.xyz
        gh.sixyin.com
        gh.xx9527.cn
        ghp.arslantu.xyz
        ghp.ci
        ghp.miaostay.com
        ghpr.cc
        ghproxy.cc
        ghproxy.cianogame.top
        ghproxy.cn
        ghproxy.homeboyc.cn
        ghproxy.lainbo.com
        ghps.cc
        git.19970301.xyz
        git.40609891.xyz
        git.669966.xyz
        github.moeyy.xyz
        github.muou666.com
        hub.gitmirror.com
        m.whereisdoge.work
        mail.whereisdoge.work
        mirror.ghproxy.com
        mtp.whereisdoge.work
        ql.133.info
        slink.ltd
        v.whereisdoge.work
        webdav.camus.xyz
        webdisk.whereisdoge.work
        www.ghpr.cc
        x.whereisdoge.work
        xxqg.168828.xyz:8088
        y.whereisdoge.work
        zipchannel.top:4000
    """.trimIndent().split("\n")

    SimpleScaffold(navController, "Proxy for github", scrollable = false) {
//        Text(
//            modifier = Modifier.padding(horizontal = 32.dp)
//        )
        Section(
            buildAnnotatedString {
                append("If download fail, try set a proxy. Input your own, or choose a public host")
            }
        ) {
            SectionTextField(
                state.value.githubMirror,
                placeholder = "no proxy",
                onValueChange = {
                    viewModel.updateGithubMirror(it)
                })
        }
        val title = buildAnnotatedString {
            append("public on ")
            withLink(
                LinkAnnotation.Url(
                    "https://github.com/hunshcn/gh-proxy/issues",
                    TextLinkStyles(SpanStyle(color = Color.Blue))
                )
            ) {
                append("hunshcn/gh-proxy")
            }
        }
        Section(
            title,
        ) {

            LazyColumn {

                itemsIndexed(proxyList, key = { index: Int, _: String -> index }) { index, proxy ->
                    SectionRow(onClick = {
                        viewModel.updateGithubMirror(proxy)
                    }) {
                        Text(proxy, modifier = Modifier.clickable {
                            viewModel.updateGithubMirror(proxy)
                        })
                    }
                }
            }
        }

    }
}


@Composable
fun MainUI(
    viewModel: MainViewModel
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    SimpleNavHost(navController, Screen.Home) {
        composable<Screen.Home> {
            AnimatedContent(state.value.afterBootstrap) {
                if (it) {
                    HomeUI(navController, viewModel)
                } else {
                    DownloadUI(navController, viewModel)
                }
            }
        }
        composable<Screen.Guest> {
            val guest = it.toRoute<Screen.Guest>()
            GuestUI(navController, viewModel, guest)
        }
        composable<Screen.Download> {
            DownloadUI(navController, viewModel)
        }
        composable<Screen.Proxy> {
            ProxyUI(navController, viewModel)
        }
    }
}


sealed class Screen {
    @Serializable
    data object Home : Screen()

    @Serializable
    data class Guest(
        val name: String
    ) : Screen()

    @Serializable
    data object Download : Screen()

    @Serializable
    data object Proxy : Screen()
}


