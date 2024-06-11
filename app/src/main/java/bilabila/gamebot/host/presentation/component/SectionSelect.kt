package bilabila.gamebot.host.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bilabila.gamebot.host.presentation.ToUIString.toUIString

@Composable
fun <T> SectionSelect(
    title: String? = null,
    info: String? = null,
    body: T,
    selection: List<T>,
    onChange: (T) -> Unit,
) {
    var dropdownExpanded by remember {
        mutableStateOf(false)
    }
    NoRippleSectionRow({ dropdownExpanded = true }) {
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = {
                dropdownExpanded = false
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            selection.forEach {
                DropdownMenuItem(text = {
                    CenterRow {
                        val selected = it == body
                        val color by animateColorAsState(targetValue = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        Text(
                            it.toUIString(),
                            color = color
                        )
                        Space()
                        AnimatedVisibility(selected) {
                            Icon(
                                Icons.Default.Done,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = "done", // TODO
                            )
                        }
                    }

                }, onClick = {
                    onChange(it)
                    dropdownExpanded = false
                })
            }
        }
        SectionContent(title, info, body.toUIString())
    }
}

@Preview
@Composable
fun SampleSectionSelect() {

    SectionSelect(
        title = "title",
        body = "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
        selection = listOf(
            "开启", "关闭"
        )
    ) {

    }
}