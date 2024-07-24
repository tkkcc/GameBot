//package gamebot.host

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

val LocalComponentRoot = compositionLocalOf { Component.Root(Component.Unknown) }

// TODO don't like enum, use trait

@Serializable
sealed class Component {
    @Serializable
    data class Root(val child: Component) : Component()

    @Serializable
    data object Unknown : Component()

    @Serializable
    data class Column(var children: List<Component>) : Component()

    @Serializable
    data class TextField(val text: String, val id: Int = 0) : Component()

    @Serializable
    data class Button(val content: Component, val callback: String) : Component()

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render() {
        when (this) {
            is Root -> {
                CompositionLocalProvider(LocalComponentRoot provides this) {
                    child.Render()
                }
            }

            is Button -> {
                val layout = LocalComponentRoot.current.toString()
                Button(onClick = {
                    Log.e("", layout)
                    // TODO callback from local to remote to jni
//                    Native.callback(callback)
                }) {
                    content.Render()
                }
            }

            is Column -> {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    children.forEach {
                        it.Render()
                    }
                }
            }

            is TextField -> {

                // we do not allow mutating text on callback side if focused
                // as it async updating value cause racing
                // related: https://medium.com/androiddevelopers/effective-state-management-for-textfield-in-compose-d6e5b070fbe5

                val callback = LocalCallback.current

                var text by remember(text) { mutableStateOf(text) }
                var localText by remember { mutableStateOf(text) }
                var isFocused by remember { mutableStateOf(false) }

                TextField(if (isFocused) localText else text, {
                    localText = it
                    text = it
                    callback.onEvent(0, it)
                }, modifier = Modifier.onFocusChanged {
                    isFocused = it.isFocused
                })
            }
            is Unknown -> {

            }

            else -> {
                throw Exception("unknown component: $this")
            }
        }
    }
}


interface Callback {
    fun onEvent(eventId: Int, data: Any)
}

val LocalCallback = compositionLocalOf<Callback> {
    object : Callback {
        override fun onEvent(eventId: Int, data: Any) {
            // Handle default behavior here, like logging a warning that no callback is provided
            println("Warning: Callback not provided, event $eventId with data $data ignored.")
        }
    }
}
val state = mutableStateOf(0)


@OptIn(ExperimentalSerializationApi::class)
fun initConfigUI(context: ComponentActivity, layout: MutableState<Component>) {

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
        CompositionLocalProvider(LocalCallback provides object : Callback {
            override fun onEvent(eventId: Int, data: Any) {
                coroutine.launch {
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
        }) {
            layout.value.Render()
        }
    }
}
