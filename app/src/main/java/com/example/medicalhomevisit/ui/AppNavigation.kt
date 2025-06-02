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
//import com.example.medicalhomevisit.ui.patient.PatientViewModelFactory
import com.example.medicalhomevisit.ui.patient.RequestDetailsScreen
import com.example.medicalhomevisit.ui.protocol.ProtocolScreen
import com.example.medicalhomevisit.ui.protocol.ProtocolViewModel
import com.example.medicalhomevisit.ui.protocol.ProtocolViewModelFactory
import com.example.medicalhomevisit.ui.visitdetail.VisitDetailScreen
import com.example.medicalhomevisit.ui.visitdetail.VisitDetailViewModel
import com.example.medicalhomevisit.ui.visitlist.VisitListScreen
import com.example.medicalhomevisit.ui.visitlist.VisitListViewModel
import com.example.medicalhomevisit.ui.visitlist.VisitListViewModelFactory
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.example.medicalhomevisit.ui.patient.PatientUiState

// Объект для маршрута вложенного графа пациента (если еще не вынесен)
object PatientNavGraph {
    const val route = "patient_graph_route"
}

// Sealed class для экранов (оставляем твою версию)
sealed class Screen(val route: String) {
    object SplashScreen : Screen("splash_screen")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object VisitList : Screen("visitList")
    object Profile : Screen("profile")

    object PatientHome : Screen("patientHome") // Это будет startDestination для patient_graph_route
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

