package gamebot.host.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt

@Composable
fun SectionSlider(
    title: String? = null,
    info: String? = null,
    value: Float = 0f,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    var cachedValue by remember(value) {
        mutableStateOf(value)
    }
    val body = cachedValue.roundToInt().toString()
    SectionRow {
        Column(Modifier.weight(1f)) {
            ContentTitle(title)
            ContentInfo(info)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContentBody(text = body, modifier = Modifier.weight(.2f))
                Slider(
                    modifier = Modifier.weight(.8f),
                    value = cachedValue, valueRange = range,
                    onValueChange = {
                        cachedValue = it
                    },
                    onValueChangeFinished = {
                        onChange(cachedValue)
                    }
                )
            }
        }
    }
}