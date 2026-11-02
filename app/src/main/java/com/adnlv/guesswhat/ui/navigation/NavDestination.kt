package com.adnlv.guesswhat.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.vector.ImageVector
import com.adnlv.guesswhat.R

enum class NavDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    @StringRes val contentDescriptionRes: Int
) {
    Home(
        route = "home",
        labelRes = R.string.nav_home,
        icon = Icons.Default.Home,
        contentDescriptionRes = R.string.nav_home
    ),
    Categories(
        route = "categories",
        labelRes = R.string.nav_categories,
        icon = Icons.Default.Menu,
        contentDescriptionRes = R.string.nav_categories
    )
}
