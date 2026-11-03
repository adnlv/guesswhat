package com.adnlv.guesswhat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.adnlv.guesswhat.AppContainer
import com.adnlv.guesswhat.NewsApp
import com.adnlv.guesswhat.ui.categories.CategoriesScreen
import com.adnlv.guesswhat.ui.categories.CategoriesViewModel
import com.adnlv.guesswhat.ui.home.HomeScreen
import com.adnlv.guesswhat.ui.home.HomeViewModel

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier, appContainer: AppContainer? = null) {
    NavHost(
        navController = navController,
        startDestination = NavDestination.Home.route,
        modifier = modifier
    ) {
        composable(NavDestination.Home.route) {
            val context = LocalContext.current
            val container = appContainer ?: (context.applicationContext as? NewsApp)?.container
            val viewModel: HomeViewModel? = container?.let {
                viewModel(factory = HomeViewModel.provideFactory(it.newsRepository))
            }
            HomeScreen(viewModel = viewModel)
        }
        composable(NavDestination.Categories.route) {
            val context = LocalContext.current
            val container = appContainer ?: (context.applicationContext as? NewsApp)?.container
            val viewModel: CategoriesViewModel? = container?.let {
                viewModel(factory = CategoriesViewModel.provideFactory(it.newsRepository))
            }
            CategoriesScreen(viewModel = viewModel)
        }
    }
}
