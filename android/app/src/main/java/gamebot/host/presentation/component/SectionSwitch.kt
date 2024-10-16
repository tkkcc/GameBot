package gamebot.host.presentation.component

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

@Composable
fun SectionSwitch(
    title: String = "",
    info: String = "",
    body: String = "",
    checked: Boolean = false,
    onChange: (Boolean) -> Unit,
) {
    SectionRow({ onChange(!checked) }) {
        SectionContent(title, info, body)
        Space()
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.scale(.8f))
    }
}