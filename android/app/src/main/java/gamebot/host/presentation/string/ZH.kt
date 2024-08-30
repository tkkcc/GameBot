package gamebot.host.presentation.string

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = "zh")
val ZH = EN.copy(
    simple = "dd",
    Add = "新增",
    Test = "te",
    Edit="编辑",
    Debug = "调试",
    More = "更多",
    Back = "返回",
    Schedule = "计划任务",
    Timing = "定时点",
    DayLimit = "日期限制",
    DayOfWeekLimit = "星期限制",
    NoLimit = "无限制",
    FirstTime = "首次时间",
    IntervalTime = "时间间隔",
    FirstDay = "开始日期",
    LastDay = "结束日期",
    LimitFirstDay = "限制开始日期",
    LimitLastDay = "限制结束日期",
    ScheduleSummary = { first, times ->
        "${first}点起，每日${times}次"
    },
    NoSchedule = "无计划任务",
    Skip = "跳过",
    ScheduleExecution= "定时执行",
    Priority = "优先级"
)