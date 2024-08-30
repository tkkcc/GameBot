package gamebot.host.presentation

import gamebot.host.domain.TaskStatus
import gamebot.host.domain.crontab.Crontab
import gamebot.host.domain.crontab.CrontabInterval
import gamebot.host.domain.crontab.DayLimit
import gamebot.host.domain.crontab.DayOfWeekWhitelist
import gamebot.host.presentation.string.Strings
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.text.DateFormatSymbols

object ToUIString {
    lateinit var locale: java.util.Locale

    // init by root composable
    lateinit var S: Strings

    fun TaskStatus.toUIString(): String {

        // 1. 即将执行
        //2. 今天11:20结束，耗时10分钟       今天19:20开始
        //2. 昨天11:20结束，耗时10分钟       明天19:20开始
        //2. 昨天11:20结束，耗时10分钟       明天04:00开始
        //2. 昨天11:20结束，耗时10分钟       现在开始
        //2. 昨天11:20结束，耗时10分钟       现在开始
        val now = LocalDateTime.now()
        if (nextStartDateTime<=now) {
            return "现在开始"
        }
        val startDate = nextStartDateTime.toLocalDate()
        val nowDate = now.toLocalDate()

        if (startDate == LocalDateTime.MAX.toLocalDate()) {
            return "永不开始"
        }
        if (startDate <= nowDate.plusDays(3)){
            val prefix = when(startDate){
                nowDate -> "今天"
                nowDate.plusDays(1)->"明天"
                nowDate.plusDays(2)->"后天"
                nowDate.plusDays(3)->"大后天"
                else -> throw NotImplementedError()
            }
            val format =
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    .withLocale(locale)
            return prefix + nextStartDateTime.format(format) + "开始"
        }



        // we need ago library to format to shorter
        val format =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
//                .withZone(ZoneId.systemDefault())
                .withLocale(locale)
        return nextStartDateTime.format(format) + "开始"
    }

    fun Crontab.toUIString(): String {

        return if (this.enable) {
            val allTime = this.resolve()
            S.ScheduleSummary(this.first.toString(), allTime.size)
//            S.NoSchedule

        } else {
            S.NoSchedule
        }
    }

    fun DayOfWeekWhitelist.toUIString(): String {
        if (this.isAllAllowed()) {
            return S.NoLimit
        }

        // TODO does this need desugar
        val name = DateFormatSymbols().shortWeekdays
        return S.Skip + " " + this.banned().map {
            name[(it + 1) % 7 + 1]
        }.joinToString()
    }


//    // not yet https://github.com/Kotlin/kotlinx-datetime/discussions/253
//    fun LocalDate.toUIString(): String {
//        return S.LocalDateSummary(this.year, this.monthNumber, this.dayOfMonth)
//    }

    fun DayLimit.toUIString(): String {
        return if (!this.limitFirstDay && !this.limitLastDay) {
            S.NoLimit
        } else if (this.limitFirstDay && !this.limitLastDay) {
            this.toFirstDayUIString()
        } else if (this.limitLastDay && !this.limitFirstDay) {
            this.toLastDayUIString()
        } else {
            if (this.firstDay > this.lastDay) {
                return "无可执行日期，永不执行"
            }
            val summary = "可执行日期"
            val detail = if (this.firstDay == this.lastDay) {
                this.firstDay.toUIString()
            } else {
                this.firstDay.toUIString() + "至" + this.lastDay.toUIString()
            }
            "$summary: $detail"
        }
    }

    fun DayLimit.toFirstDayUIString(): String {
        return if (!this.limitFirstDay) {
            S.NoLimit
        } else {
            "${this.firstDay.toUIString()}0点起可执行"
        }
    }

    fun DayLimit.toLastDayUIString(): String {
        return if (!this.limitLastDay) {
            S.NoLimit
        } else {
            "${this.lastDay.toUIString()}24点前可执行"
        }
    }

    fun CrontabInterval.toUIString(): String = when (this) {
//                CrontabInterval.Hour0 -> "0小时"
        CrontabInterval.Hour1 -> "1小时"
        CrontabInterval.Hour2 -> "2小时"
        CrontabInterval.Hour3 -> "3小时"
        CrontabInterval.Hour4 -> "4小时"
        CrontabInterval.Hour6 -> "6小时"
        CrontabInterval.Hour8 -> "8小时"
        CrontabInterval.Hour12 -> "12小时"
    }


    fun Any?.toUIString() = when (this) {
        is TaskStatus -> {
            "taskStatus $this"
        }

        else -> {
            this.toString()
        }
    }
}