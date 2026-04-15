package com.example.agenttest.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.agenttest.ui.detail.NoteDetailScreen
import com.example.agenttest.ui.home.HomeScreen
import com.example.agenttest.ui.view.NoteViewScreen
import com.example.agenttest.ui.viewmodel.NoteViewModel
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class NoteDetailRoute(val id: String? = null)

@Serializable
data class NoteViewRoute(val id: String)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val viewModel: NoteViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(400)
            ) + fadeIn(animationSpec = tween(400))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(400)
            ) + fadeOut(animationSpec = tween(400))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(400)
            ) + fadeIn(animationSpec = tween(400))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(400)
            ) + fadeOut(animationSpec = tween(400))
        }
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNoteClick = { id -> navController.navigate(NoteViewRoute(id)) },
                onAddNoteClick = { navController.navigate(NoteDetailRoute()) },
                viewModel = viewModel
            )
        }
        composable<NoteViewRoute> { backStackEntry ->
            val route: NoteViewRoute = backStackEntry.toRoute()
            NoteViewScreen(
                noteId = route.id,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate(NoteDetailRoute(id)) },
                viewModel = viewModel
            )
        }
        composable<NoteDetailRoute> { backStackEntry ->
            val route: NoteDetailRoute = backStackEntry.toRoute()
            NoteDetailScreen(
                noteId = route.id,
                onBackClick = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}