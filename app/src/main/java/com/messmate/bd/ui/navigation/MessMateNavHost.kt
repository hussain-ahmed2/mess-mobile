package com.messmate.bd.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.messmate.bd.ui.audit.AuditScreen
import com.messmate.bd.ui.bazar.BazarScreen
import com.messmate.bd.ui.components.LoadingBox
import com.messmate.bd.ui.home.HomeScreen
import com.messmate.bd.ui.join.CreateMessScreen
import com.messmate.bd.ui.join.JoinMessScreen
import com.messmate.bd.ui.members.MembersScreen
import com.messmate.bd.ui.onboarding.OnboardingScreen
import com.messmate.bd.ui.root.RootViewModel
import com.messmate.bd.ui.settings.SettingsScreen
import com.messmate.bd.ui.statement.StatementScreen
import com.messmate.bd.ui.utilities.UtilitiesScreen

/**
 * The navigation graph of the app.
 *
 * The start destination depends on the state of the account: without a mess the
 * user goes through the onboarding, otherwise straight to the dashboard.
 */
@Composable
fun MessMateNavHost(modifier: Modifier = Modifier) {
    val rootViewModel: RootViewModel = hiltViewModel()
    val rootState by rootViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            rootState.isLoading -> LoadingBox()

            !rootState.hasMess -> OnboardingGraph(navController)

            else -> MainGraph(navController)
        }
    }
}

/**
 * Onboarding: sign in, create a mess, join a mess.
 */
@Composable
private fun OnboardingGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Destination.Onboarding.route) {
        composable(Destination.Onboarding.route) {
            OnboardingScreen(
                onCreateMess = { navController.navigate(Destination.CreateMess.route) },
                onJoinMess = { navController.navigate(Destination.JoinMess.route) },
            )
        }
        composable(Destination.CreateMess.route) {
            CreateMessScreen(onBack = { navController.popBackStack() })
        }
        composable(Destination.JoinMess.route) {
            JoinMessScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * The main app with the bottom navigation bar.
 */
@Composable
private fun MainGraph(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // The bottom bar is hidden on the secondary screens (audit, settings).
    val showBottomBar = BottomTab.entries.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.destination.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomTab.entries.forEach { tab ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == tab.destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.destination.route) {
                                    // Keep a single instance of every tab.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.titleRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Home.route) { HomeScreen() }
            composable(Destination.Bazar.route) { BazarScreen() }
            composable(Destination.Utilities.route) { UtilitiesScreen() }
            composable(Destination.Members.route) { MembersScreen() }
            composable(Destination.Statement.route) { StatementScreen() }
            composable(Destination.Audit.route) { AuditScreen() }
            composable(Destination.Settings.route) {
                SettingsScreen(onOpenAudit = { navController.navigate(Destination.Audit.route) })
            }
        }
    }
}
