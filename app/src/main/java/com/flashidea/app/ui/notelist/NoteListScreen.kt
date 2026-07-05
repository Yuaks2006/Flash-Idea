package com.flashidea.app.ui.notelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.ui.common.MetallicBrandMark
import com.flashidea.app.ui.common.QuietBackground
import com.flashidea.app.ui.common.QuietDivider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val gson = Gson()
private val listType = object : TypeToken<List<String>>() {}.type

private fun parseTags(json: String): List<String> =
    runCatching { gson.fromJson<List<String>>(json, listType) ?: emptyList() }
        .getOrDefault(emptyList())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onNavigateToNewNote: () -> Unit,
    onNavigateToNoteDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: NoteListViewModel = hiltViewModel()
) {
    val ideas by viewModel.ideas.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    var selectedCategory by rememberSaveable { mutableStateOf("全部") }
    val categories = remember(ideas) {
        listOf("全部") + ideas.map(IdeaEntity::category).filter(String::isNotBlank).distinct()
    }
    val visibleIdeas = remember(ideas, selectedCategory) {
        if (selectedCategory == "全部") ideas else ideas.filter { it.category == selectedCategory }
    }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    MetallicBrandMark()
                },
                actions = {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            Surface(
                modifier = Modifier
                    .size(62.dp)
                    .semantics { role = Role.Button }
                    .clickable(onClickLabel = "记录灵感", onClick = onNavigateToNewNote),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)),
                shadowElevation = 14.dp,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.20f),
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            )
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "记录灵感",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        QuietBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

            if (searchExpanded || query.isNotBlank()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("搜索") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )
            }

            if ((searchExpanded || selectedCategory != "全部") && categories.size > 1) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        AssistChip(
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            leadingIcon = if (selectedCategory == category) {
                                {
                                    Surface(
                                        modifier = Modifier.width(8.dp).height(8.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {}
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            when {
                visibleIdeas.isEmpty() && query.isNotBlank() -> EmptyState(
                    title = "无结果",
                    actionLabel = "清空搜索",
                    onAction = { viewModel.onSearchQueryChange("") }
                )

                visibleIdeas.isEmpty() -> EmptyState(
                    title = "",
                    actionLabel = "",
                    onAction = onNavigateToNewNote
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        top = 4.dp,
                        end = 18.dp,
                        bottom = 104.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(visibleIdeas, key = IdeaEntity::id) { idea ->
                        NoteCard(
                            idea = idea,
                            onClick = { onNavigateToNoteDetail(idea.id) }
                        )
                        QuietDivider()
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun OverviewCard(total: Int, processed: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column {
                Text(
                    "$total 条灵感",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "$processed 条已完成 AI 整理",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (title.isNotBlank()) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
            }
            if (actionLabel.isNotBlank()) {
                AssistChip(onClick = onAction, label = { Text(actionLabel) })
            }
        }
    }
}

@Composable
private fun NoteCard(idea: IdeaEntity, onClick: () -> Unit) {
    val tags = remember(idea.tags) { parseTags(idea.tags) }
    val dateText = remember(idea.createdAt) {
        SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()).format(Date(idea.createdAt))
    }
    val title = idea.summary.ifBlank {
        idea.content.trim().lineSequence().firstOrNull().orEmpty()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics { role = Role.Button }
            .clickable(onClickLabel = "打开笔记详情", onClick = onClick),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0f)
    ) {
        Column(Modifier.padding(horizontal = 2.dp, vertical = 15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    idea.category.ifBlank { "未分类" },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (idea.summary.isNotBlank() && idea.content != idea.summary) {
                Spacer(Modifier.height(4.dp))
                Text(
                    idea.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    tags.take(4).joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
