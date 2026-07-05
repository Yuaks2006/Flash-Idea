package com.flashidea.app.ui.newnote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashidea.app.BuildConfig
import com.flashidea.app.ui.common.QuietBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewNoteScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNoteDetail: (String) -> Unit,
    initialContent: String? = null,
    onInitialContentConsumed: () -> Unit = {},
    viewModel: NewNoteViewModel = hiltViewModel()
) {
    val content by viewModel.content.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val savedNoteId by viewModel.savedNoteId.collectAsState()
    val message by viewModel.message.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialContent) {
        if (initialContent != null) {
            viewModel.applyInitialContent(initialContent)
            onInitialContentConsumed()
        }
        focusRequester.requestFocus()
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f)
                )
            )
        }
    ) { innerPadding ->
        QuietBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .padding(horizontal = 18.dp)
            ) {
            OutlinedTextField(
                value = content,
                onValueChange = viewModel::onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("记录") },
                supportingText = {
                    Text("${content.length} 字")
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = MaterialTheme.shapes.extraLarge
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.save(onNavigateBack) },
                    enabled = content.isNotBlank() && !isProcessing,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("保存")
                }
                Button(
                    onClick = viewModel::sendToAi,
                    enabled = content.isNotBlank() && !isProcessing,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.size(6.dp))
                    Text(if (isProcessing) "整理中" else "整理")
                }
            }
            }
        }
    }

    aiResult?.let { result ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissAiResult,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            AiResultContent(
                result = result,
                onViewDetail = {
                    savedNoteId?.let(onNavigateToNoteDetail)
                }
            )
        }
    }
}

@Composable
private fun AiResultContent(
    result: AiProcessResult,
    onViewDetail: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "整理完成",
            style = MaterialTheme.typography.headlineSmall
        )
        if (result.category.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    result.category,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Text(
            result.summary.ifBlank { "已保存" },
            style = MaterialTheme.typography.bodyLarge
        )
        if (result.tags.isNotEmpty()) {
            Text(
                result.tags.joinToString("  ") { "#$it" },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Button(
            onClick = onViewDetail,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("查看")
        }
    }
}
