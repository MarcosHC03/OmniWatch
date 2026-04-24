package com.watchlist.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
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
import com.watchlist.app.ui.discovery.DiscoveryScreen
import com.watchlist.app.ui.addprintmedia.AddPrintMediaScreen
import com.watchlist.app.ui.printdetails.PrintDetailsScreen
import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Discovery : Screen("discovery")
    object MyList : Screen("my_list")
    object Calendar : Screen("calendar")
    object AddMedia : Screen("add_media?itemId={itemId}&cacheId={cacheId}&query={query}") {
        fun createRoute(itemId: Long = -1L, cacheId: Int = -1, query: String = "") =
            "add_media?itemId=$itemId&cacheId=$cacheId&query=${Uri.encode(query)}"
    }
    object AddPrintMedia : Screen("add_print_media?itemId={itemId}") {
        fun createRoute(itemId: Long = -1L) = "add_print_media?itemId=$itemId"
    }
    object PrintDetails : Screen("print_details/{franchiseId}") {
        fun createRoute(franchiseId: Long) = "print_details/$franchiseId"
    }
}

sealed class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(Screen.Home, "Inicio", Icons.Filled.Home)
    object Discovery : BottomNavItem(Screen.Discovery, "Descubrí", Icons.Filled.Search)
    object MyList : BottomNavItem(Screen.MyList, "Mi lista", Icons.AutoMirrored.Filled.List)
    object Calendar : BottomNavItem(Screen.Calendar, "Calendario", Icons.Filled.DateRange)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Discovery,
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
        composable(Screen.Discovery.route) {
            DiscoveryScreen(navController = navController)
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
                },
                navArgument("cacheId") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
            val cacheId = backStackEntry.arguments?.getInt("cacheId") ?: -1
            val query  = backStackEntry.arguments?.getString("query") ?: ""
            AddMediaScreen(
                itemId = itemId,
                cacheId = cacheId,
                autoSearchQuery = query,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddPrintMedia.route,
            arguments = listOf(
                navArgument("itemId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
            AddPrintMediaScreen(
                itemId = itemId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PrintDetails.route,
            arguments = listOf(
                navArgument("franchiseId") { type = NavType.LongType }
            )
        ) {
            PrintDetailsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
