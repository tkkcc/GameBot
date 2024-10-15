package com.gamekeeper.star_rail_cn.presentation.crontab

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import gamebot.host.domain.crontab.DayLimit
import gamebot.host.presentation.LocalStrings
import gamebot.host.presentation.ToUIString.S
import gamebot.host.presentation.ToUIString.toFirstDayUIString
import gamebot.host.presentation.ToUIString.toLastDayUIString
import gamebot.host.presentation.component.Section
import gamebot.host.presentation.component.SectionNavigation
import gamebot.host.presentation.component.SimpleScaffold
import gamebot.host.presentation.schedule.ScheduleViewModel
import gamebot.host.presentation.schedule.Screen


@Composable
fun DayLimitScreen(
    navController: NavController,
    viewModel: ScheduleViewModel,
) {
    val detail = viewModel.detail.value.dayLimit
    val updateDetail = viewModel::updateDetail
    SimpleScaffold(
        navController, LocalStrings.current.DayLimit,
        content = {
            DayLimitContent(navController, viewModel.id, detail) { new ->
                updateDetail { it.copy(dayLimit = new) }
            }
        },
    )
}

@Composable
fun DayLimitContent(
    navController: NavController, id: Long,
    custom: DayLimit, update: (DayLimit) -> Unit
) {
    Section {
        SectionNavigation(
            title = S.FirstDay,
            body = custom.toFirstDayUIString(),
        ) {
            navController.navigate("${Screen.FirstDayLimit}/$id")
        }
        SectionNavigation(
            title = S.LastDay,
            body = custom.toLastDayUIString(),
        ) {
            navController.navigate("${Screen.LastDayLimit}/$id")
        }
    }
}

