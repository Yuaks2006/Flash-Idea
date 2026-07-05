package com.flashidea.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 双按钮确认弹窗，全项目复用。
 *
 * 用于删除/危险操作前的二次确认。遵循 [QuietUi] 配色与圆角风格。
 */
@Composable
fun ConfirmDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "取消"
) {
    if (!show) return
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    )
}

/**
 * 三选一弹窗：取消 / 不保存 / 保存。
 *
 * 用于"未保存退出"场景：
 * - [onNeutral]      "取消"动作（仅关闭弹窗，留在当前页）
 * - [onNegative]     "不保存"动作（直接退出，丢弃草稿）
 * - [onAffirmative]  "保存"动作（先保存再退出）
 *
 * Material3 [AlertDialog] 原生仅 confirm + dismiss 两个按钮槽，
 * 故将"取消"与"不保存"合并至 dismissButton 行内。
 */
@Composable
fun TripleActionDialog(
    show: Boolean,
    title: String,
    message: String,
    neutralText: String,
    negativeText: String,
    affirmativeText: String,
    onNeutral: () -> Unit,
    onNegative: () -> Unit,
    onAffirmative: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!show) return
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onAffirmative) {
                Text(affirmativeText, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onNeutral) { Text(neutralText) }
                TextButton(onClick = onNegative) { Text(negativeText) }
            }
        }
    )
}