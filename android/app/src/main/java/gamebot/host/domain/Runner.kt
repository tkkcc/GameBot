package gamebot.host.domain

import Container
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import gamebot.host.RemoteRun
import gamebot.host.presentation.detail.DetailViewModel


interface Runner {

    fun runTask(rr: RemoteRun) {
        TODO()
    }


    @Composable
    fun ConfigScreen(
        parentNavController: NavController,
        realViewModel: DetailViewModel
    ) {
        TODO()
    }

    @Composable
    fun FloatScreen(container: Container) {
        TODO()
    }
}
