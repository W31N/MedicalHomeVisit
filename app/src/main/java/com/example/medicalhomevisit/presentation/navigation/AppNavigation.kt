package com.example.medicalhomevisit.presentation.navigation

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.medicalhomevisit.domain.model.UserRole
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.example.medicalhomevisit.presentation.ui.profile.ProfileScreen
import com.example.medicalhomevisit.presentation.ui.admin.AdminDashboardScreen
import com.example.medicalhomevisit.presentation.ui.admin.AssignRequestScreen
import com.example.medicalhomevisit.presentation.ui.admin.ManageRequestsScreen
import com.example.medicalhomevisit.presentation.ui.admin.RegisterPatientScreen
import com.example.medicalhomevisit.presentation.ui.auth.LoginScreen
import com.example.medicalhomevisit.presentation.ui.auth.SignUpScreen
import com.example.medicalhomevisit.presentation.ui.patient.CreateRequestScreen
import com.example.medicalhomevisit.presentation.ui.patient.PatientProfileScreen
import com.example.medicalhomevisit.presentation.ui.patient.PatientRequestsScreen
import com.example.medicalhomevisit.presentation.ui.patient.RequestDetailsScreen
import com.example.medicalhomevisit.presentation.ui.protocol.ProtocolScreen
import com.example.medicalhomevisit.presentation.ui.visitdetail.VisitDetailScreen
import com.example.medicalhomevisit.presentation.ui.visitlist.VisitListScreen
import com.example.medicalhomevisit.presentation.viewmodel.AdminViewModel
import com.example.medicalhomevisit.presentation.viewmodel.AuthUiState
import com.example.medicalhomevisit.presentation.viewmodel.AuthViewModel
import com.example.medicalhomevisit.presentation.viewmodel.PatientProfileViewModel
import com.example.medicalhomevisit.presentation.viewmodel.PatientUiState
import com.example.medicalhomevisit.presentation.viewmodel.PatientViewModel
import com.example.medicalhomevisit.presentation.viewmodel.ProtocolViewModel
import com.example.medicalhomevisit.presentation.viewmodel.VisitDetailViewModel
import com.example.medicalhomevisit.presentation.viewmodel.VisitListViewModel

object PatientNavGraph {
    const val route = "patient_graph_route"
}

object AdminNavGraph {
    const val route = "admin_graph_route"
}

object MedicalStaffNavGraph {
    const val route = "medical_staff_graph_route"
}

sealed class Screen(val route: String) {
    object PatientProfile : Screen("patientProfile")
    object SplashScreen : Screen("splash_screen")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object VisitList : Screen("visitList")
    object Profile : Screen("profile")

    object PatientHome : Screen("patientHome")
    object CreateRequest : Screen("createRequest")
    object RequestDetails : Screen("requestDetails/{requestId}") {
        const val ARG_REQUEST_ID = "requestId"
        fun createRoute(requestId: String) = "requestDetails/$requestId"
    }

    object VisitDetail : Screen("visitDetail/{visitId}") {
        const val ARG_VISIT_ID = "visitId"
        fun createRoute(visitId: String) = "visitDetail/$visitId"
    }

