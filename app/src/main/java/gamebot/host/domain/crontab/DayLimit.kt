@file:UseSerializers(LocalDateSerializer::class)

package gamebot.host.domain.crontab

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.threeten.bp.LocalDate

@Serializable
data class DayLimit(
    val limitFirstDay: Boolean = false,
    val limitLastDay: Boolean = false,
    @Serializable(with = LocalDateSerializer::class)
    val firstDay: LocalDate = LocalDate.now(),
    @Serializable(with = LocalDateSerializer::class)
    val lastDay: LocalDate = LocalDate.now(),
) {

//    val startTime = LocalDateTime(this.firstDay, LocalTime(0,0))
//    val stopTime = LocalDateTime(this.lastDay, LocalTime(23,59))
}