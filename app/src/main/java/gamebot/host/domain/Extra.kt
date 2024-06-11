package gamebot.host.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class Extra(
    @PrimaryKey
    val type: String = "",
    val detail: String = ""
)