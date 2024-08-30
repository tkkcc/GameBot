package com.gamekeeper.star_rail_cn.presentation.crontab

import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import gamebot.host.domain.crontab.DayLimit
import gamebot.host.presentation.LocalStrings
import gamebot.host.presentation.component.Enable
import gamebot.host.presentation.component.Section
import gamebot.host.presentation.component.SectionSwitch
import gamebot.host.presentation.component.SimpleScaffold
import gamebot.host.presentation.schedule.ScheduleViewModel
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset


@Composable
fun FirstDayLimitScreen(
    navController: NavController,
    viewModel: ScheduleViewModel,
) {
    val detail = viewModel.detail.value.dayLimit
    val limit = detail.limitFirstDay
    val update = { dayLimit: DayLimit ->
        viewModel::updateDetail {
            it.copy(dayLimit = dayLimit)
        }
    }
    SimpleScaffold(navController, LocalStrings.current.FirstDay) {
        Section {
            SectionSwitch(title = LocalStrings.current.LimitFirstDay, checked = limit) {
                update(detail.copy(limitFirstDay = it))
            }
        }
        Enable(limit) {
            FirstDayLimitContent(detail.firstDay) {
                update(detail.copy(firstDay = it))
            }
        }
    }
}


// why we don't use com.maxkeppeler.sheets-compose-dialogs
// landscape top and bottom view is collapsed

// why we don't use https://github.com/marosseleng/compose-material3-datetime-pickers:
// can't change firstDayOfWeek

// why we can't use compose material3 date picker:
// we need kotlinx-datetime, so we need desugar(i also try ThreeTen)
// date picker with desugar give wrong weekday names for chinese

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstDayLimitContent(
    custom: LocalDate, update: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = custom.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis ?: return@LaunchedEffect

        update(
            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        )
    }


    DatePicker(
        title = null,
        state = datePickerState,
    )
}

