package gamebot.host.presentation

import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

fun showThemeView(context: AppCompatActivity, content: @Composable () -> Unit) {
    context.runOnUiThread {
        context.setContent {
            ThemeView(context, content = content)
        }
    }
}

fun showCenterView(context: AppCompatActivity, content: @Composable () -> Unit) {
    showThemeView(context) {
        CenterView(content)
    }
}

fun showLoadingView(context: AppCompatActivity) {
    showCenterView(context) {
        CircularProgressIndicator()
    }
}

fun showErrorView(context: AppCompatActivity, head: String, body: String, restart: () -> Unit) {
    val url = "https://github.com/tkkcc/gamekeeper/issues"
    showCenterView(context) {
        val opener = LocalUriHandler.current

        Text(
            head, color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = {
                opener.openUri(url)
            }) {
                Text("check issues")
            }
            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = restart
            ) {
                Text("restart")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            body, color = MaterialTheme.colorScheme.onSurface
        )
    }
}