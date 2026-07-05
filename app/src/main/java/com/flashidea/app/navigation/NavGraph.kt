package com.flashidea.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.flashidea.app.ui.aichat.AiChatScreen
import com.flashidea.app.ui.graph.GraphScreen
import com.flashidea.app.ui.insightfeed.InsightFeedScreen
import com.flashidea.app.ui.notedetail.NoteDetailScreen
import com.flashidea.app.ui.notelist.NoteListScreen
import com.flashidea.app.ui.newnote.NewNoteScreen
import com.flashidea.app.ui.settings.SettingsScreen

object Routes {
    const val NOTE_LIST    = "note_list"
    const val NEW_NOTE     = "new_note"
    const val NOTE_DETAIL  = "note_detail/{noteId}"
    const val GRAPH        = "graph"
    const val INSIGHT_FEED = "insight_feed"
    const val AI_CHAT      = "ai_chat"
    const val SETTINGS     = "settings"

    fun noteDetail(noteId: String) = "note_detail/$noteId"
}

@Composable
fun FlashIdeaNavGraph(
    navController: NavHostController,
    initialCaptureText: String? = null,
    onCaptureTextConsumed: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = Routes.NOTE_LIST) {

        composable(Routes.NOTE_LIST) {
            NoteListScreen(
                onNavigateToNewNote    = { navController.navigate(Routes.NEW_NOTE) },
                onNavigateToNoteDetail = { id -> navController.navigate(Routes.noteDetail(id)) },
                onNavigateToSettings   = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.NEW_NOTE) {
            NewNoteScreen(
                onNavigateBack         = { navController.popBackStack() },
                onNavigateToNoteDetail = { id -> navController.navigate(Routes.noteDetail(id)) { popUpTo(Routes.NEW_NOTE) { inclusive = true } } },
                initialContent = initialCaptureText,
                onInitialContentConsumed = onCaptureTextConsumed
            )
        }

        composable(
            route = Routes.NOTE_DETAIL,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStack ->
            NoteDetailScreen(
                noteId         = backStack.arguments?.getString("noteId") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GRAPH) {
            GraphScreen(
                onNavigateToNoteDetail  = { id -> navController.navigate(Routes.noteDetail(id)) },
                onNavigateToInsightFeed = { navController.navigate(Routes.INSIGHT_FEED) }
            )
        }

        composable(Routes.INSIGHT_FEED) {
            InsightFeedScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.AI_CHAT) {
            AiChatScreen()
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
