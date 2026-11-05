package cl.smartvolt.asistencia.ui.checkin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.smartvolt.asistencia.ui.navigation.LocalNavController
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.BuildConfig
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.*
import cl.smartvolt.asistencia.domain.usecase.RegistrarMarcaUseCase
import cl.smartvolt.asistencia.notifications.Notificaciones
import cl.smartvolt.asistencia.sensors.BleScanner
import cl.smartvolt.asistencia.sensors.DemoState
import cl.smartvolt.asistencia.sensors.Escenario
import cl.smartvolt.asistencia.sync.SyncScheduler
import cl.smartvolt.asistencia.ui.login.SessionState
import cl.smartvolt.asistencia.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckInUi(
    val cargando: Boolean = false,
    val veredicto: Veredicto? = null,
    val error: String? = null,
    val aviso: String? = null,
    val avisoId: Long = 0,
)

/** Resultado de la prueba manual de beacon (solo variante campo). */
data class BeaconTestUi(
    val probando: Boolean = false,
    val mensaje: String? = null,
    val lecturas: List<LecturaBle> = emptyList(),
)

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val registrar: RegistrarMarcaUseCase,
    private val repo: AttendanceRepository,
    private val session: SessionState,
    private val ble: BleScanner,
    @ApplicationContext private val ctx: Context,
) : ViewModel() {
    private val _ui = MutableStateFlow(CheckInUi())
    val ui: StateFlow<CheckInUi> = _ui
    val pendientes = repo.contarPendientes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val usuarioId get() = session.usuario.value?.id.orEmpty()

    /** Beacons autorizados de la faena: se muestran para configurar Locate Beacon. */
    private val _beacons = MutableStateFlow<List<BeaconAutorizado>>(emptyList())
    val beacons: StateFlow<List<BeaconAutorizado>> = _beacons

    private val _beaconTest = MutableStateFlow(BeaconTestUi())
    val beaconTest: StateFlow<BeaconTestUi> = _beaconTest

    init {
        viewModelScope.launch {
            session.usuario.value?.let { u -> _beacons.value = repo.beacons(u.faenaId) }
        }
    }

    /** Escanea 8 s y muestra qué iBeacons ve realmente el teléfono. */
    fun probarBeacon() {
        val u = session.usuario.value ?: return
        viewModelScope.launch {
            _beaconTest.value = BeaconTestUi(probando = true)
            val autorizados = repo.beacons(u.faenaId)
            _beacons.value = autorizados
            val lecturas = runCatching { ble.escanear(autorizados, 8_000) }
                .getOrElse {
                    _beaconTest.value = BeaconTestUi(
                        mensaje = it.message ?: "No se pudo escanear Bluetooth.",
                    )
                    return@launch
                }
            val validas = lecturas.filter { l ->
                autorizados.any { a ->
                    a.uuid.equals(l.uuid, ignoreCase = true) && l.rssiPromedio >= a.rssiMinimo
                }
            }
            _beaconTest.value = BeaconTestUi(
                mensaje = when {
                    lecturas.isEmpty() ->
                        "No se detectó ningún iBeacon. Revisa Bluetooth, ubicación, permisos y que Locate Beacon esté emitiendo en primer plano."
                    validas.isEmpty() ->
                        "Se detectaron ${lecturas.size} beacon(s), pero ninguno autorizado o con señal suficiente."
                    else ->
                        "Beacon autorizado detectado: ${validas.first().uuid} (${validas.first().rssiPromedio} dBm)."
                },
                lecturas = lecturas,
            )
        }
    }

    fun marcar(tipo: TipoMarca) {
        val u = session.usuario.value ?: return
        viewModelScope.launch {
            _ui.value = CheckInUi(cargando = true)
            runCatching { registrar(u.id, u.faenaId, tipo) }
                .onSuccess {
                    // La regla SECUENCIA es la que detecta entradas duplicadas o
                    // salidas sin entrada. Se avisa con un Snackbar flotante.
                    val fallo = it.resultados.firstOrNull { r -> r.regla == "SECUENCIA" && !r.aprobada }
                    val aviso = when {
                        fallo == null -> null
                        tipo == TipoMarca.IN -> "Ya registraste tu entrada. Marca salida para cerrar el turno."
                        else -> "No hay una entrada previa registrada. Marca entrada primero."
                    }
                    _ui.value = CheckInUi(
                        veredicto = it,
                        aviso = aviso,
                        avisoId = if (aviso != null) System.currentTimeMillis() else 0,
                    )
                    if (it.estado == EstadoMarca.SOSPECHOSA) {
                        Notificaciones.marcaSospechosa(ctx, u.id, it.motivo)
                    }
                    SyncScheduler.enqueue(ctx)
                }
                .onFailure {
                    // Mensaje entendible para el usuario final; el detalle técnico
                    // (por ejemplo el código 17 de GMS) queda en los logs.
                    val mensaje = when (it) {
                        is SecurityException -> "Permiso de ubicación denegado. Actívalo en Ajustes."
                        else -> it.message ?: "Error inesperado"
                    }
                    _ui.value = CheckInUi(error = mensaje)
                }
        }
    }

    fun limpiarAviso() {
        _ui.value = _ui.value.copy(aviso = null)
    }

    fun cerrarSesion() = session.cerrarSesion()
}

private fun permisosCriticos(): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= 31) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    }
}.toTypedArray()

