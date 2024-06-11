package bilabila.gamebot.host.presentation.main

import bilabila.gamebot.host.domain.Task

data class MainState (
    val taskList:List<Task> = emptyList(),
    val editMode: Boolean = false,
    val selectedTaskId: Set<Long> = emptySet(),
)
