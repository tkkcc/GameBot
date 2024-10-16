package gamebot.host.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SectionTextField(
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    singleLine: Boolean = true,
    shape: Shape = TextFieldDefaults.shape,
    textStyle: TextStyle = LocalTextStyle.current,
    onValueChange: (String) -> Unit,
) {

    val focus = LocalFocusManager.current
    var passwordVisible by remember {
        mutableStateOf(if (isPassword) false else true)
    }
    val containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)

    // set focus to end, otherwise it's at first

//    var fieldValue by remember {
//        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
//    }
//    LaunchedEffect(key1 = value.isNotEmpty()){
//        if (fieldValue.text.isEmpty()) {
//
//        }
//    }


    TextField(enabled = enabled,
        shape = shape,
        textStyle = textStyle,
        modifier = modifier
            .fillMaxWidth(),
//            .onPreviewKeyEvent {
////                if (it.key == Key.Enter){
////                    return@onPreviewKeyEvent false
////                }
//                // use tab and enter to navigate
//                if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
//                    val direction =
//                        if (it.isShiftPressed) FocusDirection.Up else FocusDirection.Down
//                    focus.moveFocus(direction)
//                    true
//                } else {
//                    false
//                }
//            },
        singleLine = singleLine,
        value = value,
        placeholder = {
//            Text(placeholder, color=LocalContentColor.current.copy(alpha = 0.5f))
            Text(
                placeholder, style = textStyle
            )
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction,
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text
        ),

        onValueChange = onValueChange,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            errorContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            Row {
                if (isPassword && value.isNotEmpty() && enabled) {
                    val image = if (passwordVisible) Icons.Default.Visibility
                    else Icons.Default.VisibilityOff

                    // Please provide localized description for accessibility services
                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                }

                if (value.isNotEmpty() && enabled) {
                    IconButton(onClick = {
                        onValueChange("")
                    }) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            }
        }

    )

}

@Preview
@Composable
fun SampleSectionTextField() {
    SectionTextField(placeholder = "12", value = "34", onValueChange = {

    })
}