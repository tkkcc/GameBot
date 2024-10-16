package gamebot.host

//import gamebot.host.presentation.main.MainState
//import gamebot.host.presentation.main.MainViewModel
import RemoteService.Companion.remoteCache
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.InputField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fetchWithCache
import gamebot.host.LocalService.Companion.localCache
import gamebot.host.presentation.component.Section
import gamebot.host.presentation.component.SectionContent
import gamebot.host.presentation.component.SectionRow
import gamebot.host.presentation.component.SectionTextField
import gamebot.host.presentation.component.SimpleNavHost
import gamebot.host.presentation.component.SimpleScaffold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
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
    val guestList: List<Guest> = emptyList(),
    @Transient val downloadList: List<Download> = emptyList(),
    @Transient val filter: String = ""
)

@Serializable
data class Market(
    val host: HostInfo, val guest: List<Guest>
)

@Serializable
data class HostInfo(
    @SerialName("min_version") val minVersion: String
)

@Serializable
data class Guest(
    val name: String,
    val repo: String,
    val desc: String = "",
    val tag: List<String> = emptyList(),
    val icon: String = "",
    @Transient val state: GuestState = GuestState.BeforeUpdate,
) {
    fun match(filter: String): Boolean {
        if (filter.isEmpty()) {
            return true
        }
        if (name.contains(filter) || desc.contains(filter) || tag.any { it.contains(filter) }) {
            return true
        }
        return false
    }
}

sealed class GuestState {
    data object BeforeUpdate : GuestState()
    data class Stopped(val message: String) : GuestState()
    data object Started : GuestState()
}

@Serializable
sealed class DownloadState {
    data class Started(val progress: Float, val bytePerSecond: Float) : DownloadState()
    data class Stopped(val message: String) : DownloadState()
    data object Completed : DownloadState()
}

//@Serializable
data class Download(
    val url: String,
    val path: String,
    val sha256sum: String,
    val isRepo: Boolean,
    val state: DownloadState,
    val postProcess: (Result<Unit>) -> Unit
)


