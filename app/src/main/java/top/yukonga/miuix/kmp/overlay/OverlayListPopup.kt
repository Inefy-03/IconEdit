package top.yukonga.miuix.kmp.overlay

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.LocalIsDarkTheme

@Composable
fun OverlayListPopup(
    show: Boolean,
    alignment: Any = PopupPositionProvider.Align.Start,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    if (show) {
        val positionProvider = if (alignment is androidx.compose.ui.window.PopupPositionProvider) {
            alignment
        } else {
            object : androidx.compose.ui.window.PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: androidx.compose.ui.unit.IntRect,
                    windowSize: androidx.compose.ui.unit.IntSize,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    popupContentSize: androidx.compose.ui.unit.IntSize
                ): androidx.compose.ui.unit.IntOffset {
                    // 首页右上角和设置主题点击弹窗的位置（不遮住按钮本身，窗口右边与按钮右侧对齐，上边缘在按钮下边）
                    val x = anchorBounds.right - popupContentSize.width
                    val y = anchorBounds.bottom
                    
                    val safeX = x.coerceIn(4, windowSize.width - popupContentSize.width - 4)
                    val safeY = y.coerceIn(4, windowSize.height - popupContentSize.height - 4)
                    return androidx.compose.ui.unit.IntOffset(safeX, safeY)
                }
            }
        }
        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            val isDark = LocalIsDarkTheme.current
            val backgroundColor = if (isDark) Color(0xFF242424) else Color(0xFFFFFFFF)
            top.yukonga.miuix.kmp.basic.Surface(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = backgroundColor
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    content()
                }
            }
        }
    }
}
