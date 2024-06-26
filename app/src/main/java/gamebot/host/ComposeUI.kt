//package gamebot.host

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import gamebot.host.Native
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json.Default.decodeFromString

val LocalComponentRoot = compositionLocalOf { Component.Root(Component.Empty) }

// TODO don't like enum, use trait

@Serializable
sealed class Component {
    @Serializable
    data class Root(val child: Component) : Component()

    @Serializable
    data object Empty : Component()

    @Serializable
    data class Column(val children: List<Component>) : Component()

    @Serializable
    data class TextField(val text: String) : Component()

    @Serializable
    data class Button(val content: Component, val callback: String) : Component()

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
                Column {
                    children.forEach {
                        it.Render()
                    }
                }
            }

            is TextField -> {
                var text by remember {
                    mutableStateOf(text)
                }
                TextField(text, { text = it })
            }

            else -> {
                throw Exception("unknown component: $this")
            }
        }
    }
}


class ComposeUI(val context: ComponentActivity) {
    fun render(layout: String) {
        val xx: Component = decodeFromString(layout)
        context.setContent {
            xx.Render()
        }
    }
}