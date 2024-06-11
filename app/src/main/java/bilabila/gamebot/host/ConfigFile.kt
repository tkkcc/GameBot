package bilabila.gamebot.host

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

@Serializable
data class ConfigMap(
    val data: MutableMap<String, String> = HashMap()
)

@OptIn(ExperimentalSerializationApi::class)
class ConfigFile(private val path: String) {
    var map: ConfigMap = ConfigMap()
    val file: File = File(path)

    init {
        load()
    }

    fun load() {
        if (!file.exists()) return
        map = Json.decodeFromStream<ConfigMap>(file.inputStream())
    }

    fun save() {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
        }
        Json.encodeToStream<ConfigMap>(map, file.outputStream())
    }

    fun get(key: String, default: String = "", reload: Boolean = false): String {
        if (reload) {
            load()
        }
        return map.data.getOrDefault(key, default)
    }

    fun set(key: String, value: String, save: Boolean = true) {
        map.data[key] = value
        if (save) {
            save()
        }
    }

}
