package com.flashidea.app.ui.graph

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.CheckCircle

/**
 * 按 category 映射节点装饰图标。
 * 缺省 → Lightbulb（灵感），常见类别：项目火种/任务/待办 等。
 */
fun categoryIcon(category: String): ImageVector = when {
    category.contains("项目") || category.contains("火种") -> Icons.Default.Work
    category.contains("任务") || category.contains("待办") -> Icons.Default.CheckCircle
    else -> Icons.Default.Lightbulb
}

/** 命中检测放宽半径（节点半径外的额外容差） */
const val NODE_HIT_RADIUS_DP = 12