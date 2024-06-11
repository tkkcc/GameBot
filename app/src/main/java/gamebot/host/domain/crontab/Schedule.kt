package gamebot.host.domain.crontab

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.threeten.bp.LocalDateTime


@Serializable
data class Schedule(
    val priority: Int = 50,
    val crontab: Crontab = Crontab(),
    val dayOfWeekWhitelist: DayOfWeekWhitelist = DayOfWeekWhitelist(),
    val dayLimit: DayLimit = DayLimit(),
) {
    suspend fun nextDateTime(now: LocalDateTime): LocalDateTime = withContext(Dispatchers.Default) {
        val allowTime = crontab.resolve()
        if (allowTime.isEmpty())
            return@withContext LocalDateTime.MAX

        val allowDayOfWeek = dayOfWeekWhitelist.toList()
        if (allowDayOfWeek.find { it } == null) {
            return@withContext LocalDateTime.MAX
        }

//        Log.d("",dayLimit.toString())
//        Log.d("",now.toLocalDate().toString())
//        Log.d("",now.toLocalDate().toString())
        if (dayLimit.limitLastDay && dayLimit.lastDay < now.toLocalDate()) {
            return@withContext LocalDateTime.MAX
        }

        if (dayLimit.limitFirstDay && dayLimit.limitLastDay && dayLimit.firstDay > dayLimit.lastDay) {
            return@withContext LocalDateTime.MAX
        }


        var day = now.toLocalDate()
        if (dayLimit.limitFirstDay && dayLimit.firstDay > day) {
            day = dayLimit.firstDay
        }
        var lastAllowDay = day.plusDays(7)
        if (dayLimit.limitLastDay && dayLimit.lastDay < lastAllowDay){
            lastAllowDay = dayLimit.lastDay
        }
        while (day <= lastAllowDay) {
//            Log.d("TAG", "day39 ${day} ${day.dayOfWeek} ${allowDayOfWeek}")
            if (allowDayOfWeek[day.dayOfWeek.value-1]) {
                for (time in allowTime) {
                    val candidate = time.atDate(day)
                    if (candidate > now) {
                        return@withContext candidate
                    }
                }
            }


            day = day.plusDays(1)
        }

        return@withContext LocalDateTime.MAX
    }
}