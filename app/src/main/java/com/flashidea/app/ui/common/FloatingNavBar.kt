package com.flashidea.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.flashidea.app.navigation.Routes

/**
 * 悬浮胶囊底部导航条 — 灵动岛式 Box overlay。
 * 处于内容上层、毛玻璃半透明，由调用方挂 [Modifier.align] 控制位置。
 */
data class FloatingNavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun FloatingNavBar(
    currentRoute: String?,
    onRouteSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        FloatingNavItem("笔记", Icons.AutoMirrored.Filled.List, Routes.NOTE_LIST),
        FloatingNavItem("图谱", Icons.Default.AccountTree, Routes.GRAPH),
        FloatingNavItem("AI", Icons.AutoMirrored.Filled.Chat, Routes.AI_CHAT),
    )

    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(260.dp)
                .height(64.dp)
                .blur(radius = 20.dp),
            shape = RoundedCornerShape(32.dp),
            color = surfaceContainerHigh.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, outlineVariant.copy(alpha = 0.9f)),
            shadowElevation = 12.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                surfaceContainerHigh,
                                primary.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .semantics { role = Role.Button }
                            .clickable(onClickLabel = item.label) { onRouteSelected(item.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(if (selected) 42.dp else 34.dp),
                            shape = CircleShape,
                            color = if (selected) primaryContainer else Color.Transparent,
                            border = if (selected) {
                                BorderStroke(1.dp, primary.copy(alpha = 0.24f))
                            } else {
                                null
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) onPrimaryContainer else onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}