package cl.smartvolt.asistencia.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.smartvolt.asistencia.modelo.Rol
import cl.smartvolt.asistencia.ui.theme.SmartVoltTheme
import cl.smartvolt.asistencia.ui.view.admin.AdminScreen
import cl.smartvolt.asistencia.ui.view.horasextra.AprobarHoraExtraScreen
import cl.smartvolt.asistencia.ui.view.general.CalendarioScreen
import cl.smartvolt.asistencia.ui.view.operario.CheckInScreen
import cl.smartvolt.asistencia.ui.view.capataz.DetalleVisacionScreen
import cl.smartvolt.asistencia.ui.view.capataz.ForemanScreen
import cl.smartvolt.asistencia.ui.view.horasextra.HoraExtraScreen
import cl.smartvolt.asistencia.ui.view.auth.LoginScreen
import cl.smartvolt.asistencia.ui.view.general.MapaScreen
import cl.smartvolt.asistencia.ui.view.horasextra.MisHorasExtraScreen
import cl.smartvolt.asistencia.ui.view.auth.RegisterView
import cl.smartvolt.asistencia.ui.view.profile.EditProfileView
import cl.smartvolt.asistencia.ui.view.settings.SettingsView
import cl.smartvolt.asistencia.ui.view.splash.SplashView

/** NavController ambiente para que las pantallas puedan navegar sin recibirlo por parámetro. */
val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("NavController no disponible")
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    var modoOscuro by rememberSaveable { mutableStateOf(false) }

    SmartVoltTheme(darkTheme = modoOscuro) {
        CompositionLocalProvider(LocalNavController provides nav) {
            NavHost(
                navController = nav,
                startDestination = "splash",
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
                // --- BIENVENIDA ---
                composable("splash") {
                    SplashView(
                        onFinished = {
                            nav.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    )
                }

                // --- AUTENTICACIÓN ---
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

                composable("register") {
                    RegisterView(
                        onNavigateToLogin = { nav.popBackStack() },
                        onRegisterSubmit = { _, _, _, _, _ ->
                            nav.popBackStack()
                        }
                    )
                }

                // --- FLUJOS POR ROL ---
                composable("operario") { CheckInScreen() }
                composable("capataz") { ForemanScreen() }
                composable("admin") {
                    AdminScreen(onNavUp = { nav.popBackStack() })
                }

                // --- PERFIL Y AJUSTES ---
                composable("editar-perfil") {
                    EditProfileView(
                        onGuardarContacto = { _, _ -> },
                        onVolver = { nav.popBackStack() }
                    )
                }

                composable("configuracion") {
                    SettingsView(
                        temaOscuro = modoOscuro,
                        onCambiarTemaOscuro = { modoOscuro = it },
                        onVolver = { nav.popBackStack() }
                    )
                }

                // --- HORAS EXTRA ---
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

                // --- CALENDARIO, MAPA Y VISACIÓN ---
                composable("calendario") {
                    CalendarioScreen(onNavUp = { nav.popBackStack() })
                }
                composable("mapa") {
                    MapaScreen(onNavUp = { nav.popBackStack() })
                }
                composable(
                    "detalle-visacion/{marcaId}",
                    arguments = listOf(navArgument("marcaId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val marcaId = backStackEntry.arguments?.getString("marcaId") ?: ""
                    DetalleVisacionScreen(marcaId = marcaId, onNavUp = { nav.popBackStack() })
                }
            }
        }
    }
}