package gamebot.host.domain

import kotlinx.serialization.Serializable

@Serializable
data class TaskDetail(
    val content: String = ""
)