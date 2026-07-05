package com.flashidea.app.ui.aichat

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin

/**
 * Markdown 渲染 Composable，用于 AI assistant 回复。
 *
 * 基于 [Markwon] 4.6.2 的 `CorePlugin`：
 * - 覆盖标题（# / ## / ###）、列表（- / *）、粗体 `**x**`、斜体、引用 `>`、
 *   行内代码 `` `x` ``、代码块 ``` ``` ``` 、分割线、链接等。
 * - 链接可点击（[LinkMovementMethod]）。
 * - Markwon 实例通过 [remember] 在首次组合时创建一次，避免每帧重建。
 *
 * 流式场景：当 [markdown] 动态变化时，[AndroidView] 的 `update` 块会重新调用
 * `markwon.setMarkdown(...)`，因此可直接接入流式累加（见批次3）。
 *
 * 注：语法高亮（`markwon-syntax-highlight` + `prism4j`）依赖已在 `app/build.gradle.kts`
 * 中引入，供后续批次接入；当前批次先不做语法着色以降低构建风险（决策见任务分解文档 1.2-3）。
 * 代码块横向滚动 + 复制按钮同样留待后续打磨（决策 1.2-6 允许）。
 *
 * @param markdown 待渲染的 markdown 文本
 * @param modifier 末位 modifier，便于父级布局覆盖
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setTextColor(textColor)
                textSize = 14f
                setLineSpacing(0f, 1.18f)
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        }
    )
}