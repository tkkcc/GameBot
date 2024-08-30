// from https://github.com/only52607/compose-floating-window

package gamebot.host
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
//import com.github.only52607.compose.window.ComposeFloatingWindow
//import com.github.only52607.compose.window.LocalFloatingWindow
import kotlinx.coroutines.launch

val LocalFloatingWindow: ProvidableCompositionLocal<ComposeFloatingWindow> = compositionLocalOf {
    noLocalProvidedFor("LocalFloatingWindow")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}

class ComposeFloatingWindow(
    val context: Context
) : SavedStateRegistryOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        SavedStateViewModelFactory(
            context.applicationContext as Application,
            this@ComposeFloatingWindow,
            null
        )
    }

    override val defaultViewModelCreationExtras: CreationExtras = MutableCreationExtras().apply {
        val application = context.applicationContext?.takeIf { it is Application }
        if (application != null) {
            set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, application as Application)
        }
        set(SAVED_STATE_REGISTRY_OWNER_KEY, this@ComposeFloatingWindow)
        set(VIEW_MODEL_STORE_OWNER_KEY, this@ComposeFloatingWindow)
    }

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private var savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    var showing = false
    var decorView: ViewGroup = FrameLayout(context)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val windowParams: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        height = WindowManager.LayoutParams.WRAP_CONTENT
        width = WindowManager.LayoutParams.WRAP_CONTENT
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.CENTER
        windowAnimations = android.R.style.Animation_Dialog
        flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                )
        if (context !is Activity) {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        setContentView(ComposeView(context).apply {
            setContent {
                CompositionLocalProvider(
                    LocalFloatingWindow provides this@ComposeFloatingWindow
                ) {
                    content()
                }
            }
            setViewTreeLifecycleOwner(this@ComposeFloatingWindow)
            setViewTreeViewModelStoreOwner(this@ComposeFloatingWindow)
            setViewTreeSavedStateRegistryOwner(this@ComposeFloatingWindow)
        })
    }

    fun setContentView(view: View) {
        if (decorView.childCount > 0) {
            decorView.removeAllViews()
        }
        decorView.addView(view)
        update()
    }

    fun show() {
        require(decorView.childCount != 0) {
            "Content view cannot be empty"
        }
        if (showing) {
            update()
            return
        }
        decorView.getChildAt(0)?.takeIf { it is ComposeView }?.let { composeView ->
            val reComposer = Recomposer(AndroidUiDispatcher.CurrentThread)
            composeView.compositionContext = reComposer
            lifecycleScope.launch(AndroidUiDispatcher.CurrentThread) {
                reComposer.runRecomposeAndApplyChanges()
            }
        }
        if (decorView.parent != null) {
            windowManager.removeViewImmediate(decorView)
        }
        windowManager.addView(decorView, windowParams)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        showing = true
    }

    fun update() {
        if (!showing) return
        windowManager.updateViewLayout(decorView, windowParams)
    }

    fun hide() {
        if (!showing) {
            return
        }
        showing = false
        windowManager.removeViewImmediate(decorView)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        enableSavedStateHandles()
    }
}


fun Modifier.dragFloatingWindow(): Modifier = composed {
    val floatingWindow = LocalFloatingWindow.current
    val windowParams = remember { floatingWindow.windowParams }
    this.then(pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            val w = floatingWindow.decorView.width
            val h = floatingWindow.decorView.height
//            Log.e("", "$w x $h, $windowParams")
            val f = Rect().also { floatingWindow.decorView.getWindowVisibleDisplayFrame(it) }
            windowParams.x =
                (windowParams.x + dragAmount.x.toInt()).coerceIn(0..(f.width() - w))
            windowParams.y =
                (windowParams.y + dragAmount.y.toInt()).coerceIn(0..(f.height() - h))
            floatingWindow.update()
        }
    })
}