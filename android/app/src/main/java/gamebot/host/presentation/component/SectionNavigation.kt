package gamebot.host.presentation.component

import androidx.compose.runtime.Composable

@Composable
fun SectionNavigation(
    title: String,
    info: String = "",
    body: String = "",
    onClick: () -> Unit,
) {
    NoRippleSectionRow(onClick) {
        SectionContent(title, info, body)
        Space()
        IconNext()
    }
}