@OptIn(ExperimentalSerializationApi::class)
class MainViewModel(
    val remoteService: IRemoteService, val localService: LocalService
//    val startDownloadJob: (String, String, String, Boolean) -> String,
//    val stopDownloadJob: (String) -> Unit
) : ViewModel() {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _uiState = MutableStateFlow(MainState())
    val uiState = _uiState.asStateFlow()
    private val stateFile = File(localCache + "/state.json")

    init {
        // load state from disk
//        if (stateFile.exists()) {
        try {
            val state: MainState = Json.decodeFromStream(stateFile.inputStream())
            _uiState.update { state }
        } catch (e: Throwable) {
            d("fail to load ui state", e)
        }
//        }

        scope.launch {
            bootstrap()
        }
    }

//    fun checkAutoStart() {
//        if (File(remoteCache +"/guest/autostart").exists()) {
//
//        }
//    }

    suspend fun bootstrap() {
        // bootstrap: download libhost.so
        if (_uiState.value.afterBootstrap) {
            pullMarket()
            return
        }

        val abi = Build.SUPPORTED_ABIS[0]
        val url = "https://github.com/tkkcc/GameBot/releases/download/v0.0.1/libhost_$abi.so"
        val path = remoteCache + "/libhost.so"
        val sha256sum = """
                cafeb190c6692e475a09e5cf3710daadf28322018d93cb5f4f92d37d1638b0a3  libhost_arm64-v8a.so
                784740995471fc485a4d060c080665a7970d82dc79c4373b5f6da22189133ee0  libhost_armeabi-v7a.so
                55a060f0933585aff175e2c338b4057da0856ced3d32006a9d10e2ab3caed9eb  libhost_x86.so
                2d892f15266fda8ba0e16a7687c35b011df68bd333affb78b751f0e16b86b3bd  libhost_x86_64.so
            """.trimIndent().let {
            for (line in it.split("\n")) {
                val (hash, name) = line.split("  ")
                if (name == url.split("/").last()) {
                    return@let hash
                }
            }
            throw Exception("unsupported abi $abi")
        }
        startDownloadWaitSuccess(url, path, sha256sum, isRepo = false)

        _uiState.update {
            it.copy(afterBootstrap = true)
        }
        saveState()
        pullMarket()
    }


    suspend fun pullMarket() {
        // download market.toml
        val url = "https://raw.githubusercontent.com/tkkcc/GameBot/refs/heads/master/market.json"
        val cache = localCache + "/http_cache"
        val market = try {
            val byteArray = fetchWithCache(mirroredGithubUrl(url), cache)
            val market: Market =
                Json { ignoreUnknownKeys = true }.decodeFromStream(byteArray.inputStream())
            market
        } catch (e: Throwable) {
            // TODO notify user: show info on homeui
            return
        }

        // check if local autostart exist
        val guestList = market.guest.toMutableList()
        val autoStartExist = remoteService.autoStartExist()
        d("autoStartExist", autoStartExist)
        if (autoStartExist) {
            guestList.add(
                0, Guest(
                    name = "autostart", repo = "", desc = remoteCache + "/guest/autostart"
                )
            )
        }

        _uiState.update {
            it.copy(guestList = guestList)
        }
        saveState()

        // autostart
        if (autoStartExist) {
            startGuest(Guest(name = "autostart", repo = ""))
        }
    }


    suspend fun saveState() {
        scope.launch {
            try {
                Json.encodeToStream(_uiState.value, stateFile.outputStream())
            } catch (e: Exception) {
                d("fail to save ui state", e)
            }
        }
    }


    fun mirroredGithubUrl(url: String): String {
//        d("mirroredUrl before: $url")
        var mirroredUrl = url
        val mirror = _uiState.value.githubMirror
        if (mirror.isNotEmpty() && (isGithubUrl(url))) {
            mirroredUrl = "http://$mirror/$url"
        }
        return mirroredUrl
    }

    suspend fun startDownloadWaitSuccess(
        url: String, path: String, sha256sum: String, isRepo: Boolean
    ) {
        suspendCancellableCoroutine { cont ->
            startDownload(url, path, sha256sum, isRepo, {
                it.onSuccess {
                    cont.resumeWith(Result.success(Unit))
                }
            })
        }
    }

    fun startDownload(
        url: String,
        path: String,
        sha256sum: String,
        isRepo: Boolean,
        postProcess: (Result<Unit>) -> Unit
    ) = scope.launch {
        _uiState.update {
            val x = it.downloadList.toMutableList()
            val download = Download(
                url, path, sha256sum, isRepo, state = DownloadState.Started(0f, 0f), postProcess
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

        val ret = remoteService.startDownload(mirroredGithubUrl(url), path, sha256sum, isRepo)

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
            postProcess(Result.failure(Exception(ret)))
//                return@async Result.failure(Exception(ret))
        } else {
            postProcess(Result.success(Unit))
//                Result.failure(Exception(ret))
        }
//            Result.success(Unit)
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
        remoteService.stopDownload(path)
    }

    fun updateGithubMirror(mirror: String) {
        _uiState.update {
            it.copy(githubMirror = mirror)
        }
        scope.launch {
            saveState()
        }
    }

    fun startGuest(guest: Guest) {
        scope.launch {
            // update on first start
            if (guest.state is GuestState.BeforeUpdate && guest.repo.isNotEmpty()) {
                startDownloadWaitSuccess(
                    guest.repo,
                    remoteCache + "/guest/${guest.name}",
                    "",
                    isRepo = true,
                )
            }

            // set state to started
            _uiState.update {
                val x = it.guestList.toMutableList()
                val index = x.indexOfFirst { it.name == guest.name }
                x[index] = x[index].copy(state = GuestState.Started)
                it.copy(
                    guestList = x
                )
            }

            val ret = remoteService.startGuest(guest.name)

            // set state to stopped
            _uiState.update {
                val x = it.guestList.toMutableList()
                val index = x.indexOfFirst { it.name == guest.name }
                x[index] = x[index].copy(state = GuestState.Stopped(ret))
                it.copy(
                    guestList = x
                )
            }
        }
    }

    fun isGithubUrl(url: String): Boolean {
        return url.startsWith("https://github.com") || url.startsWith("https://raw.githubusercontent.com") || url.startsWith(
            "https://gist.github.com"
        ) || url.startsWith("https://gist.githubusercontent.com")
    }

    fun restartApp() {
        localService.restartApp()
//        TODO("Not yet implemented")
    }

    fun updateFilter(filter: String) {
        _uiState.update {
            it.copy(filter = filter)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUI(navController: NavController, viewModel: MainViewModel) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    val guestList by remember {
        derivedStateOf {
            val filter = state.value.filter
            state.value.guestList.filter {
                it.match(filter)
            }
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {

            TopAppBar(title={
                if (state.value.afterBootstrap) {
                    SectionTextField(
                        value = state.value.filter,
                        placeholder = "search",
                        shape = RoundedCornerShape(50),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        viewModel.updateFilter(it)
                    }
                }
            })
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize().padding(padding),
        ) {
            if (state.value.afterBootstrap) {
                Column(modifier = Modifier.fillMaxSize()) {



                    Section {

                        LazyColumn {


                            items(guestList, key = { it.name }) {


                                SectionRow({
                                    viewModel.startGuest(it)
                                    navController.navigate(Screen.Guest(name = it.name))
                                }) {
                                    SectionContent(it.name, it.desc)
                                }
                            }
                        }
                    }
                }
            } else {
                CenterDownloadUI(navController, viewModel, remoteCache + "/libhost.so")
            }
        }
    }

}

@Composable
fun CenterDownloadUI(
    navController: NavController,
    viewModel: MainViewModel,
    path: String,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val downloadOrNull by remember {
        derivedStateOf {
            state.value.downloadList.firstOrNull { it.path == path }
        }
    }
    val download = downloadOrNull ?: return
    val showProxy by remember {
        derivedStateOf {
            viewModel.isGithubUrl(download.url)
        }
    }

    CenterColumn {
        download.state.let {
            if (it is DownloadState.Stopped) {
                Text(it.message, color = MaterialTheme.colorScheme.error)
                IconButton(onClick = {
                    viewModel.startDownload(
                        download.url,
                        download.path,
                        download.sha256sum,
                        download.isRepo,
                        download.postProcess
                    )
                }) {
                    Icon(Icons.Default.RestartAlt, "restart")
                }
                return@let
            }

            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f), progress = {
                when (it) {
                    DownloadState.Completed -> 1f
                    is DownloadState.Started -> it.progress
                    is DownloadState.Stopped -> 0f
                }
            })
        }

        if (showProxy) {
            TextButton(onClick = {
                navController.navigate(Screen.Proxy)
            }) {
                Text("speed up")
            }
        }
    }
}

@Composable
fun DownloadUI(
    navController: NavController,
    viewModel: MainViewModel,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    SimpleScaffold(navController,
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
                            viewModel.startDownload(
                                it.url, it.path, it.sha256sum, it.isRepo, it.postProcess
                            )
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestUI(navController: NavController, viewModel: MainViewModel, name: String) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val guestOrNull by remember {
        derivedStateOf {
            state.value.guestList.firstOrNull { it.name == name }
        }
    }
    val guest = guestOrNull ?: return

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(guest.name, maxLines = 1)
        }, navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, "back")
            }
        })
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (guest.state) {
                GuestState.BeforeUpdate -> {
                    CenterDownloadUI(navController, viewModel, remoteCache + "/guest/${guest.name}")
                }

                GuestState.Started -> {
                    CenterColumn {
                        Text("started")
                    }
                }

                is GuestState.Stopped -> {
                    CenterColumn {
                        Text(guest.state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

}

@Composable
fun CenterColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )


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
    var proxyChanged by remember {
        mutableStateOf(false)
    }
    SimpleScaffold(
        navController, "Proxy for github", scrollable = false
    ) {
        Section(buildAnnotatedString {
            append("If download slow or fail, try set a proxy")
        }) {
            SectionTextField(state.value.githubMirror, placeholder = "no proxy", onValueChange = {
                proxyChanged = true
                viewModel.updateGithubMirror(it)
            })
        }

        val title = buildAnnotatedString {
            append("public on ")
            withLink(
                LinkAnnotation.Url(
                    "https://github.com/hunshcn/gh-proxy/issues",
                    TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary))
                )
            ) {
                append("hunshcn/gh-proxy")
            }
        }
        Section(title, modifier = Modifier.weight(1f)) {
            LazyColumn() {
                itemsIndexed(proxyList, key = { index: Int, _: String -> index }) { index, proxy ->
                    SectionRow(onClick = {
                        proxyChanged = true
                        viewModel.updateGithubMirror(proxy)
                    }) {
                        Text(proxy)
                    }
                }
            }
        }

        AnimatedVisibility(proxyChanged) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    viewModel.restartApp()
                }) {
                    Text("restart app to apply change")
                }
            }
        }
    }
}

