package gamebot.host.presentation

import android.os.Build
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext


@Composable
fun ThemeView(activity: AppCompatActivity, maxSize:Boolean = true, content: @Composable () -> Unit) {
    // 1. good status and navigation bar color
    // 2. auto light/dark theme

    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(darkTheme) {
        // on android >= 8.0, navigation bar icon has dark color. we override navigation background to transparent.
        // on android <= 7.1, navigation bar icon always light, we use default strategy.
        val transparent = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 26) {
            val style = if (darkTheme) {
                SystemBarStyle.dark(transparent)
            } else {
                SystemBarStyle.light(transparent, transparent)
            }
            activity.enableEdgeToEdge(
                navigationBarStyle = style
            )
        } else {
            activity.enableEdgeToEdge()
        }
        onDispose {}
    }

    // Dynamic color is available on Android 12+
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }



    MaterialTheme(colorScheme = colors) {
        if (maxSize) {
            // wrap in surface to solve NavHost start flicker
            Surface(
                modifier = Modifier.fillMaxSize(),
                content = content
            )
        }else{
            content()
        }
    }
}