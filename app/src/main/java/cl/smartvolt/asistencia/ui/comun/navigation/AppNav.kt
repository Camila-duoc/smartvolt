package cl.smartvolt.asistencia.ui.comun.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.smartvolt.asistencia.modelo.Rol
import cl.smartvolt.asistencia.ui.view.AdminScreen
import cl.smartvolt.asistencia.ui.view.CalendarioScreen
import cl.smartvolt.asistencia.ui.view.CheckInScreen
import cl.smartvolt.asistencia.ui.view.ForemanScreen
import cl.smartvolt.asistencia.ui.view.AprobarHoraExtraScreen
import cl.smartvolt.asistencia.ui.view.HoraExtraScreen
import cl.smartvolt.asistencia.ui.view.MisHorasExtraScreen
import cl.smartvolt.asistencia.ui.comun.login.view.LoginScreen
import cl.smartvolt.asistencia.ui.view.MapaScreen
import cl.smartvolt.asistencia.ui.view.DetalleVisacionScreen

/** NavController ambiente para que las pantallas puedan navegar sin recibirlo por parámetro. */
val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("NavController no disponible")
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    CompositionLocalProvider(LocalNavController provides nav) {
        NavHost(
            navController = nav,
            startDestination = "login",
            enterTransition = {
                slideInHorizontally(animationSpec = tween(320), initialOffsetX = { it / 5 }) +
                    fadeIn(animationSpec = tween(320))
            },
            exitTransition = { fadeOut(animationSpec = tween(180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(320)) },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(320), targetOffsetX = { it / 5 }) +
                    fadeOut(animationSpec = tween(180))
            },
        ) {
            composable("login") {
                LoginScreen(
                    onLogin = { rol ->
                        val destino = when (rol) {
                            Rol.OPERARIO -> "operario"
                            Rol.CAPATAZ -> "capataz"
                            Rol.ADMIN -> "admin"
                        }
                        nav.navigate(destino) { popUpTo("login") { inclusive = true } }
                    }
                )
            }
            composable("operario") { CheckInScreen() }
            composable("capataz") { ForemanScreen() }
            composable("horas-extra") {
                HoraExtraScreen(onNavUp = { nav.popBackStack() })
            }
            composable("mis-horas-extra") {
                MisHorasExtraScreen(
                    onNavUp = { nav.popBackStack() },
                    onSolicitar = { nav.navigate("horas-extra") }
                )
            }
            composable("aprobar-horas-extra") {
                AprobarHoraExtraScreen(onNavUp = { nav.popBackStack() })
            }
            composable("calendario") {
                CalendarioScreen(onNavUp = { nav.popBackStack() })
            }
            composable("mapa") {
                MapaScreen(onNavUp = { nav.popBackStack() })
            }
            composable(
                "detalle-visacion/{marcaId}",
                arguments = listOf(navArgument("marcaId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val marcaId = backStackEntry.arguments?.getString("marcaId") ?: ""
                DetalleVisacionScreen(marcaId = marcaId, onNavUp = { nav.popBackStack() })
            }
            composable("admin") {
                AdminScreen(onNavUp = { nav.popBackStack() })
            }
        }
    }
}
