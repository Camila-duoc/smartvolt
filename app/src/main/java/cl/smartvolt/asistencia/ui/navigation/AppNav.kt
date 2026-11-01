package cl.smartvolt.asistencia.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.navArgument
import cl.smartvolt.asistencia.domain.model.Rol
import cl.smartvolt.asistencia.ui.admin.AdminScreen
import cl.smartvolt.asistencia.ui.calendario.CalendarioScreen
import cl.smartvolt.asistencia.ui.checkin.CheckInScreen
import cl.smartvolt.asistencia.ui.foreman.ForemanScreen
import cl.smartvolt.asistencia.ui.horasextra.AprobarHoraExtraScreen
import cl.smartvolt.asistencia.ui.horasextra.HoraExtraScreen
import cl.smartvolt.asistencia.ui.horasextra.MisHorasExtraScreen
import cl.smartvolt.asistencia.ui.login.LoginScreen
import cl.smartvolt.asistencia.ui.mapa.MapaScreen
import cl.smartvolt.asistencia.ui.visacion.DetalleVisacionScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login") {
        composable("login") {
            LoginScreen { rol ->
                val destino = when (rol) {
                    Rol.OPERARIO -> "operario"
                    Rol.CAPATAZ -> "capataz"
                    Rol.ADMIN -> "admin"
                }
                nav.navigate(destino) { popUpTo("login") { inclusive = true } }
            }
        }
        composable("operario") { CheckInScreen() }
        composable("capataz") { ForemanScreen() }
        composable("horas-extra") {
            HoraExtraScreen { nav.popBackStack() }
        }
        composable("mis-horas-extra") {
            MisHorasExtraScreen { nav.popBackStack() }
        }
        composable("aprobar-horas-extra") {
            AprobarHoraExtraScreen { nav.popBackStack() }
        }
        composable("calendario") {
            CalendarioScreen { nav.popBackStack() }
        }
        composable(
            "mapa/{lat}/{lon}",
            arguments = listOf(
                navArgument("lat") { type = androidx.navigation.NavType.FloatType },
                navArgument("lon") { type = androidx.navigation.NavType.FloatType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat") ?: -33.4180f
            val lon = backStackEntry.arguments?.getFloat("lon") ?: -70.6050f
            MapaScreen(lat = lat.toDouble(), lon = lon.toDouble()) { nav.popBackStack() }
        }
        composable(
            "detalle-visacion/{marcaId}",
            arguments = listOf(navArgument("marcaId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val marcaId = backStackEntry.arguments?.getString("marcaId") ?: ""
            DetalleVisacionScreen(marcaId = marcaId) { nav.popBackStack() }
        }
        composable("admin") {
            AdminScreen { nav.popBackStack() }
        }
    }
}
