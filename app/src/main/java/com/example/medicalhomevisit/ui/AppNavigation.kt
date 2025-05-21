package com.example.medicalhomevisit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.medicalhomevisit.ui.protocol.ProtocolScreen
import com.example.medicalhomevisit.ui.protocol.ProtocolViewModel
import com.example.medicalhomevisit.ui.protocol.ProtocolViewModelFactory
import com.example.medicalhomevisit.ui.visitdetail.VisitDetailScreen
import com.example.medicalhomevisit.ui.visitdetail.VisitDetailViewModel
import com.example.medicalhomevisit.ui.visitdetail.VisitDetailViewModelFactory
import com.example.medicalhomevisit.ui.visitlist.VisitListScreen
import com.example.medicalhomevisit.ui.visitlist.VisitListViewModel
import com.example.medicalhomevisit.ui.visitlist.VisitListViewModelFactory

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.VisitList.route
    ) {
        composable(Screen.VisitList.route) {
            val viewModel = remember { VisitListViewModelFactory().create(VisitListViewModel::class.java) }
            VisitListScreen(
                viewModel = viewModel,
                onVisitClick = { visit ->
                    navController.navigate(Screen.VisitDetail.createRoute(visit.id))
                }
            )
        }

        composable(
            route = Screen.VisitDetail.route,
            arguments = listOf(
                navArgument(Screen.VisitDetail.ARG_VISIT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val visitId = backStackEntry.arguments?.getString(Screen.VisitDetail.ARG_VISIT_ID)
                ?: throw IllegalArgumentException("Не указан ID визита")

            val viewModel = remember {
                VisitDetailViewModelFactory(visitId).create(VisitDetailViewModel::class.java)
            }

            VisitDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProtocol = { id ->
                    navController.navigate(Screen.Protocol.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.Protocol.route,
            arguments = listOf(
                navArgument(Screen.Protocol.ARG_VISIT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val visitId = backStackEntry.arguments?.getString(Screen.Protocol.ARG_VISIT_ID)
                ?: throw IllegalArgumentException("Не указан ID визита")

            val viewModel = remember {
                ProtocolViewModelFactory(visitId).create(ProtocolViewModel::class.java)
            }

            ProtocolScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object VisitList : Screen("visitList")

    object VisitDetail : Screen("visitDetail/{visitId}") {
        const val ARG_VISIT_ID = "visitId"

        fun createRoute(visitId: String): String {
            return "visitDetail/$visitId"
        }
    }

    object Protocol : Screen("protocol/{visitId}") {
        const val ARG_VISIT_ID = "visitId"

        fun createRoute(visitId: String): String {
            return "protocol/$visitId"
        }
    }
}