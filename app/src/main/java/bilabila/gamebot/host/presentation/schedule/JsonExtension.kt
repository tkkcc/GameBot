package bilabila.gamebot.host.presentation.schedule

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class JsonExtension {
    companion object {
         inline fun <reified T> Json.decodeFromStringOrNull(string: String): T? {
            return try {
                decodeFromString(string)
            } catch (e: SerializationException) {
                null
            }
        }
    }
}