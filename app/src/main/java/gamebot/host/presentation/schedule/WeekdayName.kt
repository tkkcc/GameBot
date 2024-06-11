package gamebot.host.presentation.schedule

import android.os.Build
import java.text.DateFormatSymbols
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale

// from compose DatePicker
class WeekdayName(val locale: Locale) {

    fun dayInISO8601(day: Int): Int {
        val shiftedDay = (day + 6) % 7
        return if (shiftedDay == 0) return /* Sunday */ 7 else shiftedDay
    }

    val firstDayOfWeek: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WeekFields.of(locale).firstDayOfWeek.value
        } else {
            dayInISO8601(Calendar.getInstance(locale).firstDayOfWeek)
        }

    val absoluteWeekdayName: List<Pair<String, String>> = buildList {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            with(locale) {
                addAll(DayOfWeek.entries.map {
                    it.getDisplayName(
                        java.time.format.TextStyle.FULL,
                        /* locale = */ this
                    ) to it.getDisplayName(
                        java.time.format.TextStyle.NARROW,
                        /* locale = */ this
                    )
                })
            }
        } else {

            val weekdays = DateFormatSymbols(locale).weekdays
            val shortWeekdays = DateFormatSymbols(locale).shortWeekdays
            // Skip the first item, as it's empty, and the second item, as it represents Sunday while it
            // should be last according to ISO-8601.
            weekdays.drop(2).forEachIndexed { index, day ->
                add(Pair(day, shortWeekdays[index + 2]))
            }
            // Add Sunday to the end.
            add(Pair(weekdays[1], shortWeekdays[1]))
        }
    }

    fun weekdayName(): List<Pair<String, String>> {
        val weekdays = absoluteWeekdayName
        val firstDayOfWeek = firstDayOfWeek
        val dayNames = arrayListOf<Pair<String, String>>()

        // Start with firstDayOfWeek - 1 as the days are 1-based.
        for (i in firstDayOfWeek - 1 until weekdays.size) {
            dayNames.add(weekdays[i])
        }
        for (i in 0 until firstDayOfWeek - 1) {
            dayNames.add(weekdays[i])
        }
        return dayNames
    }
}


