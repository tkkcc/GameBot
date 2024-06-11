package bilabila.gamebot.host.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import bilabila.gamebot.host.domain.crontab.Schedule
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val type: String = "",
    val detail: String = "",
    val schedule: Schedule = Schedule(),
    val orderId: Int = 0,
    val status: TaskStatus = TaskStatus()
) {

    fun toTaskId(): TaskId {
        return TaskId(id = id, orderId = orderId)
    }

}


data class TaskId(
    val id: Long,
    val orderId: Int
)


