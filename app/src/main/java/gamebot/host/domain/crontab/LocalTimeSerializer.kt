package gamebot.host.domain.crontab

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.threeten.bp.LocalTime


@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = LocalTime::class)
object LocalTimeSerializer : KSerializer<LocalTime> {
    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeLong(value.toNanoOfDay())
    }

    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.ofNanoOfDay(decoder.decodeLong())
    }
}

