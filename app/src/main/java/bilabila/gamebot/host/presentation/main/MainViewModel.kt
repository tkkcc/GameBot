package bilabila.gamebot.host.presentation.main

import Container
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import bilabila.gamebot.host.domain.Task
import bilabila.gamebot.host.domain.TaskId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime


class MainViewModel(
    savedStateHandle: SavedStateHandle, container: Container
) : ViewModel() {
    private val taskRepository = container.taskRepository
    val runnerRepository = container.runnerRepository
    val localRun = container.localRun
    val remoteRun = container.remoteRun
    var state = mutableStateOf(MainState())
        private set

    init {
        Log.d("UI", "main view model init")

        viewModelScope.launch(Dispatchers.Default) {

            taskRepository.observeAll().collectLatest { new ->
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(
                        taskList = new
                    )
                }
            }
        }
    }

    val taskLock = Mutex()

    //    suspend fun addTask():Long = 100
    suspend fun addTask(): Long = viewModelScope.async(Dispatchers.Default) {
        // make multiple addTask safe
        taskLock.withLock {
            val count = taskRepository.count()
            taskRepository.add(
                Task(
                    name = "task1", type = "star_rail_cn", orderId = count
                )
            )
        }
    }.await()

    suspend fun addTask(content: List<Task>) =
        viewModelScope.launch(Dispatchers.Default) {
            // make multiple addTask safe
            taskLock.withLock {
                val count = taskRepository.count()
                content.forEachIndexed { index, task ->
                    taskRepository.add(
                        task.copy(
                            id = 0, orderId = count + index
                        )
                    )
                }
            }
        }

