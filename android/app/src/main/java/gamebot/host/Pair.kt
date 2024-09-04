import android.view.accessibility.AccessibilityNodeInfo
import java.nio.ByteBuffer


data class ScreenNode(
    val first: ByteArray,
    val second: Array<AccessibilityNodeInfo>
)

data class Screenshot(
    val width: Int,
    val height: Int,
    val data: ByteBuffer,
    val pixelStride: Int,
    val rowStride: Int,
    val rotation: Int,
//    var timestamp: Long
)