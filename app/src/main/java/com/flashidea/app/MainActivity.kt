package com.flashidea.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flashidea.app.navigation.FlashIdeaNavGraph
import com.flashidea.app.navigation.Routes
import com.flashidea.app.ui.theme.FlashIdeaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val captureRequest = mutableStateOf<CaptureRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        consumeIntent(intent)
        setContent {
            FlashIdeaContent(
                captureRequest = captureRequest.value,
                onCaptureConsumed = { captureRequest.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        val sharedText = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        } else {
            ""
        }
        if (intent?.action == ACTION_NEW_NOTE || sharedText.isNotBlank()) {
            captureRequest.value = CaptureRequest(
                id = System.nanoTime(),
                initialText = sharedText
            )
        }
    }

    companion object {
        const val ACTION_NEW_NOTE = "com.flashidea.app.ACTION_NEW_NOTE"
    }
}

private data class CaptureRequest(val id: Long, val initialText: String)

@Composable
private fun FlashIdeaContent(
    captureRequest: CaptureRequest?,
    onCaptureConsumed: () -> Unit
) {
    FlashIdeaTheme {
        val navController = rememberNavController()
        val bottomNavRoutes = listOf(Routes.NOTE_LIST, Routes.GRAPH, Routes.AI_CHAT)
        val currentRoute by navController.currentBackStackEntryAsState()
        val showBottomBar = currentRoute?.destination?.route in bottomNavRoutes

        LaunchedEffect(captureRequest?.id) {
            if (captureRequest != null) {
                navController.navigate(Routes.NEW_NOTE) {
                    popUpTo(Routes.NOTE_LIST) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar) BottomNavBar(navController)
            }
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                FlashIdeaNavGraph(
                    navController = navController,
                    initialCaptureText = captureRequest?.initialText,
                    onCaptureTextConsumed = onCaptureConsumed
                )
            }
        }
    }
}

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
private fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("笔记", Icons.AutoMirrored.Filled.List,   Routes.NOTE_LIST),
        BottomNavItem("图谱", Icons.Default.AccountTree,         Routes.GRAPH),
        BottomNavItem("AI",   Icons.AutoMirrored.Filled.Chat,   Routes.AI_CHAT),
    )
    val currentRoute by navController.currentBackStackEntryAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(260.dp)
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)),
            shadowElevation = 10.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute?.destination?.route == item.route
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .semantics { role = Role.Button }
                            .clickable(onClickLabel = item.label) {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.NOTE_LIST) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(if (selected) 42.dp else 34.dp),
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            border = if (selected) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
                            } else {
                                null
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
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