@Composable
fun Test(navController: NavController,viewModel: MainViewModel) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val guestList by remember {
        derivedStateOf {
            val filter = state.value.filter
            state.value.guestList.filter {
                it.match(filter)
            }
        }
    }
    SimpleScaffold(navController, "Test") {
        Column(
            modifier = Modifier
//                .fillMaxSize().padding(padding),
        ) {
            if (state.value.afterBootstrap) {
                Column(modifier = Modifier.fillMaxSize()) {

                    SectionTextField(
                        value = state.value.filter,
                        placeholder = "search",
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        viewModel.updateFilter(it)
                    }

                    Section {

                        LazyColumn {


                            items(guestList, key = { it.name }) {


                                SectionRow({
                                    viewModel.startGuest(it)
                                    navController.navigate(Screen.Guest(name = it.name))
                                }) {
                                    SectionContent(it.name, it.desc)
                                }
                            }
                        }
                    }
                }
            } else {
                CenterDownloadUI(navController, viewModel, remoteCache + "/libhost.so")
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
//            AnimatedContent(state.value.afterBootstrap) {
//                if (it) {
            HomeUI(navController, viewModel)
//            Test(navController,viewModel)
//                } else {
//                    DownloadUI(navController, viewModel)
//                }
//            }
        }
        composable<Screen.Guest> {
            val guest = it.toRoute<Screen.Guest>()
            GuestUI(navController, viewModel, guest.name)
        }

//        composable<Screen.Download> {
//            DownloadUI(navController, viewModel)
//        }
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


