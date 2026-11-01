package cl.smartvolt.asistencia.ui.horasextra

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.ui.theme.*

@Composable
fun MisHorasExtraScreen(
    onNavUp: () -> Unit,
    vm: MisHorasExtraViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    vm.cargarMisHorasExtra()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis horas extra") },
                navigationIcon = { IconButton(onClick = onNavUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            if (ui.cargando && ui.horasExtra.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                ResumenHorasExtraScreen(ui.horasExtra, onNavUp)

                ui.error?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
