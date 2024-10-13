package gamebot.host.presentation

import Container
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gamebot.host.ILocalRun
import gamebot.host.presentation.component.SimpleNavHost
import gamebot.host.presentation.detail.DetailScreen
import gamebot.host.presentation.detail.DetailViewModel
import gamebot.host.presentation.main.DebugScreen
import gamebot.host.presentation.main.DebugViewModel
import gamebot.host.presentation.main.MainScreen
import gamebot.host.presentation.main.MainViewModel
import gamebot.host.presentation.schedule.scheduleGraph


@Composable
fun NavigationView(
    container: Container,
) {
    val navController = rememberNavController()

//    SimpleNavHost(
//        navController = navController, startDestination = Screen.Main.toString()
//    ) {
//        composable(Screen.Main.toString()) {
//            MainScreen(
//                navController,
////                viewModel(factory = MainViewModel.factory(container))
//            )
//        }
//        composable(Screen.Debug.toString()) {
//            DebugScreen(
//                navController, viewModel(factory = DebugViewModel.factory(container))
//            )
//        }
//        composable(
//            Screen.Detail.toString() + "/{id}",
//            arguments = listOf(navArgument(name = "id") { type = NavType.LongType })
//        ) {
//            DetailScreen(
//                navController, viewModel(factory = DetailViewModel.factory(container))
//            )
//        }
////        composable(
////            Screen.Schedule.toString() + "/{id}",
////            arguments = listOf(navArgument(name = "id") { type = NavType.LongType })
////        ) {
//////            ScheduleScreen(
//////                navController, viewModel(factory = ScheduleViewModel.factory(container))
//////            )
////            DebugScreen(
////                navController, viewModel(factory = DebugViewModel.factory(container))
////            )
////        }
//
//        scheduleGraph(navController, container)
//
//
//    }
}
