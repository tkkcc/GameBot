package com.gamekeeper.star_rail_cn.presentation.crontab

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import gamebot.host.domain.crontab.DayLimit
import gamebot.host.presentation.LocalStrings
import gamebot.host.presentation.component.Enable
import gamebot.host.presentation.component.Section
import gamebot.host.presentation.component.SectionSwitch
import gamebot.host.presentation.component.SimpleScaffold
import gamebot.host.presentation.schedule.ScheduleViewModel


@Composable
fun LastDayLimitScreen(
    navController: NavController,
    viewModel: ScheduleViewModel,
) {

    val detail = viewModel.detail.value.dayLimit
    val limit = detail.limitLastDay
    val update = { dayLimit: DayLimit ->
        viewModel::updateDetail {
            it.copy(dayLimit = dayLimit)
        }
    }
    SimpleScaffold(navController, LocalStrings.current.LastDay) {
        Section {
            SectionSwitch(title = LocalStrings.current.LimitLastDay, checked = limit) {
                update(detail.copy(limitLastDay = it))
            }
        }
        Enable(limit) {
            FirstDayLimitContent(detail.lastDay) {
                update(detail.copy(lastDay = it))
            }
        }
    }
}


