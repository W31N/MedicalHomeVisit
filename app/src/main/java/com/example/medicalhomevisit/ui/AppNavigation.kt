// com/example/medicalhomevisit/ui/AppNavigation.kt
package com.example.medicalhomevisit.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.medicalhomevisit.data.model.UserRole
import com.example.medicalhomevisit.ui.admin.AdminDashboardScreen
import com.example.medicalhomevisit.ui.admin.AdminViewModel
import com.example.medicalhomevisit.ui.admin.AdminViewModelFactory
import com.example.medicalhomevisit.ui.admin.AssignRequestScreen
import com.example.medicalhomevisit.ui.admin.ManageRequestsScreen
import com.example.medicalhomevisit.ui.admin.RegisterPatientScreen
import com.example.medicalhomevisit.ui.auth.AuthUiState
import com.example.medicalhomevisit.ui.auth.AuthViewModel
//import com.example.medicalhomevisit.ui.auth.AuthViewModelFactory
import com.example.medicalhomevisit.ui.auth.LoginScreen
import com.example.medicalhomevisit.ui.auth.SignUpScreen
import com.example.medicalhomevisit.ui.patient.CreateRequestScreen
import com.example.medicalhomevisit.ui.patient.PatientRequestsScreen
import com.example.medicalhomevisit.ui.patient.PatientViewModel
import com.example.medicalhomevisit.ui.patient.PatientViewModelFactory
import com.example.medicalhomevisit.ui.patient.RequestDetailsScreen
import com.example.medicalhomevisit.ui.protocol.ProtocolScreen
import com.example.medicalhomevisit.ui.protocol.ProtocolViewModel
import com.example.medicalhomevisit.ui.protocol.ProtocolViewModelFactory
import com.example.medicalhomevisit.ui.visitdetail.VisitDetailScreen
import com.example.medicalhomevisit.ui.visitdetail.VisitDetailViewModel
import com.example.medicalhomevisit.ui.visitdetail.VisitDetailViewModelFactory
import com.example.medicalhomevisit.ui.visitlist.VisitListScreen
import com.example.medicalhomevisit.ui.visitlist.VisitListViewModel
import com.example.medicalhomevisit.ui.visitlist.VisitListViewModelFactory
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    // Определяем начальный путь на основе состояния авторизации
    val startDestination = when {
        authState is AuthUiState.LoggedIn -> {
            val user = (authState as AuthUiState.LoggedIn).user
            when (user.role) {
                UserRole.PATIENT -> Screen.PatientHome.route
                UserRole.ADMIN -> Screen.AdminDashboard.route
                UserRole.MEDICAL_STAFF,
                UserRole.DISPATCHER -> Screen.VisitList.route
                else -> Screen.Login.route
            }
        }
        else -> Screen.Login.route
    }

    // Определяем маппинг экранов к разрешенным ролям
    val screenAccessMap = mapOf(
        Screen.Login.route to null, // доступен всем
        Screen.SignUp.route to null, // доступен всем
        Screen.Profile.route to null, // доступен всем авторизованным
        Screen.PatientHome.route to listOf(UserRole.PATIENT),
        Screen.CreateRequest.route to listOf(UserRole.PATIENT),
        Screen.RequestDetails.route to listOf(UserRole.PATIENT),
        Screen.VisitList.route to listOf(UserRole.MEDICAL_STAFF, UserRole.DISPATCHER), // УБИРАЕМ ADMIN!
        Screen.VisitDetail.route to listOf(UserRole.MEDICAL_STAFF, UserRole.DISPATCHER), // УБИРАЕМ ADMIN!
        Screen.Protocol.route to listOf(UserRole.MEDICAL_STAFF, UserRole.DISPATCHER), // УБИРАЕМ ADMIN!
        Screen.AdminDashboard.route to listOf(UserRole.ADMIN), // Убедитесь что это есть
        Screen.ManageRequests.route to listOf(UserRole.ADMIN),
        Screen.AssignRequest.route to listOf(UserRole.ADMIN),
        Screen.RegisterPatient.route to listOf(UserRole.ADMIN)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route
    ) {

        composable(Screen.SplashScreen.route) {
            SplashScreen()
            Log.d("AppNavigation", "SplashScreen: Current authState: $authState") // <--- ЛОГ

            LaunchedEffect(authState) {
                Log.d("AppNavigation", "SplashScreen: LaunchedEffect triggered with authState: $authState")
                val currentAuthState = authState // Сохраняем в локальную переменную

                when (currentAuthState) {
                    is AuthUiState.LoggedIn -> {
                        val user = currentAuthState.user
                        Log.d("AppNavigation", "SplashScreen: Navigating based on LoggedIn state. User role: ${user.role}")

                        when (user.role) {
                            UserRole.PATIENT -> {
                                navController.navigate(Screen.PatientHome.route) {
                                    popUpTo(Screen.SplashScreen.route) { inclusive = true }
                                }
                            }
                            UserRole.ADMIN -> {
                                navController.navigate(Screen.AdminDashboard.route) {
                                    popUpTo(Screen.SplashScreen.route) { inclusive = true }
                                }
                            }
                            UserRole.MEDICAL_STAFF,
                            UserRole.DISPATCHER -> {
                                navController.navigate(Screen.VisitList.route) {
                                    popUpTo(Screen.SplashScreen.route) { inclusive = true }
                                }
                            }
                            else -> {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.SplashScreen.route) { inclusive = true }
                                }
                            }
                        }
                    }
                    is AuthUiState.NotLoggedIn -> {
                        Log.d("AppNavigation", "SplashScreen: AuthState is NotLoggedIn, navigating to Login.")
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.SplashScreen.route) { inclusive = true }
                        }
                    }
                    is AuthUiState.Initial, is AuthUiState.Loading -> {
                        Log.d("AppNavigation", "SplashScreen: AuthState is Initial or Loading, waiting...")
                        // Ждем изменения состояния
                    }
                    is AuthUiState.Error -> {
                        Log.e("AppNavigation", "SplashScreen: AuthState is Error: ${currentAuthState.message}. Navigating to Login.")
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.SplashScreen.route) { inclusive = true }
                        }
                    }
                    else -> {
                        Log.d("AppNavigation", "SplashScreen: AuthState is unexpected ($currentAuthState), navigating to Login as fallback.")
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.SplashScreen.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onAuthSuccessful = {
                    val userRole = authViewModel.getCurrentUserRole()
                    when (userRole) {
                        UserRole.PATIENT -> navController.navigate(Screen.PatientHome.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                        UserRole.ADMIN -> navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                        UserRole.MEDICAL_STAFF,
                        UserRole.DISPATCHER -> navController.navigate(Screen.VisitList.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                        null -> navController.navigate(Screen.Login.route)
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                viewModel = authViewModel,
                onAuthSuccessful = {
                    val userRole = authViewModel.getCurrentUserRole()
                    Log.d("AppNavigation", "SignUp successful, user role: $userRole")
                    when (userRole) {
                        UserRole.PATIENT -> navController.navigate(Screen.PatientHome.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                        UserRole.ADMIN -> navController.navigate(Screen.AdminDashboard.route) { // ← ИСПРАВЛЯЕМ!
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                        UserRole.MEDICAL_STAFF,
                        UserRole.DISPATCHER -> navController.navigate(Screen.VisitList.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                        null -> navController.navigate(Screen.Login.route)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Основные экраны приложения
        composable(Screen.VisitList.route) {
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.VisitList.route],
                authState = authState,
                navController = navController
            ) {
                val viewModel = remember { VisitListViewModelFactory().create(VisitListViewModel::class.java) }
                VisitListScreen(
                    viewModel = viewModel,
                    onVisitClick = { visit ->
                        navController.navigate(Screen.VisitDetail.createRoute(visit.id))
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route)
                    }
                )
            }
        }

        composable(
            route = Screen.VisitDetail.route,
            arguments = listOf(
                navArgument(Screen.VisitDetail.ARG_VISIT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.VisitDetail.route],
                authState = authState,
                navController = navController
            ) {
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
        }

        composable(
            route = Screen.Protocol.route,
            arguments = listOf(
                navArgument(Screen.Protocol.ARG_VISIT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.Protocol.route],
                authState = authState,
                navController = navController
            ) {
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

        // Экран профиля и настроек пользователя
        composable(Screen.Profile.route) {
            AuthProtectedScreen(
                requiredRoles = null, // доступен всем авторизованным
                authState = authState,
                navController = navController
            ) {
                ProfileScreen(
                    viewModel = authViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSignOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Главный экран пациента
        composable(Screen.PatientHome.route) {
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.PatientHome.route],
                authState = authState,
                navController = navController
            ) {
                val viewModel = remember { PatientViewModelFactory().create(PatientViewModel::class.java) }
                PatientRequestsScreen(
                    viewModel = viewModel,
                    onCreateRequest = { navController.navigate(Screen.CreateRequest.route) },
                    onRequestDetails = { requestId ->
                        navController.navigate(Screen.RequestDetails.createRoute(requestId))
                    },
                    onProfileClick = { navController.navigate(Screen.Profile.route) }
                )
            }
        }

        // Экран создания заявки
        composable(Screen.CreateRequest.route) {
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.CreateRequest.route],
                authState = authState,
                navController = navController
            ) {
                val viewModel = remember { PatientViewModelFactory().create(PatientViewModel::class.java) }
                CreateRequestScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onRequestCreated = { navController.popBackStack() }
                )
            }
        }

        // Экран деталей заявки
        composable(
            route = Screen.RequestDetails.route,
            arguments = listOf(
                navArgument(Screen.RequestDetails.ARG_REQUEST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.RequestDetails.route],
                authState = authState,
                navController = navController
            ) {
                val requestId = backStackEntry.arguments?.getString(Screen.RequestDetails.ARG_REQUEST_ID)
                    ?: throw IllegalArgumentException("Не указан ID заявки")

                val viewModel = remember { PatientViewModelFactory().create(PatientViewModel::class.java) }
                val requests by viewModel.requests.collectAsState()

                val request = requests.find { it.id == requestId }

                if (request != null) {
                    RequestDetailsScreen(
                        request = request,
                        onCancelRequest = { reason ->
                            viewModel.cancelRequest(requestId, reason)
                            navController.popBackStack()
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
        composable(Screen.AdminDashboard.route) {
            AuthProtectedScreen(
                requiredRoles = listOf(UserRole.ADMIN),
                authState = authState,
                navController = navController
            ) {
                val viewModel = remember { AdminViewModelFactory().create(AdminViewModel::class.java) }
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToManageRequests = { navController.navigate(Screen.ManageRequests.route) },
                    onNavigateToRegisterPatient = { navController.navigate(Screen.RegisterPatient.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }
        }

// Экран управления заявками
        composable(Screen.ManageRequests.route) {
            AuthProtectedScreen(
                requiredRoles = listOf(UserRole.ADMIN),
                authState = authState,
                navController = navController
            ) {
                val viewModel = remember { AdminViewModelFactory().create(AdminViewModel::class.java) }
                ManageRequestsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onAssignRequest = { request ->
                        navController.navigate(Screen.AssignRequest.createRoute(request.id))
                    }
                )
            }
        }

// Экран назначения заявки
        composable(
            route = Screen.AssignRequest.route,
            arguments = listOf(
                navArgument(Screen.AssignRequest.ARG_REQUEST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            AuthProtectedScreen(
                requiredRoles = listOf(UserRole.ADMIN),
                authState = authState,
                navController = navController
            ) {
                val requestId = backStackEntry.arguments?.getString(Screen.AssignRequest.ARG_REQUEST_ID)
                    ?: throw IllegalArgumentException("Не указан ID заявки")

                val viewModel = remember { AdminViewModelFactory().create(AdminViewModel::class.java) }
                val activeRequests by viewModel.activeRequests.collectAsState()

                // Находим заявку по ID
                val request = activeRequests.find { it.id == requestId }

                if (request != null) {
                    AssignRequestScreen(
                        viewModel = viewModel,
                        request = request,
                        onBackClick = { navController.popBackStack() },
                        onRequestAssigned = { navController.popBackStack() }
                    )
                } else {
                    // Если заявка не найдена, показываем индикатор загрузки
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    // Загружаем активные заявки
                    LaunchedEffect(Unit) {
                        viewModel.refreshData()
                    }
                }
            }
        }

// Экран регистрации пациента
        composable(Screen.RegisterPatient.route) {
            AuthProtectedScreen(
                requiredRoles = listOf(UserRole.ADMIN),
                authState = authState,
                navController = navController
            ) {
                val viewModel = remember { AdminViewModelFactory().create(AdminViewModel::class.java) }
                RegisterPatientScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onPatientRegistered = { navController.popBackStack() }
                )
            }
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
fun AuthProtectedScreen(
    requiredRoles: List<UserRole>?,
    authState: AuthUiState,
    navController: androidx.navigation.NavController,
    content: @Composable () -> Unit
) {
    // Проверяем, авторизован ли пользователь
    if (authState !is AuthUiState.LoggedIn) {
        Log.d("AppNavigation", "User not logged in, redirecting to login")
        LaunchedEffect(Unit) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
        // Показываем загрузку пока происходит редирект
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val user = (authState as AuthUiState.LoggedIn).user
    Log.d("AppNavigation", "Current user role: ${user.role}, required roles: $requiredRoles")

    // Если экран доступен всем авторизованным или роль соответствует требуемым
    if (requiredRoles == null || requiredRoles.contains(user.role)) {
        content()
    } else {
        // Редирект на соответствующий домашний экран
        LaunchedEffect(Unit) {
            Log.d("AppNavigation", "Access denied, redirecting to appropriate home screen")
            when (user.role) {
                UserRole.PATIENT -> navController.navigate(Screen.PatientHome.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
                UserRole.ADMIN -> navController.navigate(Screen.AdminDashboard.route) { // ← ИСПРАВЛЯЕМ!
                    popUpTo(navController.graph.id) { inclusive = true }
                }
                UserRole.MEDICAL_STAFF,
                UserRole.DISPATCHER -> navController.navigate(Screen.VisitList.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
                else -> navController.navigate(Screen.Login.route)
            }
        }
        // Показываем загрузку пока происходит редирект
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

sealed class Screen(val route: String) {
    object SplashScreen : Screen("splash_screen")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object VisitList : Screen("visitList")
    object Profile : Screen("profile")

    object PatientHome : Screen("patientHome")
    object CreateRequest : Screen("createRequest")
    object RequestDetails : Screen("requestDetails/{requestId}") {
        const val ARG_REQUEST_ID = "requestId"

        fun createRoute(requestId: String): String {
            return "requestDetails/$requestId"
        }
    }

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
    object AdminDashboard : Screen("adminDashboard")
    object ManageRequests : Screen("manageRequests")
    object AssignRequest : Screen("assignRequest/{requestId}") {
        const val ARG_REQUEST_ID = "requestId"

        fun createRoute(requestId: String): String {
            return "assignRequest/$requestId"
        }
    }
    object RegisterPatient : Screen("registerPatient")
}