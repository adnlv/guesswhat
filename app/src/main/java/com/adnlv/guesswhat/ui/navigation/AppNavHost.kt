package com.adnlv.guesswhat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.adnlv.guesswhat.ui.categories.CategoriesScreen
import com.adnlv.guesswhat.ui.home.HomeScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = NavDestination.Home.route,
        modifier = modifier
    ) {
        composable(NavDestination.Home.route) {
            HomeScreen()
        }
        composable(NavDestination.Categories.route) {
            CategoriesScreen()
        }
    }
}