//
//    fun update(function: (DebugState) -> DebugState) {
//        // Write to room?
////        state.value = function(state.value)
//    }

    fun toggleEditMode() {
        state.value = state.value.let {
            it.copy(
                editMode = !it.editMode
            )
        }
    }

    fun start() {
        localRun.startTask()
    }

    fun updateSelectedTask(transform: MutableSet<Long>.() -> Unit) {
        state.value = state.value.let {

            it.copy(selectedTaskId = it.selectedTaskId.toMutableSet().apply(transform))
        }
    }

    fun toggleSelectedTask(id: Long) {
        updateSelectedTask {
            if (contains(id)) {
                remove(id)
            } else {
                add(id)
            }
        }
    }

    fun selectAllTask() {
        updateSelectedTask {
            if (state.value.selectedTaskId.size == state.value.taskList.size) {
                clear()
            } else {
                addAll(state.value.taskList.map { it.id })
            }
        }
    }

    fun selectAllSameTypeTask() {
        // TODO
        updateSelectedTask {
            addAll(state.value.taskList.map { it.id })
        }
    }

    enum class Direction {
        Up, Down
    }

    suspend fun moveSelectedTaskUp(): Int? =
        moveSelectedTaskUpDown()


    suspend fun moveSelectedTaskDown(): Int? =
        moveSelectedTaskUpDown(direction = Direction.Down)


    suspend fun moveSelectedTaskUpDown(
        direction: Direction = Direction.Up
    ): Int? = withContext(Dispatchers.Default) {
        taskLock.withLock {
            val down = direction == Direction.Down
            val taskList = state.value.taskList.toMutableList()
            val selectedTaskId = state.value.selectedTaskId


            val selectedOrderId = selectedTaskId.let { selectedId ->
                var list = taskList.filter {
                    selectedId.contains(it.id)
                }.map { it.orderId }
                if (down) {
                    list = list.reversed()
                }
                list
            }

            val changedOrderId = buildSet {
                val step = if (down) 1 else -1
                selectedOrderId.forEachIndexed { index, orderId ->
                    // can't move up or down, continue
                    val wall = if (down) taskList.size - 1 - index else index
                    if (orderId == wall) {
                        return@forEachIndexed
                    }
                    // swap current and above
                    val tmp = taskList[orderId]
                    taskList[orderId] = taskList[orderId + step]
                    taskList[orderId + step] = tmp

                    add(orderId + step)
                    add(orderId)
                }
            }
            val changedTask = buildList {
                changedOrderId.forEach { index ->
                    taskList[index] = taskList[index].copy(orderId = index)
                    add(taskList[index])
                }
            }

            changedTask.forEach {
                taskRepository.updateOrderId(it.id, it.orderId)
            }


            return@withContext changedTask.firstOrNull()?.orderId
        }
    }


    suspend fun duplicateSelectedTask() = withContext(Dispatchers.Default) {
        taskLock.withLock {
            val selectedTaskId = state.value.selectedTaskId
            if (selectedTaskId.isEmpty()) {
                return@withLock
            }
            val taskList = state.value.taskList.toMutableList()
            val allTask = buildList {
                var offset = 0
                taskList.forEachIndexed { index, task ->
                    add(task)
                    if (selectedTaskId.contains(task.id)) {
                        add(
                            task.copy(
                                id = 0,
                                name = "复制 " + task.name,
                                orderId = index + 1 + offset
                            )
                        )
                        offset += 1
                    }
                }
            }
            val otherTask = buildList {
                allTask.forEachIndexed { index, task ->
                    if (index != task.orderId) {
                        add(TaskId(task.id, index))
                    }
                }
            }
//            Log.d("TAG", newTask.toString())
//            Log.d("TAG", otherTask.toString())
//            Log.d("TAG", allTask.toString())

            // should be in single transaction
            taskRepository.add(allTask.filter { it.id == 0L })
            otherTask.forEach {
                taskRepository.updateOrderId(it.id, it.orderId)
            }

        }
    }

    lateinit var recycleTask: List<Task>
    lateinit var recycleSelection: Set<Long>
    suspend fun removeSelectedTask() = withContext(Dispatchers.Default) {
        taskLock.withLock {
            val selectedTaskId = state.value.selectedTaskId
            if (selectedTaskId.isEmpty()) {
                return@withLock
            }
            val taskList = state.value.taskList.toMutableList()

            // simply backup all
            recycleTask = taskList
            recycleSelection = selectedTaskId

            val (selectedTask, remainTask) = taskList.partition {
                selectedTaskId.contains(it.id)
            }
            val changedRemainTask = buildList {
                remainTask.forEachIndexed { index, task ->
                    if (index == task.orderId) {
                        return@forEachIndexed
                    }
                    add(TaskId(id = task.id, orderId = index))
                }
            }
//            val orderedRemainTask = remainTask.mapIndexed { index, task ->
//                if (index == task.orderId) task else task.copy(orderId = index)
//            }
            state.value = state.value.copy(
//                taskList = orderedRemainTask,
                selectedTaskId = emptySet()
            )
            // this should be in single transaction
            taskRepository.remove(selectedTask)
            changedRemainTask.forEach {
                taskRepository.updateOrderId(it.id, it.orderId)
            }
        }
    }

    suspend fun restoreRemovedTask() = withContext(Dispatchers.Default) {

        taskLock.withLock {
            if (!this@MainViewModel::recycleSelection.isInitialized) {
                return@withLock
            }
            state.value = state.value.copy(
//                taskList = recycleTask,
                selectedTaskId = recycleSelection
            )
            // simply re add all task, not efficient
            taskRepository.add(recycleTask)

        }
    }

    suspend fun enableRunNow(dateTime: LocalDateTime = LocalDateTime.MIN) =
        withContext(Dispatchers.Default) {
            taskLock.withLock {
                val selectedTaskId = state.value.selectedTaskId
                val taskList = state.value.taskList
                val changedTask = taskList.filter {
                    selectedTaskId.contains(it.id)
                }
                changedTask.forEach {
                    taskRepository.updateStatus(
                        it.id, it.status.copy(
                            nextStartDateTime = dateTime
                        )
                    )
                }
            }
        }

    suspend fun disableRunNow() =
        withContext(Dispatchers.Default) {
            taskLock.withLock {
                val selectedTaskId = state.value.selectedTaskId
                val taskList = state.value.taskList
                val changedTask = taskList.filter {
                    selectedTaskId.contains(it.id)
                }
                val now = LocalDateTime.now()
                changedTask.forEach {
                    val dateTime = it.schedule.nextDateTime(now)
                    taskRepository.updateStatus(
                        it.id, it.status.copy(
                            nextStartDateTime = dateTime
                        )
                    )
                }
            }
        }
    companion object {
        fun factory(
            container: Container,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return MainViewModel(
                    extras.createSavedStateHandle(), container
                ) as T
            }
        }
    }
}