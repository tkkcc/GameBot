package bilabila.gamebot.host.presentation.component

import androidx.compose.runtime.Composable

@Composable
fun SectionNavigation(
    title: String,
    info: String? = null,
    body: String? = null,
    onClick: () -> Unit,
) {
    NoRippleSectionRow(onClick) {
        SectionContent(title, info, body)
        Space()
        IconNext()
    }
}
