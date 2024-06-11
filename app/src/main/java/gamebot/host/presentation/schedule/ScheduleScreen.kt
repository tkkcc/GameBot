package gamebot.host.presentation.schedule

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.gamekeeper.star_rail_cn.presentation.crontab.CrontabScreen
import com.gamekeeper.star_rail_cn.presentation.crontab.DayLimitScreen
import com.gamekeeper.star_rail_cn.presentation.crontab.DayOfWeekScreen
import com.gamekeeper.star_rail_cn.presentation.crontab.FirstDayLimitScreen
import com.gamekeeper.star_rail_cn.presentation.crontab.FirstTimeScreen
import com.gamekeeper.star_rail_cn.presentation.crontab.LastDayLimitScreen
import Container
import gamebot.host.presentation.LocalStrings
import gamebot.host.presentation.ToUIString.toUIString
import gamebot.host.presentation.component.Section
import gamebot.host.presentation.component.SectionNavigation
import gamebot.host.presentation.component.SectionSlider
import gamebot.host.presentation.component.SimpleScaffold
import kotlin.math.roundToInt

internal enum class Screen {
    Schedule,
    Crontab,
    DayLimit,
    DayOfWeek,
    FirstDayLimit,
    LastDayLimit,
    FirstTime
}

fun NavGraphBuilder.scheduleGraph(
    navController: NavController,
    container: Container,
) {
    navigation(
        startDestination = "${Screen.Schedule}", route = "${Screen.Schedule}/{id}",
        arguments = listOf(navArgument(name = "id") { type = NavType.LongType })
    ) {

        composable(
            "${Screen.Schedule}",
//            arguments = listOf(navArgument(name = "id") { type = NavType.LongType })
        ) {
            ScheduleScreen(
                navController,
//                viewModel(factory = ScheduleViewModel.factory(container))
                it.sharedScheduleViewModel(navController, container)
            )
        }
        composable(
            "${Screen.Crontab}/{id}",
            arguments = listOf(navArgument(name = "id") { type = NavType.LongType })
        ) {
            CrontabScreen(
                navController,
                it.sharedScheduleViewModel(navController, container)
            )
        }

        composable(
            "${Screen.DayOfWeek}/{id}",
            arguments = listOf(navArgument(name = "id") { type = NavType.LongType })
        ) {
            DayOfWeekScreen(
                navController,
                it.sharedScheduleViewModel(navController, container)
            )
        }
        composable(
            "${Screen.DayLimit}/{id}",
            arguments = listOf(navArgument(name = "id") { type = NavType.LongType })
        ) {
            DayLimitScreen(
                navController,
                it.sharedScheduleViewModel(navController, container)
            )
        }
        composable(
            "${Screen.FirstDayLimit}/{id}",
            arguments = listOf(navArgument(name = "id") { type = NavType.LongType })
        ) {
            FirstDayLimitScreen(navController, it.sharedScheduleViewModel(navController, container))
        }

        composable(
            "${Screen.LastDayLimit}/{id}",
            arguments = listOf(navArgument(name = "id") { type = NavType.LongType })
        ) {
            LastDayLimitScreen(navController, it.sharedScheduleViewModel(navController, container))
        }

        composable(
            "${Screen.FirstTime}/{id}",
            arguments = listOf(navArgument(name = "id") { type = NavType.LongType })
        ) {
            FirstTimeScreen(navController, it.sharedScheduleViewModel(navController, container))
        }
    }
}


@Composable
fun ScheduleScreen(
    navController: NavController,
    viewModel: ScheduleViewModel = viewModel(),
) {
    val detail = viewModel.detail.value
    val id = viewModel.id

    val S = LocalStrings.current
    SimpleScaffold(navController, S.Schedule) {
        Section(title = S.ScheduleExecution) {
            SectionNavigation(title = S.Timing, body = detail.crontab.toUIString()) {
                navController.navigate("${Screen.Crontab}/${id}")
            }
            SectionNavigation(
                title = S.DayOfWeekLimit,
                body = detail.dayOfWeekWhitelist.toUIString()
            ) {
                navController.navigate("${Screen.DayOfWeek}/${id}")
            }
            SectionNavigation(title = S.DayLimit, body = detail.dayLimit.toUIString()) {
                navController.navigate("${Screen.DayLimit}/${id}")
            }
        }
        Section {
            SectionSlider(
                title = S.Priority,
                info = "任务在执行过程中，可被更高优先级任务抢占",
                value = detail.priority.toFloat(),
                range = 0f..100f
            ) { new ->
                viewModel.updateDetail {
//                    Log.d("TAG","new priority $new")
                    it.copy(priority = new.roundToInt())
                }
            }
        }

    }
}

