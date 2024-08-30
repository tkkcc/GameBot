@file:UseSerializers(LocalTimeSerializer::class)

package gamebot.host.domain.crontab

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

@Serializable
data class Crontab(
    val enable: Boolean = false,
    @Serializable(with = LocalTimeSerializer::class)
    val first: LocalTime = LocalTime.of(4, 0),
    val interval: CrontabInterval = CrontabInterval.Hour8
) {
    fun resolve(): List<LocalTime> {
        if (this.enable== false){
            return emptyList()
        }

        val now = LocalDateTime.now()
        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay()
        return buildList {
            var candidate = now.toLocalDate().atTime(first)
            while(candidate < nextDay){
                add(candidate.toLocalTime())
                candidate = candidate.plusHours(interval.toHour().toLong())
            }
        }

    }
}