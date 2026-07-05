package com.flashidea.app.ui.notedetail

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashidea.app.ui.theme.*
import com.flashidea.app.ui.common.QuietBackground
import com.flashidea.app.ui.common.QuietDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    onNavigateBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val idea by viewModel.idea.collectAsState()
    val relatedIdeas by viewModel.relatedIdeas.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        idea?.let { note ->
                            val text = buildString {
                                if (note.summary.isNotEmpty()) append("# ${note.summary}\n\n")
                                append(note.content)
                                if (note.category.isNotEmpty()) append("\n\n分类：${note.category}")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "导出笔记"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "导出")
                    }
                    TextButton(onClick = { viewModel.delete { onNavigateBack() } }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f)
                )
            )
        }
    ) { padding ->
        QuietBackground {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
            idea?.let { note ->
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (note.category.isNotEmpty() || note.summary.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    QuietDivider(Modifier.padding(horizontal = 0.dp))
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (note.category.isNotEmpty()) {
                            Text(note.category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            }
                            if (note.summary.isNotEmpty()) {
                            Text(note.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (relatedIdeas.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    QuietDivider(Modifier.padding(horizontal = 0.dp))
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            relatedIdeas.forEach { related ->
                                Text(
                                    related.content.take(56) + if (related.content.length > 56) "..." else "",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                    }
                }
            } ?: run {
                Text("不存在", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            }
        }
    }
}
