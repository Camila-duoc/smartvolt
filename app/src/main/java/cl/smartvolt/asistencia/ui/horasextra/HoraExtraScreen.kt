package cl.smartvolt.asistencia.ui.horasextra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoraExtraScreen(
    onNavUp: () -> Unit,
    vm: HoraExtraViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    var horas by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var diasAtras by remember { mutableStateOf(0f) }
    val fechaSeleccionada = remember(diasAtras) {
        System.currentTimeMillis() - (diasAtras * 86_400_000L).toLong()
    }

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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Solicitud de horas extra", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = horas,
                onValueChange = { horas = it },
                label = { Text("Horas solicitadas") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = motivo,
                onValueChange = { motivo = it },
                label = { Text("Motivo / justificación") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("Fecha: $fechaTxt", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (diasAtras == 0f) "Hoy" else "Hace ${diasAtras.toInt()} día(s)",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = diasAtras,
                onValueChange = { diasAtras = it },
                valueRange = 0f..30f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val h = horas.toDoubleOrNull() ?: 0.0
                    vm.solicitarHoraExtra(null, fechaSeleccionada, h, motivo)
                },
                enabled = !ui.cargando && horas.isNotBlank() && motivo.isNotBlank(),
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
