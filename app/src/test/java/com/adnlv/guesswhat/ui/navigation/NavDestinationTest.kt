package com.adnlv.guesswhat.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavDestinationTest {
    @Test
    fun navDestination_containsExpectedTabs() {
        val destinations = NavDestination.entries
        assertEquals(2, destinations.size)
        assertEquals("home", destinations[0].route)
        assertEquals("categories", destinations[1].route)
    }
}
