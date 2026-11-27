package cl.smartvolt.asistencia.ui.view.profile

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.smartvolt.asistencia.ui.theme.SmartVoltTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileView(
    emailInicial: String = "juan.perez@smartvolt.cl",
    telefonoInicial: String = "987654321",
    onGuardarContacto: (email: String, telefono: String) -> Unit = { _, _ -> },
    onVolver: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf(emailInicial) }
    var telefono by rememberSaveable { mutableStateOf(telefonoInicial) }
    var intento by rememberSaveable { mutableStateOf(false) }

    val errorEmail = if (email.isNotEmpty() || intento) validarEmailLocal(email) else null
    val errorTelefono = if (telefono.isNotEmpty() || intento) validarTelefonoLocal(telefono) else null

    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorLabelColor = MaterialTheme.colorScheme.error
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Editar datos de contacto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Campo Correo Electrónico
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                placeholder = { Text("ejemplo@constructora.cl") },
                singleLine = true,
                isError = errorEmail != null,
                supportingText = {
                    errorEmail?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = coloresCampo,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Campo Teléfono de Contacto
            OutlinedTextField(
                value = telefono,
                onValueChange = { input ->
                    telefono = input.filter(Char::isDigit).take(9)
                },
                label = { Text("Teléfono de contacto") },
                placeholder = { Text("912345678") },
                singleLine = true,
                isError = errorTelefono != null,
                supportingText = {
                    errorTelefono?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                colors = coloresCampo,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    intento = true
                    if (validarEmailLocal(email) == null && validarTelefonoLocal(telefono) == null) {
                        onGuardarContacto(email.trim(), telefono.trim())
                        onVolver()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar cambios", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun validarEmailLocal(email: String): String? {
    val limpio = email.trim()
    if (limpio.isEmpty()) return "El correo electrónico es obligatorio."
    if (!Patterns.EMAIL_ADDRESS.matcher(limpio).matches()) return "Ingresa un correo electrónico válido."
    return null
}

private fun validarTelefonoLocal(telefono: String): String? {
    val digitos = telefono.filter(Char::isDigit)
    if (digitos.isEmpty()) return "El teléfono es obligatorio."
    if (digitos.length != 9) return "Debe tener 9 dígitos (ej. 912345678)."
    return null
}

@Preview(showBackground = true)
@Composable
private fun EditProfileViewPreview() {
    SmartVoltTheme {
        EditProfileView()
    }
}