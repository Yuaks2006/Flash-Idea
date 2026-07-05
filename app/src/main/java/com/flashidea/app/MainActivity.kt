package com.flashidea.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flashidea.app.navigation.FlashIdeaNavGraph
import com.flashidea.app.navigation.Routes
import com.flashidea.app.ui.common.FloatingNavBar
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
        val currentRouteEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentRouteEntry?.destination?.route
        val showBottomBar = currentRoute in bottomNavRoutes

        LaunchedEffect(captureRequest?.id) {
            if (captureRequest != null) {
                navController.navigate(Routes.NEW_NOTE) {
                    popUpTo(Routes.NOTE_LIST) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }

        // 灵动岛布局：NavHost 占满底层，FloatingNavBar 作为 Box overlay 漂浮于内容上层
        Box(modifier = Modifier.fillMaxSize()) {
            FlashIdeaNavGraph(
                navController = navController,
                initialCaptureText = captureRequest?.initialText,
                onCaptureTextConsumed = onCaptureConsumed
            )

            // 内容区底部渐变遮罩，消除背景色阶跳变到悬浮导航栏的硬切
            AnimatedVisibility(
                visible = showBottomBar,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.65f)
                                )
                            )
                        )
                )
            }

            AnimatedVisibility(
                visible = showBottomBar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()           // 键盘弹出时整体上移至 IME 上方
            ) {
                FloatingNavBar(
                    currentRoute = currentRoute,
                    onRouteSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.NOTE_LIST) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}