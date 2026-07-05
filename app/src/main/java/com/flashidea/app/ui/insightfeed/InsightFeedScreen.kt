package com.flashidea.app.ui.insightfeed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashidea.app.data.local.InsightEntity
import com.flashidea.app.ui.common.QuietBackground
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightFeedScreen(
    onNavigateBack: () -> Unit,
    viewModel: InsightFeedViewModel = hiltViewModel()
) {
    val insights by viewModel.insights.collectAsState()

    LaunchedEffect(Unit) { viewModel.markAllRead() }

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f)
                )
            )
        }
    ) { padding ->
        QuietBackground {
            if (insights.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(
                        "暂无洞察",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp)) {
                    items(insights) { insight -> InsightCard(insight); Spacer(Modifier.height(10.dp)) }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(insight: InsightEntity) {
    val typeLabel = when (insight.type) {
        "hidden_link"  -> "隐性关联"
        "growth"       -> "成长轨迹"
        "action_plan"  -> "行动计划"
        else           -> "AI 洞察"
    }
    val typeColor = when (insight.type) {
        "hidden_link" -> MaterialTheme.colorScheme.tertiary
        "growth"      -> MaterialTheme.colorScheme.secondary
        else          -> MaterialTheme.colorScheme.primary
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(insight.createdAt)),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(typeLabel, color = typeColor, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text(insight.content, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
