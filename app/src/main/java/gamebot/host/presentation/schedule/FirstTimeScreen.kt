package com.gamekeeper.star_rail_cn.presentation.crontab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import gamebot.host.domain.crontab.Crontab
import gamebot.host.presentation.LocalStrings
import gamebot.host.presentation.component.Section
import gamebot.host.presentation.component.SimpleScaffold
import gamebot.host.presentation.schedule.ScheduleViewModel
import org.threeten.bp.LocalTime


@Composable
fun FirstTimeScreen(
    navController: NavController,
    viewModel: ScheduleViewModel,
) {
    val detail = viewModel.detail.value.crontab
    val update = { new: Crontab ->
        viewModel::updateDetail {
            it.copy(crontab = new)
        }
    }
    SimpleScaffold(navController, LocalStrings.current.FirstTime) {
        FirstTimeContent(detail.first) { new ->
            update(detail.copy(first = new))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstTimeContent(
    custom: LocalTime, update: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = custom.hour,
        initialMinute = custom.minute,
//        is24Hour = true
    )

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        val hour = timePickerState.hour
        val minute = timePickerState.minute
        update(
            LocalTime.of(hour, minute)
        )
    }

    Section {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TimePicker(
                state = timePickerState,
            )
        }

    }
}

