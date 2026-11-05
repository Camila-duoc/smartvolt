package cl.smartvolt.asistencia.ui.horasextra

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.smartvolt.asistencia.domain.usecase.SolicitarHoraExtraUseCase
import cl.smartvolt.asistencia.domain.validation.ValidadorHoraExtra
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
    var tocadoHoras by remember { mutableStateOf(false) }
    var tocadoMotivo by remember { mutableStateOf(false) }

    val fechaSeleccionada = remember(diasAtras) {
        System.currentTimeMillis() - (diasAtras * 86_400_000L).toLong()
    }
    val fechaTxt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fechaSeleccionada))

    val horasNum = horas.replace(',', '.').toDoubleOrNull()
    val horasError = ValidadorHoraExtra.horas(horas)
    val motivoLimpio = motivo.trim()
    val motivoError = ValidadorHoraExtra.motivo(motivo)
    val formularioValido = ValidadorHoraExtra.formularioValido(horas, motivo)
    val mostrarHorasError = tocadoHoras && horasError != null
    val mostrarMotivoError = tocadoMotivo && motivoError != null

    val horasSupport: (@Composable () -> Unit)? =
        if (mostrarHorasError) ({ TextoError(horasError.orEmpty()) }) else null
    val motivoSupport: (@Composable () -> Unit)? =
        if (mostrarMotivoError) ({ TextoError(motivoError.orEmpty()) }) else null

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
            Text(
                "Tope: ${ValidadorHoraExtra.maxHorasTexto} h · motivo mínimo ${SolicitarHoraExtraUseCase.MIN_MOTIVO} caracteres",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = horas,
                onValueChange = {
                    horas = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5)
                    tocadoHoras = true
                },
                label = { Text("Horas solicitadas") },
                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = "Horas") },
                isError = mostrarHorasError,
                supportingText = horasSupport,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = motivo,
                onValueChange = {
                    motivo = it.take(200)
                    tocadoMotivo = true
                },
                label = { Text("Motivo / justificación") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = "Motivo") },
                isError = mostrarMotivoError,
                supportingText = motivoSupport,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
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
                    tocadoHoras = true
                    tocadoMotivo = true
                    if (formularioValido) {
                        vm.solicitarHoraExtra(null, fechaSeleccionada, horasNum ?: 0.0, motivoLimpio)
                    }
                },
                enabled = !ui.cargando && formularioValido,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.cargando) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Solicitar horas extra")
            }

            AnimatedVisibility(
                visible = ui.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(
                    text = ui.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            AnimatedVisibility(
                visible = ui.exito,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Solicitud enviada correctamente", color = Ok, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onNavUp, modifier = Modifier.fillMaxWidth()) {
                        Text("Volver a mis horas extra")
                    }
                }
            }
        }
    }
}

/** Mensaje de error de campo con ícono, consistente con el formulario de login. */
@Composable
private fun TextoError(mensaje: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(mensaje, color = MaterialTheme.colorScheme.error)
    }
}
