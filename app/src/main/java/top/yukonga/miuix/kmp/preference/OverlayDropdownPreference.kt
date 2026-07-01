package top.yukonga.miuix.kmp.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.basic.DropdownEntry

@Composable
fun OverlayDropdownPreference(
    title: String,
    items: List<DropdownEntry<*>>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val isDark = top.yukonga.miuix.kmp.theme.LocalIsDarkTheme.current
    val backgroundColor = if (isDark) Color(0xFF242424) else Color(0xFFFFFFFF)

    top.yukonga.miuix.kmp.basic.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { showDialog = true },
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface
            )
            Box(contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = items.getOrNull(selectedIndex)?.text ?: "",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                OverlayListPopup(
                    show = showDialog,
                    alignment = PopupPositionProvider.Align.TopEnd,
                    onDismissRequest = { showDialog = false }
                ) {
                    ListPopupColumn {
                        items.forEachIndexed { index, entry ->
                            DropdownImpl(
                                text = entry.text,
                                optionSize = items.size,
                                isSelected = selectedIndex == index,
                                index = index,
                                onSelectedIndexChange = {
                                    onSelectedIndexChange(index)
                                    showDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
