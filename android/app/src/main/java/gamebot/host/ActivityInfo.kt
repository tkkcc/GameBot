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
}

@Serializable
data class PackageInfo(
    @SerialName("name")
    val packageName: String = "",
    @SerialName("version")
    val versionName: String = "",
//    @SerialName("activity_list")
//    val activityList: List<String> = emptyList()
)