    // screenAccessMap остается как у тебя
    val screenAccessMap = mapOf(
        Screen.Login.route to null,
        Screen.SignUp.route to null,
        Screen.Profile.route to null,
        PatientNavGraph.route to listOf(UserRole.PATIENT), // Доступ к самому графу пациента
        Screen.VisitList.route to listOf(UserRole.MEDICAL_STAFF, UserRole.DISPATCHER),
        Screen.VisitDetail.route to listOf(UserRole.MEDICAL_STAFF, UserRole.DISPATCHER),
        Screen.Protocol.route to listOf(UserRole.MEDICAL_STAFF, UserRole.DISPATCHER),
        Screen.AdminDashboard.route to listOf(UserRole.ADMIN),
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
            Log.d("AppNavigation", "SplashScreen: Current authState: $authState")

            LaunchedEffect(authState) {
                Log.d("AppNavigation", "SplashScreen: LaunchedEffect triggered with authState: $authState")
                val currentAuthState = authState

                val destination: String? = when (currentAuthState) {
                    is AuthUiState.LoggedIn -> {
                        when (currentAuthState.user.role) {
                            UserRole.PATIENT -> PatientNavGraph.route // Навигация на ГРАФ пациента
                            UserRole.ADMIN -> Screen.AdminDashboard.route
                            UserRole.MEDICAL_STAFF, UserRole.DISPATCHER -> Screen.VisitList.route
                            // Убери else, если UserRole - enum и все варианты покрыты
                        }
                    }
                    is AuthUiState.NotLoggedIn, is AuthUiState.Error -> Screen.Login.route
                    is AuthUiState.Initial, is AuthUiState.Loading -> null
                    // ИСПРАВЛЕНИЕ ДЛЯ ПОЛНОТЫ WHEN:
                    AuthUiState.PasswordResetSent -> Screen.Login.route // или другой экран
                    is AuthUiState.RegistrationSuccessful -> { // После успешной регистрации решаем, куда идти
                        when (currentAuthState.user.role) {
                            UserRole.PATIENT -> PatientNavGraph.route
                            UserRole.ADMIN -> Screen.AdminDashboard.route
                            UserRole.MEDICAL_STAFF, UserRole.DISPATCHER -> Screen.VisitList.route
                            // Убери else, если UserRole - enum и все варианты покрыты
                        }
                    }
                }

                destination?.let {
                    Log.d("AppNavigation", "SplashScreen: Navigating to $it")
                    navController.navigate(it) {
                        popUpTo(Screen.SplashScreen.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onAuthSuccessful = { // user придет из authState, который обновится
                    val user = (authViewModel.uiState.value as? AuthUiState.LoggedIn)?.user
                    val route = when (user?.role) {
                        UserRole.PATIENT -> PatientNavGraph.route
                        UserRole.ADMIN -> Screen.AdminDashboard.route
                        UserRole.MEDICAL_STAFF, UserRole.DISPATCHER -> Screen.VisitList.route
                        else -> Screen.Login.route // На случай если роль null или что-то пошло не так
                    }
                    navController.navigate(route) {
                        popUpTo(Screen.SplashScreen.route) { inclusive = true } // Очищаем весь стек до сплэша
                        launchSingleTop = true
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                viewModel = authViewModel,
                onAuthSuccessful = { // user придет из authState
                    val user = (authViewModel.uiState.value as? AuthUiState.LoggedIn)?.user
                    val route = when (user?.role) {
                        UserRole.PATIENT -> PatientNavGraph.route
                        UserRole.ADMIN -> Screen.AdminDashboard.route
                        UserRole.MEDICAL_STAFF, UserRole.DISPATCHER -> Screen.VisitList.route
                        else -> Screen.Login.route
                    }
                    navController.navigate(route) {
                        popUpTo(Screen.SplashScreen.route) { inclusive = true } // Очищаем весь стек до сплэша
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- ГРАФ НАВИГАЦИИ ДЛЯ ПАЦИЕНТА ---
        patientGraph(navController, authViewModel, authState, screenAccessMap)


        // --- ОСТАЛЬНЫЕ ГРАФЫ/ЭКРАНЫ ---
        // Пример для VisitListViewModel (аналогично для других)
        composable(Screen.VisitList.route) {
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.VisitList.route],
                authState = authState,
                navController = navController
            ) {
                val viewModel: VisitListViewModel = hiltViewModel()
                VisitListScreen(viewModel,
                    onVisitClick = {navController.navigate(Screen.VisitDetail.createRoute(it.id))},
                    onProfileClick = {navController.navigate(Screen.Profile.route)}
                )
            }
        }

        composable(
            route = Screen.VisitDetail.route,
            arguments = listOf(navArgument(Screen.VisitDetail.ARG_VISIT_ID) { type = NavType.StringType })
        ) {
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.VisitDetail.route],
                authState = authState,
                navController = navController
            ) {
                val viewModel: VisitDetailViewModel = hiltViewModel()
                VisitDetailScreen(viewModel,
                    onNavigateBack = {navController.popBackStack()},
                    onNavigateToProtocol = {navController.navigate(Screen.Protocol.createRoute(it))}
                )
            }
        }

        composable(
            route = Screen.Protocol.route,
            arguments = listOf(navArgument(Screen.Protocol.ARG_VISIT_ID) { type = NavType.StringType })
        ) {
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.Protocol.route],
                authState = authState,
                navController = navController
            ) {
                val viewModel: ProtocolViewModel = hiltViewModel()
                ProtocolScreen(viewModel, onNavigateBack = {navController.popBackStack()})
            }
        }


        composable(Screen.Profile.route) {
            AuthProtectedScreen(
                requiredRoles = null, // Доступен всем авторизованным
                authState = authState,
                navController = navController
            ) {
                ProfileScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSignOut = {
                        // AuthViewModel обработает signOut, authState изменится, SplashScreen перенаправит
                        // Можно добавить явную навигацию для ускорения, если нужно
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.SplashScreen.route) { inclusive = true } // Очищаем все до сплэша
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        // --- ГРАФ НАВИГАЦИИ ДЛЯ АДМИНА (ПРИМЕР) ---
        // Аналогично можно сделать adminGraph, если экраны админа тоже должны делить AdminViewModel
        composable(Screen.AdminDashboard.route) {
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.AdminDashboard.route],
                authState = authState,
                navController = navController
            ) {
                val viewModel: AdminViewModel = hiltViewModel() // Если AdminViewModel не привязана к графу
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToManageRequests = { navController.navigate(Screen.ManageRequests.route) },
                    onNavigateToRegisterPatient = { navController.navigate(Screen.RegisterPatient.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }
        }

        composable(Screen.ManageRequests.route) {
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.ManageRequests.route],
                authState = authState,
                navController = navController
            ) {
                val viewModel: AdminViewModel = hiltViewModel()

                LaunchedEffect(Unit) {
                    viewModel.refreshData()
                }

                ManageRequestsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onAssignRequest = { request ->
                        navController.navigate(Screen.AssignRequest.createRoute(request.id))
                    }
                )
            }
        }
        composable(
            route = Screen.AssignRequest.route,
            arguments = listOf(navArgument(Screen.AssignRequest.ARG_REQUEST_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.AssignRequest.route],
                authState = authState,
                navController = navController
            ) {
                val adminViewModel: AdminViewModel = hiltViewModel() // Пока так
                val requestId = backStackEntry.arguments?.getString(Screen.AssignRequest.ARG_REQUEST_ID)
                    ?: throw IllegalArgumentException("Не указан ID заявки")
                val activeRequests by adminViewModel.activeRequests.collectAsState()
                val request = activeRequests.find { it.id == requestId }

                if (request != null) {
                    AssignRequestScreen(
                        viewModel = adminViewModel,
                        request = request,
                        onBackClick = { navController.popBackStack() },
                        onRequestAssigned = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) { adminViewModel.refreshData() }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        composable(Screen.RegisterPatient.route) {
            AuthProtectedScreen(
                requiredRoles = screenAccessMap[Screen.RegisterPatient.route],
                authState = authState,
                navController = navController
            ) {
                val viewModel: AdminViewModel = hiltViewModel() // Пока так
                RegisterPatientScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onPatientRegistered = { navController.popBackStack() }
                )
            }
        }
    }
}

// Функция для построения вложенного графа пациента
fun NavGraphBuilder.patientGraph(
    navController: NavHostController,
    authViewModelPassed: AuthViewModel, // Передаем AuthViewModel для AuthProtectedScreen
    authStatePassed: AuthUiState,       // Передаем AuthState для AuthProtectedScreen
    screenAccessMapPassed: Map<String, List<UserRole>?> // Передаем screenAccessMap
) {
    navigation(
        startDestination = Screen.PatientHome.route,
        route = PatientNavGraph.route
    ) {
        composable(Screen.PatientHome.route) { navBackStackEntry ->
            // AuthProtectedScreen для всего графа не очень хорошо работает с viewModel scoping к графу.
            // Лучше вызывать AuthProtectedScreen внутри каждого composable, если нужна защита
            // или сделать его частью самого PatientRequestsScreen и других экранов.
            // Здесь я уберу AuthProtectedScreen с уровня графа и предположу,
            // что он вызывается внутри каждого composable ниже, как в твоем оригинальном коде.
            // Либо, если PatientNavGraph.route уже защищен в screenAccessMap, то этого достаточно.

            val patientViewModel: PatientViewModel = hiltViewModel(
                remember(navBackStackEntry) { // remember важен здесь
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
            val patientViewModel: PatientViewModel = hiltViewModel(
                remember(backStackEntry) { // Используем backStackEntry текущего composable для remember ключа
                    navController.getBackStackEntry(PatientNavGraph.route)
                }
            )
            val requestId = backStackEntry.arguments?.getString(Screen.RequestDetails.ARG_REQUEST_ID)
                ?: throw IllegalArgumentException("Не указан ID заявки")

            val requests by patientViewModel.requests.collectAsState()
            val request = remember(requests, requestId) { // Пересчитываем только если requests или requestId изменились
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
                // Если PatientViewModel загружает данные в init, то к моменту навигации сюда
                // данные уже должны быть или загружаться. Можно добавить LaunchedEffect для специфичной загрузки,
                // если это необходимо, или если мы пришли по deep link и ViewModel только что создалась.
                LaunchedEffect(key1 = patientViewModel, key2 = requestId) {
                    // Проверяем, нужно ли загружать, если список пуст и это не ошибка
                    if (patientViewModel.uiState.value is PatientUiState.Initial || (patientViewModel.uiState.value is PatientUiState.Success && patientViewModel.requests.value.isEmpty())) {
                        patientViewModel.refreshRequests()
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
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