package com.gamekeeper.star_rail_cn.presentation.crontab

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import gamebot.host.domain.crontab.DayOfWeekWhitelist
import gamebot.host.presentation.ToUIString.S
import gamebot.host.presentation.ToUIString.toUIString
import gamebot.host.presentation.component.ContentBody
import gamebot.host.presentation.component.Section
import gamebot.host.presentation.component.SimpleScaffold
import gamebot.host.presentation.schedule.ScheduleViewModel
import gamebot.host.presentation.schedule.WeekdayName


@Composable
fun DayOfWeekScreen(
    navController: NavController,
    viewModel: ScheduleViewModel,
) {
    val detail = viewModel.detail.value.dayOfWeekWhitelist
    val updateDetail = viewModel::updateDetail
//    Surface(color = Color.Red){
//
//    }
//    return
    SimpleScaffold(
        navController, S.DayOfWeekLimit,
        content = {
            DayOfWeekContent(detail) { new ->
                updateDetail { it.copy(dayOfWeekWhitelist = new) }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayOfWeekContent(
    custom: DayOfWeekWhitelist, update: (DayOfWeekWhitelist) -> Unit
) {
    Section {
        ContentBody(text = custom.toUIString(),modifier=Modifier.padding(16.dp,16.dp,16.dp,0.dp))
        FlowRow(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
//            maxItemsInEachRow = 7,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // TODO check if locale start from Sunday
            // TODO set short name of day or week based on locale
            val locale = LocalConfiguration.current.locales[0]
            val weekdayName = remember {
                WeekdayName(locale)
            }
            val first = weekdayName.firstDayOfWeek
            val name = weekdayName.absoluteWeekdayName
            Log.d("58", "$first $name ${weekdayName.weekdayName()}")
            TextToggle("日", custom.sunday, onClick = {
                update(custom.copy(sunday = it))
            })
            TextToggle("一", custom.monday, onClick = {
                update(custom.copy(monday = it))
            })
            TextToggle("二", custom.tuesday, onClick = {
                update(custom.copy(tuesday = it))
            })
            TextToggle("三", custom.wednesday, onClick = {
                update(custom.copy(wednesday = it))
            })
            TextToggle("四", custom.thursday, onClick = {
                update(custom.copy(thursday = it))
            })
            TextToggle("五", custom.friday, onClick = {
                update(custom.copy(friday = it))
            })
            TextToggle("六", custom.saturday, onClick = {
                update(custom.copy(saturday = it))
            })
        }
    }


}

@Composable
fun TextToggle(content: String, enabled: Boolean, onClick: (Boolean) -> Unit) {
    val contentColor by animateColorAsState(
        targetValue = if (!enabled) LocalContentColor.current else MaterialTheme.colorScheme.onPrimary
    )
    val containerColor by animateColorAsState(
        targetValue = if (!enabled) MaterialTheme.colorScheme.surfaceColorAtElevation(
            6.dp
        ) else MaterialTheme.colorScheme.primary
    )

    IconButton(
        modifier = Modifier.size(32.dp),
        onClick = { onClick(!enabled) },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        )
    ) {
        Text(content)
    }
}

@Preview
@Composable
fun Sample() {
    Card {
        TextToggle(content = "1", enabled = true) {

        }
    }
}