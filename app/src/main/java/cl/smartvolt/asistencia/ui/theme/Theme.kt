package cl.smartvolt.asistencia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF1F3864)
val Ok = Color(0xFF388E3C)
val Warn = Color(0xFFE6A000)
val Bad = Color(0xFFC0392B)

// Alto contraste para uso en exteriores (RNF-20).
private val Scheme = lightColorScheme(primary = Navy, secondary = Color(0xFF2E75B6), error = Bad)

@Composable
fun SmartVoltTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)
