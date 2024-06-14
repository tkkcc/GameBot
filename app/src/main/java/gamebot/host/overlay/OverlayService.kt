// from https://github.com/xrubioj/JetpackComposeOverlayTest/

package gamebot.host.overlay
import MyLifecycleOwner
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
//import com.xrubio.overlaytest.overlay.MyLifecycleOwner


// https://www.jetpackcompose.app/snippets/OverlayService
class OverlayService : Service() {

    private val windowManager get() = getSystemService(WINDOW_SERVICE) as WindowManager

    override fun onCreate() {
        super.onCreate()
//        setTheme(R.style.Theme_OverlayTest)
        showOverlay()
    }

    private fun showOverlay() {
        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            // https://developer.android.com/reference/android/view/WindowManager.LayoutParams
            // alt: WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            // WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                        or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM

            ,
            PixelFormat.TRANSLUCENT
        )

        val composeView = ComposeView(this)
        composeView.setContent {
            Overlay(onClick = {
                Log.w("OverlayService", "*** Logging something from the overlay service")
                Toast.makeText(applicationContext, "Hey!", Toast.LENGTH_SHORT).show()
            })
        }

        // Trick The ComposeView into thinking we are tracking lifecycle
        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore
                get() = ViewModelStore()
        }
        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        // This is required or otherwise the UI will not recompose
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager.addView(composeView, params)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Overlay(onClick: () -> Unit) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize()
            ) {
                var showHello by remember { mutableStateOf(true) }
                if (showHello) {
                    Text(text = "Hello from Compose")
                }
                Button(onClick = {
                    onClick()
                    showHello = false
                }) {
                    Text("Tap me for a little surprise!")

                }
                var text by remember {
                    mutableStateOf("aaa")
                }
                TextField(text, {
                    text=it
                })
            }
        }
    }
}

@Preview
@Composable
private fun OverlayPreview() {
    Overlay(onClick = {})
}