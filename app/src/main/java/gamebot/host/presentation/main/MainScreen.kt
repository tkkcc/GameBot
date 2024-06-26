package gamebot.host.presentation.main

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import gamebot.host.domain.Task
import gamebot.host.presentation.LocalStrings
import gamebot.host.presentation.Screen
import gamebot.host.presentation.ToUIString.toUIString
import gamebot.host.presentation.component.IconNext
import gamebot.host.presentation.component.SectionRow
import gamebot.host.presentation.component.SectionContent
import gamebot.host.presentation.component.SectionRow
import gamebot.host.presentation.component.SimpleAnimatedContent
import gamebot.host.presentation.component.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun MainScreen(
    navController: NavController,
//    vm: MainViewModel = viewModel()
) {
//    val state = vm.state.value
    val state = MainState(
        taskList = listOf(Task(
            name="name"
        ))
    )
//    state.taskList = listOf(Task())
    val taskList = state.taskList
    var editMode by remember {
        mutableStateOf(false)
    }
    val selectedTaskId = state.selectedTaskId
    val lazyColumn = rememberLazyListState()

    fun LazyListState.getLastVisibleItemIndex() {
        this.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    val context = LocalContext.current
    val exportTask =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/json")) { result ->
            scope.launch {
                var success = false
                var exportTaskNum = 0
                runCatching {
                    withContext(Dispatchers.Default) {
                        context.contentResolver.openOutputStream(result!!)!!.use { stream ->
                            val exportTask = taskList.filter {
                                selectedTaskId.contains(it.id)
                            }
                            exportTaskNum = exportTask.size
                            stream.write(Json.encodeToString(exportTask).toByteArray())
                            success = true
                        }
                    }
                }.onFailure {
                    Log.d("TAG", "export fail", it)
                }
                snackbarHostState.showSnackbar(if (success) "成功导出${exportTaskNum}项" else "导入失败")
            }
        }

    val importTask = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            scope.launch {
                var success = false
                var importTaskNum = 0
                runCatching {
                    withContext(Dispatchers.Default) {
                        context.contentResolver.openInputStream(uri!!)!!.use { stream ->
                            val bytes = stream.readBytes()
                            val newTask: List<Task> = Json.decodeFromString(bytes.decodeToString())
//                            vm.addTask(newTask)
                            importTaskNum = newTask.size
                            success = true
                        }
                    }
                }.onFailure {
                    Log.d("TAG", "export fail", it)
                }
                snackbarHostState.showSnackbar(if (success) "成功导入${importTaskNum}项" else "导入失败")
            }
        }
    )


    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(title = {
                SimpleAnimatedContent(targetState = editMode) {
                    if (editMode) {
                        Text("已选择${selectedTaskId.size}项")
                    } else {
                        Text("GameKeeper")
                    }
                }
            }, actions = {
                IconButton({
                    scope.launch {
//                        val id = vm.addTask()
                        val id = 1
//                        navController.navigate("${Screen.Detail}/${id}") {
//                            popUpTo(Screen.Main.toString())
//                        }
                    }
                }) {
                    Icon(Icons.Default.Add, LocalStrings.current.Add)
                }
                IconButton({ editMode =  ! editMode }) {
                    Icon(Icons.Default.Edit, LocalStrings.current.Edit)
                }
                IconButton({
//                    navController.navigate("debug")
                }) {
                    Icon(Icons.Default.Build, LocalStrings.current.Debug)
                }
            })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = editMode,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + expandIn(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + shrinkOut(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                )
            ) {

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp, 0.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton({  }) {
                        Text("全选")
                    }
                    TextButton({}) {
                        Text("全选同类")
                    }

                    TextButton({
                        scope.launch {
                            if (selectedTaskId.isEmpty()) return@launch
//                            vm.moveSelectedTaskUp()?.let {
//                                delay(100)
//                                val visible = lazyColumn.layoutInfo.visibleItemsInfo
//                                Log.d(
//                                    "",
//                                    "visible up ${visible.first().index} ${visible.last().index} $it"
//                                )
//                                val index = it + 1
//
//                                if (visible.isNotEmpty() && (visible.first().index > index) || (visible.last().index < index)) {
//                                    lazyColumn.animateScrollToItem(
//                                        index, 0
//                                    )
//                                }
//
//                            } ?: run {
//                                snackbarHostState.showSnackbar("已到顶")
//                            }
                        }
                    }) {
                        Text("上移")
                    }
                    TextButton({
                        scope.launch {
                            if (selectedTaskId.isEmpty()) return@launch
//                            vm.moveSelectedTaskDown()?.let {
//                                delay(100)
//                                val visible = lazyColumn.layoutInfo.visibleItemsInfo
//                                Log.d(
//                                    "",
//                                    "visible ${visible.first().index} ${visible.last().index} $it"
//                                )
//                                val index = it + 1
//
//                                if (visible.isNotEmpty() && (visible.first().index > index) || (visible.last().index < index)) {
//                                    Log.d("", "visible scroll to ${index}")
//                                    lazyColumn.animateScrollToItem(
//                                        index,
//                                        0
//                                    )
//                                }
//
//                            } ?: run {
//                                snackbarHostState.showSnackbar("已到底")
//                            }
                        }
                    }) {
                        Text("下移")
                    }
                    TextButton({
                        scope.launch {
                            if (selectedTaskId.isEmpty()) return@launch
//                            vm.duplicateSelectedTask()
                            snackbarHostState.showSnackbar("已重复${selectedTaskId.size}项")
                        }
                    }) {
                        Text("重复")
                    }
                    TextButton({
                        scope.launch {
                            if (selectedTaskId.isEmpty()) return@launch
                            val size = selectedTaskId.size
//                            vm.removeSelectedTask()
                            val result = snackbarHostState.showSnackbar(
                                "已删除${size}项",
                                actionLabel = "恢复",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
//                                vm.restoreRemovedTask()
                                snackbarHostState.showSnackbar(
                                    "已恢复${size}项",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }) {
                        Text("删除")
                    }



                    TextButton({
                        importTask.launch(arrayOf("application/json"))
                    }) {
                        Text("导入")
                    }

                    TextButton({
                        // TODO date and name
                        exportTask.launch("${1}.json")
                    }) {
                        Text("导出")
                    }
                    TextButton({

                    }) {
                        Text("修改")
                    }
                    TextButton({

                    }) {
                        Text("计划")
                    }
                    TextButton({
                        scope.launch {
//                            vm.enableRunNow()
                        }
                    }) {
                        Text("需要立即执行")
                    }
                    TextButton({
                        scope.launch {
//                            vm.disableRunNow()
                        }
                    }) {
                        Text("取消立即执行")
                    }
                }
//                }
//                }
            }


//            AnimatedVisibility(visible = editMode) {
//                Column(modifier = Modifier.fillMaxWidth()) {
//                    Text("a")
//
//                }
//
//            }
//        LazyColumn(modifier = Modifier.padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f), state = lazyColumn) {
                // it's the workaround for LazyColumn wrong auto scroll
                item {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                items(items = taskList, key = { it.id }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .padding(16.dp, 8.dp)
                        // animation is misleading: it's always pin first visible index.
                        // https://issuetracker.google.com/issues/209652366
//                            .animateItemPlacement(
//                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
//                            )
                    ) {
                        SimpleAnimatedContent(editMode) { editMode ->
                            if (editMode) {
                                val isSelected = selectedTaskId.contains(it.id)

                                SectionRow({
                                    Log.e("","what")
//                                    vm.toggleSelectedTask(it.id)
                                }) {
                                    SectionContent(it.name, it.status.toUIString())
                                    Space()
                                    RadioButton(selected = isSelected, onClick = null)
//                                    IconButton({
//                                        navController.navigate("${Screen.Schedule}/${it.id}")
//                                    }) {
//                                        Icon(Icons.Default.Schedule, "schedule")
//                                    }
//                                    IconNext()
                                }

                            } else {

                                SectionRow({
                                    Log.e("","what")

//                                    navController.navigate("detail/${it.id}")
                                }) {
                                    SectionContent(it.name, it.status.toUIString())
                                    Space()
                                    IconButton({
//                                        navController.navigate("${Screen.Schedule}/${it.id}")
                                    }) {
                                        Icon(Icons.Default.Schedule, "schedule")
                                    }
                                    IconNext()
                                }
                            }
                        }
                    }
                }
            }


        }

    }
//    AnimatedVisibility(visible = editMode) {
//        Column {
//            Text("ok")
//            Text("ok")
//            Text("ok")
//            Text("ok")
//            Text("ok")
//            Text("ok")
//            Text("ok")
//            Text("ok")
//            Text("ok")
//            Text("ok")
//            Text("ok")
//        }
//    }
//}

}