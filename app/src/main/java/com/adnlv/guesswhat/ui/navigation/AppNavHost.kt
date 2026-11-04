package com.adnlv.guesswhat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.adnlv.guesswhat.AppContainer
import com.adnlv.guesswhat.ui.categories.CategoriesScreen
import com.adnlv.guesswhat.ui.categories.CategoriesViewModel
import com.adnlv.guesswhat.ui.home.HomeScreen
import com.adnlv.guesswhat.ui.home.HomeViewModel

@Composable
fun AppNavHost(navController: NavHostController, appContainer: AppContainer, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = NavDestination.Home.route,
        modifier = modifier
    ) {
        composable(NavDestination.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.provideFactory(appContainer.newsRepository)
            )
            HomeScreen(viewModel = viewModel)
        }
        composable(NavDestination.Categories.route) {
            val viewModel: CategoriesViewModel = viewModel(
                factory = CategoriesViewModel.provideFactory(appContainer.newsRepository)
            )
            CategoriesScreen(viewModel = viewModel)
        }
    }
}
