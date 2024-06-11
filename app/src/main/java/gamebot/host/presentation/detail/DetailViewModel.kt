package gamebot.host.presentation.detail

//import dagger.hilt.android.lifecycle.HiltViewModel
import Container
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import gamebot.host.domain.Extra
import gamebot.host.domain.Task
import gamebot.host.presentation.component.CenterScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    val container: Container
) : ViewModel() {
    val runnerRepository = container.runnerRepository
    val taskRepository = container.taskRepository
    val extraRepository = container.extraRepository
    val fetchLoadRunner = container::fetchLoadRunner
    val localRun = container.localRun
    val remoteRun = container.remoteRun


    val id: Long = checkNotNull(savedStateHandle["id"])
    private var type: String = ""
//        private set

    //
//    var name = mutableStateOf("")
//        private set
    var ready = mutableStateOf(false)
        private set

//    var runner: MutableState<Runner> = mutableStateOf(LoadingRunner())
//        private set

    lateinit var configScreen: @Composable (navController: NavController, viewModel: DetailViewModel) -> Unit

    fun observe(): Flow<Task> = taskRepository.observe(id)

    suspend fun save(task: Task) = taskRepository.update(task)

    fun observeExtra(): Flow<String> {
        Log.d("UI", "observeExtra ${type} $id")
        return extraRepository.observe(type).map {
            it.detail
        }
    }


    suspend fun saveExtra(detail: String) {
        extraRepository.update(
            Extra(
                type = type,
                detail = detail,
            )
        )
//        Log.d("UI", "saved: ${value}")
    }


    init {

        Log.d("UI", "detail view model init $type $id")
        viewModelScope.launch(Dispatchers.Default) {
            Log.d("UI", "taskrepo try get id $id")
            val task = taskRepository.get(id) ?: run {
                configScreen = @Composable { navController, _ ->
                    CenterScaffold(navController = navController) {
                        Text("can't find task $id")
                    }
                }
                withContext(Dispatchers.Main) {
                    ready.value = true
                }
                return@launch
            }
//            withContext(Dispatchers.Main) {
            type = task.type
//            }
            extraRepository.addIfNotExist(
                Extra(
                    type = type
                )
            )
            val runner = fetchLoadRunner(type).getOrElse {

                configScreen = @Composable { navController, _ ->
                    CenterScaffold(navController = navController) {
                        Text("can't find runner $type\n\n${it.stackTraceToString()}")
                    }
                }
                withContext(Dispatchers.Main) {
                    ready.value = true
                }

                return@launch
            }
//            val x :@Composable (   NavController,
//                                    DetailViewModel) ->Unit = runner::ConfigScreen
            configScreen = @Composable { navController, viewModel ->
                runner.ConfigScreen(parentNavController = navController, realViewModel = viewModel)

//                SimpleScaffold(navController = navController, "name") {
//                    Section("日常") {
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                        SectionNavigation("标题", "官服官服") {}
//                    }
//                }
            }

//            withContext(Dispatchers.Main) {
//                f = @Composable{ parentNavController->
//                    SimpleScaffold(navController = parentNavController, "name") {
//                        Section("日常"){
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                            SectionNavigation("标题","官服官服"){}
//                        }
//                    }
//
//                }
            withContext(Dispatchers.Main) {
                ready.value = true
            }

//                name.value = "What"
//                this@DetailViewModel.runner.value = object : Runner {
//                    @Composable
//                    override fun ConfigScreen(
//                        parentNavController: NavController,
//                        realViewModel: DetailViewModel
//                    ) {
//                        SimpleScaffold(navController = parentNavController, "name") {
//                            Section("日常") {
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                                SectionNavigation("标题", "官服官服") {}
//                            }
//                        }
//                    }
//
//                }
//            }
        }
    }


    companion object {
        fun factory(
            container: Container,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val savedStateHandle = extras.createSavedStateHandle()

                return DetailViewModel(
                    savedStateHandle, container

                ) as T
            }
        }
    }
}