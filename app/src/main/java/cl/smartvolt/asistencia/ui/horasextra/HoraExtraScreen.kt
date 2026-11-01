package cl.smartvolt.asistencia.ui.horasextra

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.ui.login.SessionState
import cl.smartvolt.asistencia.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun HoraExtraScreen(
    onNavUp: () -> Unit,
    vm: HoraExtraViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    var horas by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var fechaSeleccionada by remember { mutableStateOf(System.currentTimeMillis()) }

    val fechaTxt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fechaSeleccionada))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitar horas extra") },
                navigationIcon = { IconButton(onClick = onNavUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            Text("Solicitud de horas extra", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = horas,
                onValueChange = { horas = it },
                label = { Text("Horas solicitadas") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.foundation.text.KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = motivo,
                onValueChange = { motivo = it },
                label = { Text("Motivo / justificación") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("Fecha: $fechaTxt", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = (fechaSeleccionada / 86400000.0).roundToInt().toFloat(),
                onValueChange = {
                    // Slider aproximado para demo; en producción usar DatePicker
                    fechaSeleccionada = it.toLong() * 86400000L
                },
                valueRange = 0f..30f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val h = horas.toDoubleOrNull() ?: 0.0
                    vm.solicitar(null, fechaSeleccionada, h, motivo)
                },
                enabled = ui.cargando.not() && horas.isNotBlank() && motivo.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.cargando) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Solicitar horas extra")
            }

            ui.error?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (ui.exito) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Solicitud enviada correctamente", color = Ok, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
