package cl.smartvolt.asistencia.ui.view.operario

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import cl.smartvolt.asistencia.BuildConfig
import cl.smartvolt.asistencia.R
import cl.smartvolt.asistencia.modelo.EstadoMarca
import cl.smartvolt.asistencia.modelo.TipoMarca
import cl.smartvolt.asistencia.modelo.Veredicto
import cl.smartvolt.asistencia.sensors.DemoState
import cl.smartvolt.asistencia.sensors.Escenario
import cl.smartvolt.asistencia.ui.navigation.LocalNavController
import cl.smartvolt.asistencia.ui.theme.Bad
import cl.smartvolt.asistencia.ui.theme.Ok
import cl.smartvolt.asistencia.ui.theme.Warn
import cl.smartvolt.asistencia.ui.view.profile.ProfileView
import cl.smartvolt.asistencia.viewmodel.CheckInViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class CheckInNavItem(val titulo: String, val icono: ImageVector)

private fun permisosCriticos(): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_SCAN)
}.toTypedArray()

private fun faltanPermisos(c: Context) = !BuildConfig.USE_FAKE_SENSORS && permisosCriticos()
    .any { ContextCompat.checkSelfPermission(c, it) != PackageManager.PERMISSION_GRANTED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(vm: CheckInViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val ui by vm.ui.collectAsState()
    val pendientes by vm.pendientes.collectAsState()
    var sinPermisos by remember { mutableStateOf(faltanPermisos(ctx)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { sinPermisos = faltanPermisos(ctx) }

    LaunchedEffect(Unit) { if (sinPermisos) launcher.launch(permisosCriticos()) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val nav = LocalNavController.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var mostrarAvisosDialog by remember { mutableStateOf(false) }
    var fotoUriLocal by remember { mutableStateOf<String?>(null) }

    val navItems = listOf(
        CheckInNavItem("Inicio", Icons.Default.Home),
        CheckInNavItem("Mi Perfil", Icons.Default.Person)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.logo_horizontal),
                        contentDescription = "Logo SmartVolt",
                        modifier = Modifier.height(34.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { mostrarAvisosDialog = true }) {
                        BadgedBox(badge = { Badge { Text("2") } }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Avisos")
                        }
                    }
                    IconButton(onClick = {
                        vm.cerrarSesion()
                        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = { Icon(item.icono, contentDescription = item.titulo) },
                        label = { Text(item.titulo) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { pad ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(pad)
        ) { pestana ->
            when (pestana) {
                0 -> CheckInHomeContent(
                    usuarioId = vm.usuarioId,
                    cargando = ui.cargando,
                    error = ui.error,
                    veredicto = ui.veredicto,
                    pendientes = pendientes,
                    sinPermisos = sinPermisos,
                    onSolicitarPermisos = { launcher.launch(permisosCriticos()) },
                    onMarcarEntrada = { vm.marcar(TipoMarca.IN) },
                    onMarcarSalida = { vm.marcar(TipoMarca.OUT) },
                    onNavegarHorasExtra = { nav.navigate("mis-horas-extra") },
                    onNavegarCalendario = { nav.navigate("calendario") },
                    onNavegarMapa = { nav.navigate("mapa") }
                )
                else -> ProfileView(
                    nombre = vm.usuarioId,
                    rut = vm.usuarioId,
                    email = "${vm.usuarioId.lowercase()}@smartvolt.cl",
                    fotoUri = fotoUriLocal,
                    cargo = "Operador en Terreno",
                    obra = "Faena Smart-Volt",
                    onSeleccionarFoto = { uri -> fotoUriLocal = uri.toString() },
                    onCerrarSesion = {
                        vm.cerrarSesion()
                        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
                    }
                )
            }
        }
    }

    if (mostrarAvisosDialog) {
        AlertDialog(
            onDismissRequest = { mostrarAvisosDialog = false },
            title = { Text("Avisos y Aprobaciones", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Revisa tus solicitudes de horas extra aprobadas.", fontSize = 14.sp)
                    Text("• Sincronización automática activa al recuperar señal.", fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarAvisosDialog = false }) { Text("Entendido") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarAvisosDialog = false
                    nav.navigate("mis-horas-extra")
                }) { Text("Ver horas extra") }
            }
        )
    }
}

@Composable
private fun CheckInHomeContent(
    usuarioId: String,
    cargando: Boolean,
    error: String?,
    veredicto: Veredicto?,
    pendientes: Int,
    sinPermisos: Boolean,
    onSolicitarPermisos: () -> Unit,
    onMarcarEntrada: () -> Unit,
    onMarcarSalida: () -> Unit,
    onNavegarHorasExtra: () -> Unit,
    onNavegarCalendario: () -> Unit,
    onNavegarMapa: () -> Unit
) {
    var fechaActual by remember { mutableStateOf("") }
    var horaActual by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val formatoFecha = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "CL"))
        val formatoHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            val now = Date()
            fechaActual = formatoFecha.format(now).replaceFirstChar { it.uppercase() }
            horaActual = formatoHora.format(now)
            delay(1000L)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Tarjeta de Saludo con Avatar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = usuarioId.take(2).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "¡Hola, $usuarioId!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Control de Asistencia en Terreno",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Alerta de permisos críticos (si faltan GPS/Bluetooth)
        if (sinPermisos) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Faltan permisos de ubicación o Bluetooth para validar tu presencia.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onSolicitarPermisos,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Conceder permisos")
                        }
                    }
                }
            }
        }

        // 2. Tarjeta Principal de Asistencia (Reloj en vivo + Botones Entrada/Salida)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Asistencia de hoy",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = fechaActual.ifBlank { "Cargando fecha..." },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = horaActual.ifBlank { "--:--:--" },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Botones grandes (RNF-20: uso cómodo con guantes)
                    Button(
                        onClick = onMarcarEntrada,
                        enabled = !cargando && !sinPermisos,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ok,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("MARCAR ENTRADA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onMarcarSalida,
                        enabled = !cargando && !sinPermisos,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("MARCAR SALIDA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    AnimatedVisibility(
                        visible = cargando,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(
                                text = "Validando ubicación y balizas…",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = error != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }

        // 3. Resultado de Veredicto (GPS / Balizas)
        item {
            AnimatedContent(
                targetState = veredicto,
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }) togetherWith
                            fadeOut(tween(150))
                },
                label = "resultado-marca"
            ) { v ->
                if (v != null) ResultadoCard(v)
            }
        }

        // 4. Barrita Destacada para Gestión de Horas Extras
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavegarHorasExtra() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreTime,
                                contentDescription = "Horas Extras",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gestión de Horas Extras",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Postula y revisa tus cupos aprobados",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ir a Horas Extras",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 5. Tarjetas de Resumen (Pendientes Offline + Accesos Rápidos)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$pendientes",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Pendientes de envío",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavegarCalendario,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mi historial", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onNavegarMapa,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver mapa", fontSize = 13.sp)
                    }
                }
            }
        }

        // 6. Panel de Escenarios Simulados (Modo Demo)
        if (BuildConfig.USE_FAKE_SENSORS) {
            item {
                val actual by DemoState.escenario.collectAsState()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Modo demo: escenario simulado",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Escenario.values().forEach { e ->
                            FilterChip(
                                selected = actual == e,
                                onClick = { DemoState.escenario.value = e },
                                label = { Text(e.name) }
                            )
                        }
                        Text(
                            text = "Para probar offline, activa el modo avión.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultadoCard(v: Veredicto) {
    val color = when (v.estado) {
        EstadoMarca.VALIDA -> Ok
        EstadoMarca.SOSPECHOSA -> Warn
        EstadoMarca.RECHAZADA -> Bad
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, color)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(v.estado.name, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(v.motivo, color = MaterialTheme.colorScheme.onSurface)
            v.resultados.forEach {
                Text(
                    text = (if (it.aprobada) "✔ " else "✖ ") + it.detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}