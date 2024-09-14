import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class Rect(
    val left: UInt,
    val top: UInt,
    val right: UInt,
    val bottom: UInt,
)


@Serializable
data class NodeInfo(
    val id: String = "",
    val region: Rect = Rect(0u, 0u, 0u, 0u),
    val text: String = "",
    @SerialName("class") val className: String = "",
    @SerialName("package") val packageName: String = "",
    val description: String = "",
    val checkable: Boolean = false,
    val clickable: Boolean = false,
    @SerialName("long_clickable") val longClickable: Boolean = false,
    val focusable: Boolean = false,
    val scrollable: Boolean = false,
    val visible: Boolean = false,
    val checked: Boolean = false,
    val enabled: Boolean = false,
    val focused: Boolean = false,
    val selected: Boolean = false,
    val parent: Int = 0,
    val children: MutableList<Int> = mutableListOf(),
    val index: Int = 0
) {
    companion object {
        fun from(node: AccessibilityNodeInfo): NodeInfo {
            val nodeInfo = NodeInfo(
                id = node.viewIdResourceName ?: "",
                className = (node.viewIdResourceName ?: "").toString(),
                packageName = (node.viewIdResourceName ?: "").toString(),
                description = (node.viewIdResourceName ?: "").toString(),
                text = (node.viewIdResourceName ?: "").toString(),
//                text = (node.text ?: "").toString()
            )
            if (false && nodeInfo.text.contains("Gallery")) {
                node.performAction(ACTION_CLICK)
                val x = ACTION_CLICK
            }
            return nodeInfo
        }

    }

    fun index(index: Int): NodeInfo {
        return this.copy(index = index)
    }
}

data class ScreenNode(
    val data: ByteBuffer,
    val data_raw: Array<NodeInfo>,
    val reference: Array<AccessibilityNodeInfo>
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