    object Protocol : Screen("protocol/{visitId}") {
        const val ARG_VISIT_ID = "visitId"
        fun createRoute(visitId: String) = "protocol/$visitId"
    }
    object AdminDashboard : Screen("adminDashboard")
    object ManageRequests : Screen("manageRequests")
    object AssignRequest : Screen("assignRequest/{requestId}") {
        const val ARG_REQUEST_ID = "requestId"
        fun createRoute(requestId: String) = "assignRequest/$requestId"
    }
    object RegisterPatient : Screen("registerPatient")
}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    LaunchedEffect(authState, navController) {
        if (authState is AuthUiState.NotLoggedIn && navController.currentDestination?.route != Screen.Login.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route
    ) {
        composable(Screen.SplashScreen.route) {
            SplashScreen()
            Log.d("AppNavigation", "SplashScreen composable - Current authState: $authState")
            LaunchedEffect(authState) {
                Log.d("AppNavigation", "SplashScreen LaunchedEffect triggered with authState: $authState")

                val currentState = authState

                val destination: String? = when (currentState) {
                    is AuthUiState.LoggedIn -> {
                        Log.d("AppNavigation", "User logged in with role: ${currentState.user.role}")
                        when (currentState.user.role) {
                            UserRole.PATIENT -> {
                                Log.d("AppNavigation", "Routing to PatientNavGraph")
                                PatientNavGraph.route
                            }
                            UserRole.ADMIN -> {
                                Log.d("AppNavigation", "Routing to AdminNavGraph for role: ${currentState.user.role}")
                                AdminNavGraph.route
                            }
                            UserRole.MEDICAL_STAFF -> {
                                Log.d("AppNavigation", "Routing to VisitList")
                                MedicalStaffNavGraph.route
                            }
                        }
                    }
                    is AuthUiState.NotLoggedIn, is AuthUiState.Error -> {
                        Log.d("AppNavigation", "User not logged in or error state, routing to Login")
                        Screen.Login.route
                    }
                    is AuthUiState.RegistrationSuccessful -> {
                        Log.d("AppNavigation", "Registration successful for role: ${currentState.user.role}")
                        when (currentState.user.role) {
                            UserRole.PATIENT -> PatientNavGraph.route
                            UserRole.ADMIN -> AdminNavGraph.route
                            UserRole.MEDICAL_STAFF -> MedicalStaffNavGraph.route
                        }
                    }
                    AuthUiState.PasswordResetSent -> {
                        Log.d("AppNavigation", "Password reset sent, routing to Login")
                        Screen.Login.route
                    }

                    is AuthUiState.Initial, is AuthUiState.Loading -> {
                        Log.d("AppNavigation", "Auth state is initial or loading, waiting...")
                        null
                    }
                }
                destination?.let {
                    Log.d("AppNavigation", "===== NAVIGATING FROM SPLASH =====")
                    Log.d("AppNavigation", "Destination: $it")

                    navController.navigate(it) {
                        popUpTo(Screen.SplashScreen.route) { inclusive = true }
                        launchSingleTop = true
                    }
                    Log.d("AppNavigation", "Navigation call completed")
                }
            }
        }

        composable(Screen.Login.route) {
            Log.d("AppNavigation", "Login screen composable")
            LoginScreen(
                viewModel = authViewModel,
                onAuthSuccessful = {
                    Log.d("AppNavigation", "Login successful callback triggered")

                    val currentAuthState = authViewModel.uiState.value
                    if (currentAuthState is AuthUiState.LoggedIn) {
                        val destination = when (currentAuthState.user.role) {
                            UserRole.PATIENT -> {
                                Log.d("AppNavigation", "Navigating to PatientNavGraph after login")
                                PatientNavGraph.route
                            }
                            UserRole.ADMIN -> {
                                Log.d("AppNavigation", "Navigating to AdminNavGraph after login")
                                AdminNavGraph.route
                            }
                            UserRole.MEDICAL_STAFF -> {
                                Log.d("AppNavigation", "Navigating to VisitList after login")
                                MedicalStaffNavGraph.route
                            }
                        }

                        Log.d("AppNavigation", "Destination after login: $destination")
                        navController.navigate(destination) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToSignUp = {
                    Log.d("AppNavigation", "Navigating to SignUp")
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(Screen.SignUp.route) {
            Log.d("AppNavigation", "SignUp screen composable")

            SignUpScreen(
                viewModel = authViewModel,
                onAuthSuccessful = {
                    Log.d("AppNavigation", "Signup successful callback triggered")
                },
                onNavigateBack = {
                    Log.d("AppNavigation", "Navigating back from SignUp")
                    navController.popBackStack()
                }
            )
        }

        patientGraph(navController)
        adminGraph(navController)
        medicalStaffGraph(navController)
    }
}


fun NavGraphBuilder.adminGraph(
    navController: NavHostController,
) {
    Log.d("AppNavigation", "Building admin navigation graph")

    navigation(
        startDestination = Screen.AdminDashboard.route,
        route = AdminNavGraph.route
    ) {
        composable(Screen.AdminDashboard.route) { navBackStackEntry ->
            Log.d("AppNavigation", "AdminDashboard composable entered")
            val adminViewModel: AdminViewModel = hiltViewModel(
                remember(navBackStackEntry) {
                    navController.getBackStackEntry(AdminNavGraph.route)
                }
            )

            Log.d("AppNavigation", "AdminViewModel created, showing AdminDashboardScreen")

            AdminDashboardScreen(
                viewModel = adminViewModel,
                onNavigateToManageRequests = {
                    Log.d("AppNavigation", "Navigating to ManageRequests")
                    navController.navigate(Screen.ManageRequests.route)
                },
                onNavigateToRegisterPatient = {
                    Log.d("AppNavigation", "Navigating to RegisterPatient")
                    navController.navigate(Screen.RegisterPatient.route)
                },
                onNavigateToProfile = {
                    Log.d("AppNavigation", "Navigating to Profile")
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(Screen.ManageRequests.route) { navBackStackEntry ->
            Log.d("AppNavigation", "ManageRequests composable entered")

            val adminViewModel: AdminViewModel = hiltViewModel(
                remember(navBackStackEntry) {
                    navController.getBackStackEntry(AdminNavGraph.route)
                }
            )

            LaunchedEffect(Unit) {
                adminViewModel.refreshData()
            }

            ManageRequestsScreen(
                viewModel = adminViewModel,
                onBackClick = { navController.popBackStack() },
                onAssignRequest = { request ->
                    navController.navigate(Screen.AssignRequest.createRoute(request.id))
                }
            )
        }

        composable(
            route = Screen.AssignRequest.route,
            arguments = listOf(navArgument(Screen.AssignRequest.ARG_REQUEST_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            Log.d("AppNavigation", "AssignRequest composable entered")

            val adminViewModel: AdminViewModel = hiltViewModel()

            val requestId = backStackEntry.arguments?.getString(Screen.AssignRequest.ARG_REQUEST_ID)
                ?: throw IllegalArgumentException("Не указан ID заявки")

            val activeRequests by adminViewModel.activeRequests.collectAsState()
            val request = remember(activeRequests, requestId) {
                activeRequests.find { it.id == requestId }
            }

            if (request != null) {
                AssignRequestScreen(
                    viewModel = adminViewModel,
                    request = request,
                    onBackClick = { navController.popBackStack() },
                    onRequestAssigned = {
                        Log.d("AppNavigation", "Request assigned, navigating back to ManageRequests")
                        navController.popBackStack()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    Log.d("AppNavigation", "Request not found, refreshing data and navigating back")
                    adminViewModel.refreshData()
                    navController.popBackStack()
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Заявка обработана, возвращаемся к списку...")
                    }
                }
            }
        }

        composable(Screen.RegisterPatient.route) { navBackStackEntry ->
            Log.d("AppNavigation", "RegisterPatient composable entered")

            val adminViewModel: AdminViewModel = hiltViewModel(
                remember(navBackStackEntry) {
                    navController.getBackStackEntry(AdminNavGraph.route)
                }
            )
            RegisterPatientScreen(
                viewModel = adminViewModel,
                onBackClick = { navController.popBackStack() },
                onPatientRegistered = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Profile.route) { navBackStackEntry ->
            val authViewModel: AuthViewModel = hiltViewModel(
                remember(navBackStackEntry) {
                    navController.getBackStackEntry(AdminNavGraph.route)
                }
            )

            ProfileScreen(
                viewModel = authViewModel,
                navController = null,
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    authViewModel.signOut()
                }
            )
        }
    }

    Log.d("AppNavigation", "Admin graph setup completed")
}

fun NavGraphBuilder.medicalStaffGraph(
    navController: NavHostController
) {
    Log.d("AppNavigation", "Building medical staff navigation graph")

    navigation(
        startDestination = Screen.VisitList.route,
        route = MedicalStaffNavGraph.route
    ) {
        composable(Screen.VisitList.route) { navBackStackEntry ->
            Log.d("AppNavigation", "VisitList composable entered in medical staff graph")

            val visitListViewModel: VisitListViewModel = hiltViewModel(
                remember(navBackStackEntry) {
                    navController.getBackStackEntry(MedicalStaffNavGraph.route)
                }
            )

            VisitListScreen(
                viewModel = visitListViewModel,
                onVisitClick = { visit ->
                    visit.id?.let { visitId ->
                        navController.navigate(Screen.VisitDetail.createRoute(visitId))
                    } ?: run {
                        Log.e("MedicalStaffNavGraph", "Cannot navigate to visit detail: visit.id is null")
                    }
                },
                onProfileClick = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(
            route = Screen.VisitDetail.route,
            arguments = listOf(navArgument(Screen.VisitDetail.ARG_VISIT_ID) { type = NavType.StringType })
        ) { navBackStackEntry ->
            Log.d("AppNavigation", "VisitDetail composable entered in medical staff graph")

            val visitDetailViewModel: VisitDetailViewModel = hiltViewModel()

            VisitDetailScreen(
                viewModel = visitDetailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProtocol = { navController.navigate(Screen.Protocol.createRoute(it)) }
            )
        }

        composable(
            route = Screen.Protocol.route,
            arguments = listOf(navArgument(Screen.Protocol.ARG_VISIT_ID) { type = NavType.StringType })
        ) { navBackStackEntry ->
            Log.d("AppNavigation", "Protocol composable entered in medical staff graph")

            val protocolViewModel: ProtocolViewModel = hiltViewModel()

            ProtocolScreen(
                viewModel = protocolViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) { navBackStackEntry ->
            val authViewModel: AuthViewModel = hiltViewModel(
                remember(navBackStackEntry) {
                    navController.getBackStackEntry(MedicalStaffNavGraph.route)
                }
            )

            ProfileScreen(
                viewModel = authViewModel,
                navController = null,
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    authViewModel.signOut()
                }
            )
        }
    }

    Log.d("AppNavigation", "Medical staff graph setup completed")
}

fun NavGraphBuilder.patientGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = Screen.PatientHome.route,
        route = PatientNavGraph.route
    ) {
        composable(Screen.PatientHome.route) { navBackStackEntry ->
            val patientViewModel: PatientViewModel = hiltViewModel(
                remember(navBackStackEntry) {
                    navController.getBackStackEntry(PatientNavGraph.route)
                }
            )
            PatientRequestsScreen(
                viewModel = patientViewModel,
                onCreateRequest = { navController.navigate(Screen.CreateRequest.route) },
                onRequestDetails = { requestId ->
                    navController.navigate(Screen.RequestDetails.createRoute(requestId))
                },
                onProfileClick = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.PatientProfile.route) { navBackStackEntry ->
            val patientProfileViewModel: PatientProfileViewModel = hiltViewModel(
                remember(navBackStackEntry) {
                    navController.getBackStackEntry(PatientNavGraph.route)
                }
            )
            PatientProfileScreen(
                viewModel = patientProfileViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateRequest.route) { navBackStackEntry ->
            val patientViewModel: PatientViewModel = hiltViewModel(
                remember(navBackStackEntry) {
                    navController.getBackStackEntry(PatientNavGraph.route)
                }
            )
            CreateRequestScreen(
                viewModel = patientViewModel,
                onBackClick = { navController.popBackStack() },
                onRequestCreated = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RequestDetails.route,
            arguments = listOf(navArgument(Screen.RequestDetails.ARG_REQUEST_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val patientViewModel: PatientViewModel = hiltViewModel()

            val requestId = backStackEntry.arguments?.getString(Screen.RequestDetails.ARG_REQUEST_ID)
                ?: throw IllegalArgumentException("Не указан ID заявки")

            val requests by patientViewModel.requests.collectAsState()
            val request = remember(requests, requestId) {
                requests.find { it.id == requestId }
            }

            if (request != null) {
                RequestDetailsScreen(
                    request = request,
                    onCancelRequest = { reason ->
                        patientViewModel.cancelRequest(requestId, reason)
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(key1 = patientViewModel, key2 = requestId) {
                    if (patientViewModel.uiState.value is PatientUiState.Initial ||
                        (patientViewModel.uiState.value is PatientUiState.Success &&
                                patientViewModel.requests.value.isEmpty())) {
                        patientViewModel.refreshRequests()
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        composable(Screen.Profile.route) { navBackStackEntry ->
            val authViewModel: AuthViewModel = hiltViewModel(
                remember(navBackStackEntry) {
                    navController.getBackStackEntry(PatientNavGraph.route)
                }
            )

            ProfileScreen(
                viewModel = authViewModel,
                navController = navController,
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    authViewModel.signOut()
                }
            )
        }
    }
}

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun ImprovedAuthProtectedScreen(
    requiredRoles: List<UserRole>?,
    authState: AuthUiState,
    navController: androidx.navigation.NavController,
    content: @Composable () -> Unit
) {

    var hasRedirected by remember { mutableStateOf(false) }

    Log.d("AppNavigation", "ImprovedAuthProtectedScreen: authState = $authState, hasRedirected = $hasRedirected")
    Log.d("AppNavigation", "Required roles: $requiredRoles")


    val currentAuthState = authState
    when (currentAuthState) {
        is AuthUiState.LoggedIn -> {
            val user = currentAuthState.user
            Log.d("AppNavigation", "User logged in - Role: ${user.role}, Required: $requiredRoles")


            if (requiredRoles == null || requiredRoles.contains(user.role)) {
                Log.d("AppNavigation", "Access GRANTED - showing content")
                hasRedirected = false
                content()
            } else {
                Log.d("AppNavigation", "Access DENIED - redirecting to home screen")
                if (!hasRedirected) {
                    LaunchedEffect(Unit) {
                        hasRedirected = true
                        Log.d("AppNavigation", "Access denied, redirecting to home screen for role: ${user.role}")
                        val destination = when (user.role) {
                            UserRole.PATIENT -> PatientNavGraph.route
                            UserRole.ADMIN -> AdminNavGraph.route
                            UserRole.MEDICAL_STAFF -> MedicalStaffNavGraph.route
                        }
                        Log.d("AppNavigation", "Redirecting to: $destination")
                        navController.navigate(destination) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        is AuthUiState.Loading, is AuthUiState.Initial -> {
            Log.d("AppNavigation", "Auth state is loading/initial - showing progress")
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            Log.d("AppNavigation", "User not logged in - redirecting to login")
            if (!hasRedirected) {
                LaunchedEffect(Unit) {
                    hasRedirected = true
                    Log.d("AppNavigation", "User not logged in, redirecting to login")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}