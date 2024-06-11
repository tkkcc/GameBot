package bilabila.gamebot.host.presentation.main

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import bilabila.gamebot.host.presentation.LocalStrings
import bilabila.gamebot.host.presentation.component.Section
import bilabila.gamebot.host.presentation.component.SectionSwitch
import bilabila.gamebot.host.presentation.component.SectionTextField
import bilabila.gamebot.host.presentation.component.SimpleScaffold

@Composable
fun DebugScreen(
    navController: NavController,
    viewModel: DebugViewModel = viewModel()
) {

//    Surface(color = Color.White) {
//        Column {

            DebugScreen(
                navController = navController,
                state = viewModel.state.value,
                update = viewModel::update,
                applyDisplay = viewModel::applyDisplay
            )
//        }
//
//    }
//    return
}


@Composable
fun DebugScreen(
    navController: NavController,
    state: DebugState,
    update: ((DebugState) -> DebugState) -> Unit,
    applyDisplay: () -> Unit,
) {
    val S = LocalStrings.current
    SimpleScaffold(navController, S.Debug){
        Section(title = S.modifyDisplay) {
            // get current display
            SectionTextField(value = state.displayWidth, onValueChange = { new ->
                update {
                    it.copy(
                        displayWidth = new
                    )
                }
            })
            SectionTextField(value = state.displayHeight.toString(), onValueChange = { new ->
                update {
                    it.copy(
                        displayHeight = new
                    )
                }
            })
            SectionTextField(value = state.displayDensity.toString(), onValueChange = { new ->
                update {
                    it.copy(
                        displayDensity = new
                    )
                }
            })
            Button(onClick = applyDisplay) {
                Text(text = LocalStrings.current.apply)
            }
        }
        Section(title = LocalStrings.current.enableDebugServer) {
            SectionSwitch(
                title = LocalStrings.current.enableDebugServer,
                body = "已关闭",
                checked = state.startDebugServer,
                onChange = {new->
                    update {
                        state.copy(startDebugServer = new)
                    }
                }
            )
        }
    }
}