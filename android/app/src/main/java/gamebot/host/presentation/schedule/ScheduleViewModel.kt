package gamebot.host.presentation.schedule

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import Container
import gamebot.host.domain.Task
import gamebot.host.domain.crontab.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ScheduleViewModel(
    savedStateHandle: SavedStateHandle,
    container: Container,
) : ViewModel() {

    val detail = mutableStateOf(Schedule())
    val taskRepository = container.taskRepository
    val id: Long = checkNotNull(savedStateHandle["id"])
    val task = mutableStateOf(Task())

    init {
        Log.d("TAG", "ScheduleViewModel init")
        viewModelScope.launch(Dispatchers.Default) {

            taskRepository.observe(id).collectLatest { new ->
//                Json.decodeFromStringOrNull<Schedule>(new.schedule)?.let {
//                    withContext(Dispatchers.Main) {
//                    }
//                }
                Log.d("", "new schedule ${new}")
                withContext(Dispatchers.Main) {
                    task.value = new
                    detail.value = new.schedule
                }
            }
        }
    }

    fun updateDetail(function: (Schedule) -> Schedule) {
        val new = function(detail.value)
//        detail.value = new
        val status = task.value.status

        viewModelScope.launch(Dispatchers.Default) {
            taskRepository.update(
                task.value.copy(
                    schedule = new,
                    status = status.copy(
                        nextStartDateTime = new.nextDateTime(status.lastExecuteDateTime.stop)
                    )
                )
            )
        }
    }

    companion object {
        fun factory(container: Container): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {

                    return ScheduleViewModel(extras.createSavedStateHandle(), container) as T
                }
            }
    }
}
