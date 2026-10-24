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
import cl.smartvolt.asistencia.domain.model.Rol
import cl.smartvolt.asistencia.ui.checkin.CheckInScreen
import cl.smartvolt.asistencia.ui.foreman.ForemanScreen
import cl.smartvolt.asistencia.ui.login.LoginScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login") {
        composable("login") {
            LoginScreen { rol ->
                val destino = when (rol) { Rol.OPERARIO -> "operario"; Rol.CAPATAZ -> "capataz"; Rol.ADMIN -> "admin" }
                nav.navigate(destino) { popUpTo("login") { inclusive = true } }
            }
        }
        composable("operario") { CheckInScreen() }
        composable("capataz") { ForemanScreen() }
        // TODO: pantallas de Administración (HU-ADM-01..04): consolidado de horas, ausentismo, auditoría, exportación.
        composable("admin") { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Administración: pendiente de implementar") } }
    }
}
