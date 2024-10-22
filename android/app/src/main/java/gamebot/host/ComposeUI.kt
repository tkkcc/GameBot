//package gamebot.host

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kevinnzou.web.rememberWebViewState
import gamebot.host.d
import gamebot.host.presentation.component.SimpleNavHost
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed class NavHostEvent() {
    @Serializable
    @SerialName("Push")
    data class Push(val id: Int, val destination: String) : NavHostEvent()

    @Serializable
    @SerialName("Pop")
    data class Pop(val id: Int) : NavHostEvent()

    @Serializable
    @SerialName("None")
    data object None : NavHostEvent()
}

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
    @SerialName("Empty")
    data class Empty(val content: List<Component> = emptyList()) : Component {
        @Composable
        override fun Render() {

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

            val callback = LocalUIEvent.current

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
            val callback = LocalUIEvent.current
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

    @Serializable
    @SerialName("WebView")
    data class WebView(val url: String) : Component {
        @Composable
        override fun Render() {

            com.kevinnzou.web.WebView(rememberWebViewState(url))
        }
    }

    @Serializable
    @SerialName("NavHost")
    data class NavHost(
        val children: HashMap<String, Component>,
        val start: String,
        val oneTimeEvent: NavHostEvent
    ) : Component {
        @Composable
        override fun Render() {
            val navController = rememberNavController()

            LaunchedEffect(oneTimeEvent) {
                when (oneTimeEvent) {
                    is NavHostEvent.Push -> {
                        navController.navigate(oneTimeEvent.destination)
                    }
                    is NavHostEvent.Pop -> {
                        navController.popBackStack()
                    }
                    is NavHostEvent.None -> {

                    }
                }
            }
//            d(children)
//            d(start)
            SimpleNavHost(navController, startDestination = "Main") {
//                composable("Main") {
//                    Text("main")
//                }
                children.forEach { k, v ->
                    composable(k) {
                        v.Render()
                    }
                }
            }
        }
    }
}


//interface Callback {
//    fun onEvent(eventId: Int, data: Any)
//}

val LocalUIEvent = compositionLocalOf<(Int, CallbackValue) -> Unit> {
    { id, value ->
    }
}


@OptIn(ExperimentalSerializationApi::class)
fun initConfigUI(
    context: ComponentActivity,
    layout: MutableState<Component>,
    uiEventChannel: MutableState<Channel<UIEvent>>
) {
    context.setContent {
//        val coroutine = rememberCoroutineScope()
        CompositionLocalProvider(LocalUIEvent provides { id, value ->
//            coroutine.launch {
            uiEventChannel.value.trySend(UIEvent.Callback(id, value))
//            }
        }) {
            layout.value.Render()
        }
    }
}

@Serializable
sealed class UIEvent {

    @Serializable
    @SerialName("Empty")
    data object Empty : UIEvent()

    @Serializable
    @SerialName("Exit")
    data object Exit : UIEvent()

    @Serializable
    @SerialName("Callback")
    data class Callback(val id: Int, val value: CallbackValue) : UIEvent()
}

//@Serializable
//data class CallbackMsg(val id: Int, val value: CallbackValue)

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