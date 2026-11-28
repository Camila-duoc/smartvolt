package cl.smartvolt.asistencia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EsquemaColorOscuro = darkColorScheme(
    primary = BotonOscuro,
    onPrimary = TextoBotonOscuro,
    primaryContainer = ContenedorPrimarioOscuro,
    onPrimaryContainer = TextoOscuro,
    secondary = BotonTextoOscuro,
    onSecondary = TextoBotonOscuro,
    secondaryContainer = SuperficieVarianteOscura,
    onSecondaryContainer = TextoOscuro,
    tertiary = BotonTextoOscuro,
    onTertiary = TextoBotonOscuro,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = TarjetaOscura,
    onSurface = TextoOscuro,
    surfaceVariant = SuperficieVarianteOscura,
    onSurfaceVariant = BotonTextoSecundarioOscuro,
    outline = BordeTarjetaOscuro,
    outlineVariant = BordeTarjetaOscuro,
    error = ColorError,
    onError = Color.White,
)

private val EsquemaColorClaro = lightColorScheme(
    primary = BotonClaro,
    onPrimary = Color.White,
    primaryContainer = ContenedorPrimarioClaro,
    onPrimaryContainer = TextoClaro,
    secondary = BotonTextoClaro,
    onSecondary = Color.White,
    secondaryContainer = SuperficieVarianteClara,
    onSecondaryContainer = TextoClaro,
    tertiary = BotonTextoClaro,
    onTertiary = Color.White,
    background = FondoClaro,
    onBackground = TextoClaro,
    surface = CasillaClara,
    onSurface = TextoClaro,
    surfaceVariant = SuperficieVarianteClara,
    onSurfaceVariant = TextoSecundarioClaro,
    outline = BordeTarjetaClaro,
    outlineVariant = BordeTarjetaClaro,
    error = ColorError,
    onError = Color.White,
)

/** Formas redondeadas compartidas. */
private val SmartVoltShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SmartVoltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) = MaterialTheme(
    colorScheme = if (darkTheme) EsquemaColorOscuro else EsquemaColorClaro,
    typography = Typography,
    shapes = SmartVoltShapes,
    content = content,
)