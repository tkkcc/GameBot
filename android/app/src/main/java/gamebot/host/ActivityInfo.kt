package gamebot.host

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityInfo(
    @SerialName("package")
    val packageName: String = "",
    @SerialName("class")
    val className: String = ""
)

@Serializable
data class AppProcessInfo(
    @SerialName("process")
    val processName: String = "",
    val importance: Importance = Importance.Unknown
)

@Serializable
enum class Importance {
    Foreground,
    ForegroundService,
    Visible,
    Service,
    CantSaveState,
    Cached,
    Gone,
    Perceptible,
    TopSleeping,
    Unknown,
    NEW
}