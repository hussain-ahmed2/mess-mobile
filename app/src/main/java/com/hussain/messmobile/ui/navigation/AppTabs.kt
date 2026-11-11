package com.hussain.messmobile.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hussain.messmobile.R
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.screens.announcements.AnnouncementsScreen
import com.hussain.messmobile.ui.screens.attendance.AttendanceScreen
import com.hussain.messmobile.ui.screens.backup.BackupScreen
import com.hussain.messmobile.ui.screens.dues.DuesScreen
import com.hussain.messmobile.ui.screens.expenses.ExpensesScreen
import com.hussain.messmobile.ui.screens.home.HomeScreen
import com.hussain.messmobile.ui.screens.members.MembersScreen
import com.hussain.messmobile.ui.screens.meals.MealsScreen
import com.hussain.messmobile.ui.screens.more.MoreScreen
import com.hussain.messmobile.ui.screens.shopping.ShoppingScreen
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.appColors

/** Every route of the app. */
object Routes {
    const val HOME = "home"
    const val MEALS = "meals"
    const val ATTENDANCE = "attendance"
    const val DUES = "dues"
    const val MORE = "more"
    const val MEMBERS = "members"
    const val EXPENSES = "expenses"
    const val SHOPPING = "shopping"
    const val ANNOUNCEMENTS = "announcements"
    const val BACKUP = "backup"
}

/** The tabs of the bottom navigation. */
enum class TabDestination(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    Home(Routes.HOME, "Home", R.drawable.ic_home),
    Meals(Routes.MEALS, "Meals", R.drawable.ic_meal),
    Attendance(Routes.ATTENDANCE, "Attendance", R.drawable.ic_attendance),
    Dues(Routes.DUES, "Dues", R.drawable.ic_dues),
    More(Routes.MORE, "More", R.drawable.ic_more),
}

private val bottomRoutes = TabDestination.entries.map { it.route }.toSet()

private fun titleOf(route: String?): String = when (route) {
    Routes.HOME -> "Mess manager"
    Routes.MEALS -> "Meals"
    Routes.ATTENDANCE -> "Attendance"
    Routes.DUES -> "Dues"
    Routes.MORE -> "More"
    Routes.MEMBERS -> "Members"
    Routes.EXPENSES -> "Expenses"
    Routes.SHOPPING -> "Shopping list"
    Routes.ANNOUNCEMENTS -> "Announcements"
    Routes.BACKUP -> "Backup"
    else -> "Mess manager"
}

/**
 * The app shell: a bottom navigation bar with the five tabs and the screens behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessMobileApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val colors = appColors

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    ThemedText(
                        text = titleOf(currentRoute),
                        style = AppTextStyle.AppBar,
                    )
                },
                navigationIcon = {
                    if (currentRoute != null && currentRoute !in bottomRoutes) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp),
                                tint = colors.text,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.text,
                    navigationIconContentColor = colors.text,
                ),
            )
        },
        bottomBar = {
            if (currentRoute in bottomRoutes) {
                NavigationBar(
                    containerColor = colors.background,
                    tonalElevation = 0.dp,
                ) {
                    TabDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Image(
                                    painter = painterResource(destination.iconRes),
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(
                                        if (selected) colors.text else colors.textSecondary,
                                    ),
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.text,
                                selectedTextColor = colors.text,
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary,
                                indicatorColor = colors.backgroundElement,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable(Routes.MEALS) {
                MealsScreen()
            }
            composable(Routes.ATTENDANCE) {
                AttendanceScreen()
            }
            composable(Routes.DUES) {
                DuesScreen()
            }
            composable(Routes.MORE) {
                MoreScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable(Routes.MEMBERS) {
                MembersScreen()
            }
            composable(Routes.EXPENSES) {
                ExpensesScreen()
            }
            composable(Routes.SHOPPING) {
                ShoppingScreen()
            }
            composable(Routes.ANNOUNCEMENTS) {
                AnnouncementsScreen()
            }
            composable(Routes.BACKUP) {
                BackupScreen()
            }
        }
    }
}
