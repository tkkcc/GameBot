package gamebot.host.presentation.string

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = "en")
val EN = Strings(
    simple = "dd",
    Add = "Add",
    Test = "test",
    Edit = "Edit",
    Debug = "Debug",
    More = "More",
    Back = "Back",
    enableDebugServer = "start debug server",
    apply = "Modify",
    reset = "reset",
    modifyDisplay = "Modify display",
    Schedule = "Schedule",
    Timing = "Timing",
    DayLimit = "Date range",
    DayOfWeekLimit = "Day of week",
    NoLimit = "No limit",
    FirstTime = "First time",
    IntervalTime = "Time interval",
    FirstDay = "Start date",
    LastDay = "End date",
    LimitFirstDay = "Limit start date",
    LimitLastDay = "Limit end date",
    ScheduleSummary = { first, times ->
        "since $first, $times times per day"
    },
    NoSchedule = "No schedule",
    Skip = "Skip",
    LocalDateSummary = { year, month, day ->
        // TODO
        "todo $year.$month.$day"
    },
    ScheduleExecution = "Scheduled Execution",
    Priority = "Priority"
)