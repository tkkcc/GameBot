package bilabila.gamebot.host.presentation.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun DetailScreen(
    navController: NavController,
    viewModel: DetailViewModel = viewModel()
) {
    Surface(modifier= Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = viewModel.ready.value,
            enter = fadeIn(spring(stiffness = Spring.StiffnessHigh)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)),
        ) {

//            if (visible) {
                viewModel.configScreen(navController, viewModel)
//        SimpleScaffold(navController = navController, "name") {
//            Section("日常"){
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//                SectionNavigation("标题","官服官服"){}
//            }
//        }
//            } else {
//                CenterScaffold(navController = navController) {
//                    CircularProgressIndicator()
//                }
//            }
//        }
//        AnimatedContent(targetState = viewModel.ready.value) { visible->
        }
    }
    return
//    viewModel.View(navController)

}