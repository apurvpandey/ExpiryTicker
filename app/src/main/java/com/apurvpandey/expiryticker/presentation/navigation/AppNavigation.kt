package com.apurvpandey.expiryticker.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.apurvpandey.expiryticker.AppContainer
import com.apurvpandey.expiryticker.presentation.addedit.AddEditRoute
import com.apurvpandey.expiryticker.presentation.dashboard.DashboardRoute
import com.apurvpandey.expiryticker.presentation.details.ExpiryDetailRoute
import com.apurvpandey.expiryticker.presentation.insights.InsightsRoute
import com.apurvpandey.expiryticker.presentation.settings.SettingsRoute

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_EXPIRY = "add_expiry"
    const val EDIT_EXPIRY = "edit_expiry/{id}"
    const val DETAIL = "detail/{id}"
    const val SETTINGS = "settings"
    const val INSIGHTS = "insights"

    fun editExpiry(id: Long) = "edit_expiry/$id"
    fun detail(id: Long) = "detail/$id"
}

/** An external request to open a screen (notification, widget). [nonce] makes repeats distinct. */
data class LaunchRequest(val itemId: Long?, val openAdd: Boolean, val nonce: Long = System.nanoTime()) {
    companion object {
        fun from(intent: android.content.Intent?): LaunchRequest? {
            intent ?: return null
            val id = intent.getLongExtra(com.apurvpandey.expiryticker.MainActivity.EXTRA_ITEM_ID, -1L)
                .takeIf { it > 0 }
            val add = intent.getBooleanExtra(com.apurvpandey.expiryticker.MainActivity.EXTRA_OPEN_ADD, false)
            return if (id != null || add) LaunchRequest(id, add) else null
        }
    }
}

@Composable
fun ExpiryTickerApp(
    container: AppContainer,
    launchRequest: LaunchRequest? = null,
    navController: NavHostController = rememberNavController()
) {
    LaunchedEffect(launchRequest) {
        when {
            launchRequest == null -> Unit
            launchRequest.itemId != null -> navController.navigate(Routes.detail(launchRequest.itemId))
            launchRequest.openAdd -> navController.navigate(Routes.ADD_EXPIRY)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
        }
    ) {

        composable(Routes.DASHBOARD) {
            DashboardRoute(
                container = container,
                onNavigateToAdd = { navController.navigate(Routes.ADD_EXPIRY) },
                onNavigateToDetail = { id -> navController.navigate(Routes.detail(id)) },
                onNavigateToInsights = { navController.navigate(Routes.INSIGHTS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ADD_EXPIRY) {
            AddEditRoute(
                container = container,
                editItemId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_EXPIRY,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val id = backStack.arguments?.getLong("id")
            AddEditRoute(
                container = container,
                editItemId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val id = backStack.arguments?.getLong("id") ?: return@composable
            ExpiryDetailRoute(
                container = container,
                itemId = id,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { itemId -> navController.navigate(Routes.editExpiry(itemId)) }
            )
        }

        composable(Routes.INSIGHTS) {
            InsightsRoute(container = container, onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsRoute(
                container = container,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
