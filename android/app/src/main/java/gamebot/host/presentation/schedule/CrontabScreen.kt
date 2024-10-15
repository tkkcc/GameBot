package com.gamekeeper.star_rail_cn.presentation.crontab

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import gamebot.host.domain.crontab.Crontab
import gamebot.host.domain.crontab.CrontabInterval
import gamebot.host.presentation.ToUIString.S
import gamebot.host.presentation.ToUIString.toUIString
import gamebot.host.presentation.component.Enable
import gamebot.host.presentation.component.Section
import gamebot.host.presentation.component.SectionNavigation
import gamebot.host.presentation.component.SectionSelect
import gamebot.host.presentation.component.SectionSwitch
import gamebot.host.presentation.component.SimpleScaffold
import gamebot.host.presentation.schedule.ScheduleViewModel
import gamebot.host.presentation.schedule.Screen


@Composable
fun CrontabScreen(
    navController: NavController,
    viewModel: ScheduleViewModel,
) {
    val detail = viewModel.detail.value.crontab
    val updateDetail = viewModel::updateDetail
    SimpleScaffold(
        navController, "定时",
        content = {
            CrontabContent(navController, viewModel.id, detail) { new ->
                updateDetail { it.copy(crontab = new) }
            }
        },
    )

}

@Composable
fun CrontabContent(
    navController: NavController,
    id: Long,
    custom: Crontab,
    update: (Crontab) -> Unit
) {
    Section {
        SectionSwitch(
            title = "定时执行",
            info = "本任务将在以下时间加入执行队列",
            body = custom.resolve().joinToString(),
            checked = custom.enable,
            onChange = {
                update(
                    custom.copy(enable = it)
                )
            }
        )
        Enable(custom.enable) {
            SectionNavigation(
                title = S.FirstTime,
                body = custom.first.toUIString()
            ) {
                navController.navigate("${Screen.FirstTime}/$id")
            }
            SectionSelect(
                title = S.IntervalTime,
                body = custom.interval,
                selection = CrontabInterval.entries,
                onChange = {
                    update(custom.copy(interval = it))
                }
            )
        }
    }
}

