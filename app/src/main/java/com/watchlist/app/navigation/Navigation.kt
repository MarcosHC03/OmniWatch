package com.watchlist.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.watchlist.app.ui.addmedia.AddMediaScreen
import com.watchlist.app.ui.home.HomeScreen
import com.watchlist.app.ui.mylist.MyListScreen
import com.watchlist.app.ui.calendar.CalendarScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object MyList : Screen("my_list")
    object Calendar : Screen("calendar")
    object AddMedia : Screen("add_media?itemId={itemId}") {
        fun createRoute(itemId: Long = -1L) = "add_media?itemId=$itemId"
    }
}

sealed class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(Screen.Home, "Inicio", Icons.Filled.Home)
    object MyList : BottomNavItem(Screen.MyList, "Mi lista", Icons.AutoMirrored.Filled.List)
    object Calendar : BottomNavItem(Screen.Calendar, "Calendario", Icons.Filled.DateRange)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.MyList,
    BottomNavItem.Calendar
)

@Composable
fun WatchListNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.MyList.route) {
            MyListScreen(navController = navController)
        }
        composable(Screen.Calendar.route) {
            CalendarScreen(navController = navController)
        }
        composable(
            route = Screen.AddMedia.route,
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
            AddMediaScreen(
                itemId = itemId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
