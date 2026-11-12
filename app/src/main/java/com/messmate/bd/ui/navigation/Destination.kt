package com.messmate.bd.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.ui.graphics.vector.ImageVector
import com.messmate.bd.R

/**
 * Every destination of the app.
 */
sealed class Destination(val route: String) {

    // --- Onboarding -------------------------------------------------------
    data object Onboarding : Destination("onboarding")
    data object CreateMess : Destination("create-mess")
    data object JoinMess : Destination("join-mess")

    // --- Main tabs --------------------------------------------------------
    data object Home : Destination("home")
    data object Bazar : Destination("bazar")
    data object Utilities : Destination("utilities")
    data object Members : Destination("members")
    data object Statement : Destination("statement")

    // --- Secondary --------------------------------------------------------
    data object Audit : Destination("audit")
    data object Settings : Destination("settings")
}

/**
 * The entries of the bottom navigation bar.
 */
enum class BottomTab(
    val destination: Destination,
    val titleRes: Int,
    val icon: ImageVector,
) {
    HOME(Destination.Home, R.string.nav_home, Icons.Outlined.Home),
    BAZAR(Destination.Bazar, R.string.nav_bazar, Icons.Outlined.ShoppingBasket),
    UTILITIES(Destination.Utilities, R.string.nav_utilities, Icons.Outlined.Bolt),
    MEMBERS(Destination.Members, R.string.nav_members, Icons.Outlined.Groups),
    STATEMENT(Destination.Statement, R.string.nav_statement, Icons.Outlined.ReceiptLong),
}
