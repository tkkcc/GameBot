package bilabila.gamebot.host.presentation.main

import Container
import android.graphics.Point
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras


class DebugViewModel(
    savedStateHandle: SavedStateHandle,
    container: Container
) : ViewModel() {

    val taskRepository = container.taskRepository
    val runnerRepository = container.runnerRepository
    val extraRepository = container.extraRepository
    val localRun = container.localRun
    val remoteRun = container.remoteRun

    var state = mutableStateOf(DebugState())
        private set

    init {
        Log.d("UI", "main view model init")


    }


    fun update(function: (DebugState) -> DebugState) {
        // Write to room?
        state.value = function(state.value)

    }

    fun applyDisplay() {
        val width = state.value.displayWidth
        val height = state.value.displayHeight
        val dpi = state.value.displayDensity
        // TODO it's need check
        remoteRun.setOverrideDisplaySize(Point(width.toInt(), height.toInt()))
//        TODO("Not yet implemented")
    }


    companion object {
        fun factory(
            container: Container,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return DebugViewModel(
                    extras.createSavedStateHandle(),
                    container,
                ) as T
            }
        }
    }
}