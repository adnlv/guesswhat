package com.adnlv.guesswhat.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.adnlv.guesswhat.ui.theme.GuessWhatTheme

@Composable
fun NewsBottomBar(
    currentRoute: String?,
    onNavigateToDestination: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavDestination.entries.forEach { destination ->
            val isSelected = currentRoute == destination.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.contentDescriptionRes)
                    )
                },
                label = {
                    Text(text = stringResource(destination.labelRes))
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NewsBottomBarPreview() {
    GuessWhatTheme {
        NewsBottomBar(
            currentRoute = NavDestination.Home.route,
            onNavigateToDestination = {}
        )
    }
}
