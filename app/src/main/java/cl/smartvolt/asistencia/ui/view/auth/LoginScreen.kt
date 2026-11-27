package cl.smartvolt.asistencia.ui.view.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.smartvolt.asistencia.domain.usecase.ValidarLoginUseCase
import cl.smartvolt.asistencia.modelo.Rol
import cl.smartvolt.asistencia.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLogin: (Rol) -> Unit,
    onNavigateToRegister: () -> Unit = {},
    vm: LoginViewModel = hiltViewModel()
) {
    var id by rememberSaveable { mutableStateOf("OP-0001") }
    var pin by rememberSaveable { mutableStateOf("1234") }
    var intentoEnvio by rememberSaveable { mutableStateOf(false) }
    var tocadoId by rememberSaveable { mutableStateOf(false) }
    var tocadoPin by rememberSaveable { mutableStateOf(false) }
    var pinVisible by remember { mutableStateOf(false) }

    val ui by vm.ui.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // La validación vive en el dominio; aquí solo se sincroniza con lo que se escribe.
    LaunchedEffect(id, pin) { vm.validar(id, pin) }

    val mostrarIdError = (intentoEnvio || tocadoId) && ui.idError != null
    val mostrarPinError = (intentoEnvio || tocadoPin) && ui.pinError != null

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Iniciar Sesión",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Smart-Volt Asistencia · Datos ficticios",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 1. Campo Código de Usuario (Lógica original + tu estilo visual)
            OutlinedTextField(
                value = id,
                onValueChange = {
                    id = it.uppercase()
                    tocadoId = true
                    vm.limpiarError()
                },
                label = { Text("Código de usuario") },
                placeholder = { Text("OP-0001, CAP-0001 o ADM-0001") },
                leadingIcon = {
                    Icon(Icons.Default.Badge, contentDescription = null)
                },
                trailingIcon = if (mostrarIdError) {
                    { Icon(Icons.Default.ErrorOutline, contentDescription = "Error en el código") }
                } else {
                    null
                },
                singleLine = true,
                isError = mostrarIdError,
                supportingText = {
                    if (mostrarIdError) {
                        Text(
                            text = ui.idError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = coloresCampo,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Campo PIN (Lógica original + ojito de visibilidad + tu estilo visual)
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter(Char::isDigit).take(ValidarLoginUseCase.LARGO_PIN)
                    tocadoPin = true
                    vm.limpiarError()
                },
                label = { Text("PIN") },
                placeholder = { Text("Ingresa tu PIN") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    val icono = if (pinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val descripcion = if (pinVisible) "Ocultar PIN" else "Mostrar PIN"
                    IconButton(onClick = { pinVisible = !pinVisible }) {
                        Icon(imageVector = icono, contentDescription = descripcion)
                    }
                },
                singleLine = true,
                isError = mostrarPinError,
                supportingText = {
                    if (mostrarPinError) {
                        Text(
                            text = ui.pinError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                },
                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                colors = coloresCampo,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Mensaje de error general del backend/dominio
            AnimatedVisibility(
                visible = ui.errorGeneral != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ui.errorGeneral.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Único de Ingreso (Tu diseño + validación original)
            Button(
                onClick = {
                    keyboardController?.hide()
                    intentoEnvio = true
                    tocadoId = true
                    tocadoPin = true
                    vm.ingresar(id, pin, onLogin)
                },
                enabled = ui.formularioValido,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Ingresar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enlace listo para conectar la pantalla de Registro cuando la migres
            TextButton (
                onClick = { onNavigateToRegister() }
            ) {
                Text(
                    text = "¿No tienes cuenta? Regístrate",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}