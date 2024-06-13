package gamebot.host.loader

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File


// synchronous datastore on a json file
// why we don't use datastore: don't need Flow, use async on caller side
@OptIn(ExperimentalSerializationApi::class)
class JsonStore(private val path: String) {
    @Serializable
    data class Map(
        val data: MutableMap<String, String> = HashMap()
    )
    var map = Map()
    val file: File = File(path)

    init {
        load()
    }

    fun load() {
        if (!file.exists()) return
        map = Json.decodeFromStream(file.inputStream())
    }

    fun save() {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
        }
        Json.encodeToStream(map, file.outputStream())
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
