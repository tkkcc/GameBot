package bilabila.gamebot.host.presentation.string

data class Strings(
    val simple: String,
    val Add: String,
    val Edit: String,
    val Debug: String,
    val Test: String,
    val More: String,
    val Back: String,
    val apply: String,
    val modifyDisplay: String,
    val enableDebugServer: String,
    val reset: String,
    val Timing: String,
    val Schedule: String,
    val DayOfWeekLimit: String,
    val DayLimit: String,
    val NoLimit: String,
    val FirstTime: String,
    val IntervalTime: String,
    val FirstDay: String,
    val LastDay: String,
    val LimitFirstDay: String,
    val LimitLastDay: String,
    val NoSchedule:String,
    val ScheduleSummary: (first:String, times:Int)->String,
    val Skip:String,
    val LocalDateSummary: (year:Int,month:Int,day:Int) ->String,
    val ScheduleExecution: String,
    val Priority :String

) {
}

