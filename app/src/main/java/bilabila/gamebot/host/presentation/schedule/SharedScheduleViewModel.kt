package bilabila.gamebot.host.presentation.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import Container

// https://www.youtube.com/watch?v=h61Wqy3qcKg
@Composable
fun NavBackStackEntry.sharedScheduleViewModel(
    navController: NavController,
    container: Container
): ScheduleViewModel {
    val navGraphRoute = destination.parent?.route ?: throw Exception()
    val parentEntry  = remember(this){
        navController.getBackStackEntry(navGraphRoute)
    }
    return viewModel(parentEntry, factory = ScheduleViewModel.factory(container))
}