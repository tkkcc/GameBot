package gamebot.host.presentation.component

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gamebot.host.DynamicTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// we need to disable whole block easily, like egui's enable/disable
// https://stackoverflow.com/questions/69142209/jetpack-compose-how-to-disable-gesture-detection-on-children
fun Modifier.gesturesDisabled(disabled: Boolean = true) =
    if (disabled) {
        pointerInput(Unit) {
            awaitPointerEventScope {
                // we should wait for all new pointer events
                while (true) {
                    awaitPointerEvent(pass = PointerEventPass.Initial)
                        .changes
                        .forEach(PointerInputChange::consume)
                }
            }
        }.alpha(.5f)
    } else {
        this
    }

// we need to disable ripple effect to avoid navigation transition lag:
// https://issuetracker.google.com/issues/301488789
// https://stackoverflow.com/questions/66703448/how-to-disable-ripple-effect-when-clicking-in-jetpack-compose#:~:text=To%20disable%20the%20ripple%20effect,indication%20property%20of%20the%20modifier.&text=You%20can%20handle%20it%20this%20way%20when%20working%20with%20Buttons.&text=In%20case%20of%20a%20button,onClick%20%3D%20%7B%20%2F*...
object NoRippleInteractionSource : MutableInteractionSource {
    override val interactions: Flow<Interaction> = emptyFlow()
    override suspend fun emit(interaction: Interaction) {}
    override fun tryEmit(interaction: Interaction) = true
}

@Composable
fun CenterRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
fun SectionRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {

    val lighterBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick
            )
//                .clickable(
//
//                ) {
//                    onClick()
//                }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
//    }

}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.S)
@Preview
@Composable
fun T() {
    DynamicTheme {

        Scaffold(
            topBar = { TopAppBar(title = { Text("abc") }) }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {

                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                        .compositeOver(Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
//                    tonalElevation = -1.dp
                ) {
                    SectionRow {
                        Text("o11k")
                    }
                }

                DropdownMenu(expanded = true, onDismissRequest = {}) {
                    DropdownMenuItem(text = { Text("o11k") }, {})
                    DropdownMenuItem(text = { Text("o11k") }, {})
                    DropdownMenuItem(text = { Text("o11k") }, {})
                    DropdownMenuItem(text = { Text("o11k") }, {})
                }
            }
        }
    }
}

@Composable
fun NoRippleSectionRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = NoRippleInteractionSource
            ) {
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}


@Composable
fun NoRippleTextButton(

    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {

    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

}


@Composable
fun NoRippleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = NoRippleInteractionSource,
        content = content,
    )
}

@Composable
fun NoRippleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = NoRippleInteractionSource,
        content = content
    )
}


@Composable
fun SectionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun ColumnScope.Space() {
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun RowScope.Space() {
    Spacer(modifier = Modifier.width(16.dp))
}

@Composable
fun Enable(enable: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.gesturesDisabled(!enable), content = content
    )
}

@Composable
fun SectionTitle(text: String?, modifier: Modifier = Modifier) {
    text?.let {
        Text(
            text,
            color = LocalContentColor.current.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
    }
}

@Composable
fun RowScope.SectionContent(
    title: String? = null,
    info: String? = null,
    body: String? = null,
) {
    Column(modifier = Modifier.weight(1f)) {
        ContentTitle(title)
        ContentInfo(info)
        ContentBody(body)
    }
}

@Composable
fun ContentTitle(text: String?, modifier: Modifier = Modifier) {
    text?.let {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
    }
}

@Composable
fun ContentInfo(text: String?, modifier: Modifier = Modifier) {
    text?.let {
        Text(
            text, style = MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = 0.6f),
            modifier = modifier
        )
    }
}


@Composable
fun ContentBody(text: String?, modifier: Modifier = Modifier) {
    text?.let {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    }
}

@Composable
fun IconNext() {
    Icon(
        Icons.AutoMirrored.Default.KeyboardArrowRight,
        tint = LocalContentColor.current.copy(alpha = 0.6f),
        contentDescription = "next", // TODO i18n
    )
}


@Composable
fun FastAnimatedVisibility(visible: Boolean) {
    AnimatedVisibility(visible = visible) {

    }


}

@Composable
fun <S> SimpleAnimatedContent(
    targetState: S,
    content: @Composable() AnimatedContentScope.(targetState: S) -> Unit
) {
    AnimatedContent(targetState, label = "animated", transitionSpec = {
        fadeIn(spring(stiffness = Spring.StiffnessMedium)).togetherWith(
            fadeOut(spring(stiffness = Spring.StiffnessMedium))
        )
    }, content = content)

}