package bilabila.gamebot.host.presentation.detail

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import bilabila.gamebot.host.domain.Runner
import bilabila.gamebot.host.presentation.component.CenterScaffold


@OptIn(ExperimentalMaterial3Api::class)
class LoadingRunner : Runner {

    @Composable
    override fun ConfigScreen(
        parentNavController: NavController,
        realViewModel: DetailViewModel
    ) {
        CenterScaffold(navController = parentNavController) {
            CircularProgressIndicator()
        }
    }
}