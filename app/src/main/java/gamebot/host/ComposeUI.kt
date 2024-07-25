//package gamebot.host

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
sealed interface Component {
    @Composable
    fun Render()

    @Serializable
    @SerialName("Column")
    data class Column(val content: List<Component> = emptyList()) : Component {
        @Composable
        override fun Render() {
            Column(
//            modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                content.forEach {
                    it.Render()
                }
            }
        }
    }

    @Serializable
    @SerialName("Row")
    data class Row(val content: List<Component> = emptyList()) : Component {
        @Composable
        override fun Render() {
            Row(
//            modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                content.forEach {
                    it.Render()
                }
            }
        }
    }

    @Serializable
    @SerialName("TextField")
    data class TextField(val content: String, val callbackId: Int = 0) : Component {
        @Composable
        override fun Render() {
            // we do not allow mutating text on callback side if focused
            // as it async updating value cause racing
            // related: https://medium.com/androiddevelopers/effective-state-management-for-textfield-in-compose-d6e5b070fbe5

            val callback = LocalCallback.current

            var remoteText by remember(content) { mutableStateOf(content) }
            var localText by remember { mutableStateOf(content) }
            var isFocused by remember { mutableStateOf(false) }

            TextField(if (isFocused) localText else remoteText, {
                localText = it
                remoteText = it
                callback(callbackId, CallbackValue.String(it))
            }, modifier = Modifier.onFocusChanged {
                localText = remoteText
                isFocused = it.isFocused
            })
        }
    }

    @Serializable
    @SerialName("Button")
    data class Button(val content: Component, val callbackId: Int = 0) : Component {
        @Composable
        override fun Render() {
            val callback = LocalCallback.current
            Button(onClick = {
                callback(callbackId, CallbackValue.Unit)
            }) {
                content.Render()
            }
        }
    }

    @Serializable
    @SerialName("Text")
    data class Text(val content: String) : Component {
        @Composable
        override fun Render() {
            androidx.compose.material3.Text(content)
        }
    }

}


//interface Callback {
//    fun onEvent(eventId: Int, data: Any)
//}

val LocalCallback = compositionLocalOf<(Int, CallbackValue) -> Unit> {
    { id, value ->
    }
}


@OptIn(ExperimentalSerializationApi::class)
fun initConfigUI(
    context: ComponentActivity,
    layout: MutableState<Component>,
    callbackChannel: Channel<CallbackMsg>
) {

    val test: Component = Component.Column(
        listOf(Component.Button(Component.Text(content = "abc"), 0))
    )
    val a = Json.encodeToString(test)
    Log.d("", "initConfigUI: ${a}")
    context.setContent {

        val coroutine = rememberCoroutineScope()
//        var xx: Component by remember {
//            mutableStateOf(Component.TextField("abc"))
//        }
//        val a = ByteArray(10)
//        val b: Component = Json.decodeFromStream(a.inputStream())
//        LaunchedEffect(true) {
////                delay(3000)
//            val x = Component.Column(
//                listOf(Component.TextField("aaa"))
//            )
//
//            xx = decodeFromString(encodeToString(Component.serializer(), x))
//        }
        CompositionLocalProvider(LocalCallback provides { id, value ->
            coroutine.launch {
                callbackChannel.send(CallbackMsg(id, value))
//                        val x = Component.Column(
//                            (0..1000).map {
//                                if (it == 0) {
//                                    Component.TextField(data as String)
//                                } else {
//                                    Component.TextField(it.toString())
//                                }
////                                Component.TextField(it as String)
//                            }.toList()
//                        )
//                        val y = encodeToString(Component.serializer(), x)
//                        delay(3)
//                        val z: Component = decodeFromString(y)
//
//                        xx = z
            }
        }

        ) {
//            Text("abc")
            layout.value.Render()
//            test.Render()
        }
    }
}

@Serializable
data class CallbackMsg(val id: Int, val value: CallbackValue)

@Serializable
sealed interface CallbackValue {
    @Serializable
    @SerialName("string")
    data class String(val value: kotlin.String) : CallbackValue

    @Serializable
    @SerialName("bool")
    data class Bool(val value: kotlin.Boolean) : CallbackValue

    @Serializable
    @SerialName("isize")
    data class Int(val value: kotlin.Int) : CallbackValue

    @Serializable
    @SerialName("usize")
    data class UInt(val value: kotlin.UInt) : CallbackValue

    @Serializable
    @SerialName("f64")
    data class Float(val value: kotlin.Double) : CallbackValue

    @Serializable
    @SerialName("unit")
    data object Unit : CallbackValue
}