private fun faltanPermisos(c: Context) = !BuildConfig.USE_FAKE_SENSORS && permisosCriticos()
    .any { ContextCompat.checkSelfPermission(c, it) != PackageManager.PERMISSION_GRANTED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(vm: CheckInViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val ui by vm.ui.collectAsState()
    val pendientes by vm.pendientes.collectAsState()
    val beacons by vm.beacons.collectAsState()
    val beaconTest by vm.beaconTest.collectAsState()
    var mostrarBeacons by remember { mutableStateOf(false) }
    var sinPermisos by remember { mutableStateOf(faltanPermisos(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { sinPermisos = faltanPermisos(ctx) }
    LaunchedEffect(Unit) { if (sinPermisos) launcher.launch(permisosCriticos()) }

    // Notificaciones locales (Android 13+). Se piden aparte porque no bloquean la marca.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val nav = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.avisoId) {
        val aviso = ui.aviso
        if (aviso != null) {
            snackbarHostState.showSnackbar(aviso)
            vm.limpiarAviso()
        }
    }
    val volverAlLogin: () -> Unit = {
        vm.cerrarSesion()
        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
    }
    // En la pantalla raíz el botón atrás vuelve al login, no cierra la app.
    BackHandler(onBack = volverAlLogin)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Marcar asistencia · ${vm.usuarioId}") },
                navigationIcon = {
                    IconButton(onClick = volverAlLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver al inicio de sesión")
                    }
                },
                actions = {
                    IconButton(onClick = volverAlLogin) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (sinPermisos) {
                Card { Column(Modifier.padding(16.dp)) {
                    Text("Faltan permisos de ubicación o Bluetooth para validar tu presencia.")
                    TextButton(onClick = { launcher.launch(permisosCriticos()) }) { Text("Conceder permisos") }
                } }
            }
            // Botones grandes: uso con guantes (RNF-20).
            Button(onClick = { vm.marcar(TipoMarca.IN) }, enabled = !ui.cargando && !sinPermisos,
                colors = ButtonDefaults.buttonColors(containerColor = Ok), modifier = Modifier.fillMaxWidth().height(88.dp)) { Text("MARCAR ENTRADA") }
            Button(onClick = { vm.marcar(TipoMarca.OUT) }, enabled = !ui.cargando && !sinPermisos,
                modifier = Modifier.fillMaxWidth().height(88.dp)) { Text("MARCAR SALIDA") }
            AnimatedVisibility(
                visible = ui.cargando,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Validando ubicación y balizas…")
                }
            }

            AnimatedVisibility(
                visible = ui.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(ui.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            AnimatedContent(
                targetState = ui.veredicto,
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }) togetherWith
                        fadeOut(tween(150))
                },
                label = "resultado-marca",
            ) { veredicto ->
                if (veredicto != null) ResultadoCard(veredicto)
            }
            Text("Marcas pendientes de envío: $pendientes")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { nav.navigate("mis-horas-extra") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mis horas extra")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { nav.navigate("calendario") },
                    modifier = Modifier.weight(1f)
                ) { Text("Mi historial") }
                OutlinedButton(
                    onClick = { nav.navigate("mapa") },
                    modifier = Modifier.weight(1f)
                ) { Text("Ver mapa") }
            }

            if (BuildConfig.USE_FAKE_SENSORS) {
                val actual by DemoState.escenario.collectAsState()
                Text("Modo demo: escenario simulado", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Esta variante no usa GPS ni Bluetooth reales. Para probar el beacon instala la variante campo.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Escenario.values().forEach { e ->
                    FilterChip(selected = actual == e, onClick = { DemoState.escenario.value = e }, label = { Text(e.name) })
                }
                Text("Para probar offline, activa el modo avión.", style = MaterialTheme.typography.bodySmall)
            }

            // Ayuda para terreno: muestra los UUID que espera la faena del usuario.
            if (!BuildConfig.USE_FAKE_SENSORS) {
                if (beacons.isNotEmpty()) {
                    TextButton(
                        onClick = { mostrarBeacons = !mostrarBeacons },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (mostrarBeacons) "Ocultar beacons de prueba" else "Beacon de prueba (Locate Beacon)")
                    }
                    AnimatedVisibility(
                        visible = mostrarBeacons,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    "Configura un iBeacon con uno de estos UUID:",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                beacons.forEach { b ->
                                    Text("• ${b.uuid}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    "RSSI mínimo: ${beacons.first().rssiMinimo} dBm · major/minor no importan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Prueba real de escaneo: sirve para separar un problema de la app
                // de un problema de configuración del beacon.
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Diagnóstico del beacon", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Bluetooth encendido y Locate Beacon emitiendo en primer plano.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { vm.probarBeacon() },
                            enabled = !beaconTest.probando,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (beaconTest.probando) "Escaneando 8 s…" else "Probar beacon (8 s)")
                        }
                        beaconTest.mensaje?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        beaconTest.lecturas.forEach { l ->
                            Text(
                                "• ${l.uuid} · ${l.rssiPromedio} dBm · ${l.muestras} muestras",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultadoCard(v: Veredicto) {
    val color = when (v.estado) { EstadoMarca.VALIDA -> Ok; EstadoMarca.SOSPECHOSA -> Warn; EstadoMarca.RECHAZADA -> Bad }
    Card(Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(v.estado.name, color = color, style = MaterialTheme.typography.titleLarge)
            Text(v.motivo)
            v.resultados.forEach { Text((if (it.aprobada) "✔ " else "✖ ") + it.detalle, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
