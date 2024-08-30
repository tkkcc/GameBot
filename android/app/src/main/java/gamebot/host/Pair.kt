import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.Keep


data class ScreenNode(
    val first: ByteArray,
    val second: Array<AccessibilityNodeInfo>
)