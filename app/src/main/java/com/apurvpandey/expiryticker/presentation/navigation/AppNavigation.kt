package com.apurvpandey.expiryticker.presentation.navigation

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
import com.apurvpandey.expiryticker.presentation.settings.SettingsRoute

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_EXPIRY = "add_expiry"
    const val EDIT_EXPIRY = "edit_expiry/{id}"
    const val DETAIL = "detail/{id}"
    const val SETTINGS = "settings"

    fun editExpiry(id: Long) = "edit_expiry/$id"
    fun detail(id: Long) = "detail/$id"
}

@Composable
fun ExpiryTickerApp(
    container: AppContainer,
    initialItemId: Long? = null,
    navController: NavHostController = rememberNavController()
) {
    LaunchedEffect(initialItemId) {
        if (initialItemId != null) {
            navController.navigate(Routes.detail(initialItemId))
        }
    }

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {

        composable(Routes.DASHBOARD) {
            DashboardRoute(
                container = container,
                onNavigateToAdd = { navController.navigate(Routes.ADD_EXPIRY) },
                onNavigateToDetail = { id -> navController.navigate(Routes.detail(id)) },
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

        composable(Routes.SETTINGS) {
            SettingsRoute(
                container = container,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
