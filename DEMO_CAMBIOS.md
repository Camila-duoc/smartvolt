# Cambios de interfaz en vivo — recetas exactas

Guía para la defensa: si el profesor pide “cambia esto”, aquí está el archivo exacto, la línea y el efecto esperado.

> **Regla de oro:** haz **un cambio a la vez**, di qué archivo tocas y por qué, y vuelve a ejecutar. No edites dos cosas juntas.

---

## Antes de presentar (2 minutos)

1. Deja Android Studio con estas pestañas abiertas:
   - `ui/theme/Theme.kt`
   - `ui/navigation/AppNav.kt`
   - `ui/checkin/CheckInScreen.kt`
   - `ui/horasextra/HoraExtraScreen.kt`
   - `domain/usecase/SolicitarHoraExtraUseCase.kt`
   - `ui/mapa/MapaScreen.kt`
2. Confirma que la variante sea **`demoDebug`**.
3. Ten un punto de retorno:
   - **Opción fácil:** clic derecho en cada archivo → **Local History → Show History** (Android Studio guarda versiones).
   - **Opción Git:** antes de presentar, crea un commit del estado actual:
     ```bat
     git add -A
     git commit -m "estado previo a la presentacion"
     ```
     Y para volver: `git checkout -- <archivo>`.
4. Si el cambio es solo Kotlin, normalmente basta **Apply Changes**; si tocas XML o recursos, usa **Run ▶** completo.

---

## 1. Cambiar el color principal (10 segundos)

**Petición típica:** “cámbiame el azul por otro color”.

**Archivo:** `app/src/main/java/cl/smartvolt/asistencia/ui/theme/Theme.kt`

**Línea 19:**
```kotlin
val Navy = Color(0xFF1F3864)
```

**Cámbiala por, por ejemplo:**
```kotlin
val Navy = Color(0xFF00695C)   // verde petróleo
```
o
```kotlin
val Navy = Color(0xFF6A1B9A)   // morado
```

**Efecto:** cambia la barra superior, el botón de ingreso, “MARCAR SALIDA”, tarjetas y énfasis. Como el color está centralizado, no hay que tocar pantallas.

**Ojo:** el modo oscuro usa `NavyLight` (línea 25). Si quieres que también cambie ahí, edita esa línea.

**Qué decir:**
> “Como el tema está centralizado, cambio una sola constante y toda la app adopta el color.”

---

## 2. Forzar modo oscuro (5 segundos)

**Archivo:** `ui/theme/Theme.kt`

**Línea 147:**
```kotlin
darkTheme: Boolean = isSystemInDarkTheme(),
```

**Cámbiala por:**
```kotlin
darkTheme: Boolean = true,
```

**Efecto:** toda la app pasa a oscuro aunque el teléfono esté en claro.

**Para volver:** deja `isSystemInDarkTheme()`.

**Qué decir:**
> “El tema claro/oscuro está resuelto con dos `ColorScheme`; la app sigue el ajuste del sistema, pero puedo forzarlo para la demo.”

---

## 3. Cambiar los colores de estado (10 segundos)

**Archivo:** `ui/theme/Theme.kt`

**Líneas 20–22:**
```kotlin
val Ok = Color(0xFF388E3C)
val Warn = Color(0xFFE6A000)
val Bad = Color(0xFFC0392B)
```

**Ejemplo:** cambia `Ok` a azul:
```kotlin
val Ok = Color(0xFF1565C0)
```

**Efecto:** veredicto de la marca, tarjetas del panel, calendario y botón “MARCAR ENTRADA”.

**Qué decir:**
> “Los colores no son decorativos: verde, ámbar y rojo comunican válida, sospechosa y rechazada. Están centralizados para que el significado sea consistente en todas las pantallas.”

---

## 4. Cambiar la tipografía o el tamaño de los títulos (10 segundos)

**Archivo:** `ui/theme/Theme.kt`

**Líneas 74–78 (`headlineMedium`):**
```kotlin
headlineMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
),
```

**Cambia, por ejemplo:**
- `FontFamily.SansSerif` → `FontFamily.Serif`
- `fontSize = 28.sp` → `36.sp`

**Efecto:** se nota de inmediato en el título de login y en títulos de sección.

**Alternativa más global:** en `SmartVoltTypography`, cambia `FontFamily.SansSerif` por `FontFamily.Serif` en todos los estilos. Es más drástico, pero igual de reversible.

**Qué decir:**
> “La tipografía está definida una vez en el tema; las pantallas no eligen fuentes por su cuenta.”

---

## 5. Cambiar las formas (redondeado ↔ cuadrado) (10 segundos)

**Archivo:** `ui/theme/Theme.kt`

**Líneas 137–143:**
```kotlin
private val SmartVoltShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
```

**Ejemplo:** cambia `medium` y `large` a `RoundedCornerShape(0.dp)` para que tarjetas y botones se vean rectos.

**Efecto:** cambia la personalidad visual de toda la app sin tocar ninguna pantalla.

---

## 6. Hacer más lenta o más notoria la transición entre pantallas (10 segundos)

**Archivo:** `ui/navigation/AppNav.kt`

**Líneas 40–49:**
```kotlin
enterTransition = {
    slideInHorizontally(animationSpec = tween(320), initialOffsetX = { it / 5 }) +
        fadeIn(animationSpec = tween(320))
},
exitTransition = { fadeOut(animationSpec = tween(180)) },
popEnterTransition = { fadeIn(animationSpec = tween(320)) },
popExitTransition = {
    slideOutHorizontally(animationSpec = tween(320), targetOffsetX = { it / 5 }) +
        fadeOut(animationSpec = tween(180))
},
```

**Cambio vistoso:**
- `tween(320)` → `tween(900)`
- `it / 5` → `it` (la pantalla entra desde el borde completo)

**Efecto:** la navegación se siente mucho más lenta y明显的; ideal para mostrar que las transiciones están declaradas en un solo lugar.

**Para volver:** deja `tween(320)` y `it / 5`.

---

## 7. Agrandar o renombrar los botones de marcaje (10 segundos)

**Archivo:** `ui/checkin/CheckInScreen.kt`

**Líneas 142–145:**
```kotlin
Button(onClick = { vm.marcar(TipoMarca.IN) }, enabled = !ui.cargando && !sinPermisos,
    colors = ButtonDefaults.buttonColors(containerColor = Ok), modifier = Modifier.fillMaxWidth().height(88.dp)) { Text("MARCAR ENTRADA") }
Button(onClick = { vm.marcar(TipoMarca.OUT) }, enabled = !ui.cargando && !sinPermisos,
    modifier = Modifier.fillMaxWidth().height(88.dp)) { Text("MARCAR SALIDA") }
```

**Cambios seguros:**
- `height(88.dp)` → `height(120.dp)`
- `Text("MARCAR ENTRADA")` → `Text("REGISTRAR ENTRADA")`
- `Text("MARCAR SALIDA")` → `Text("REGISTRAR SALIDA")`

**Qué decir:**
> “Los botones son grandes a propósito: 88 dp para uso con guantes en faena. Si el profesor pide más grandes, es una sola línea.”

---

## 8. Cambiar una regla de validación y ver cómo la UI se adapta (30 segundos) ⭐

Esta es la demo **más valiosa** para la pauta de “validación centralizada”.

**Archivo:** `domain/usecase/SolicitarHoraExtraUseCase.kt`

**Líneas 11 y 14:**
```kotlin
const val MAX_HORAS = 12.0
const val MIN_MOTIVO = 10
```

**Cambia, por ejemplo:**
```kotlin
const val MAX_HORAS = 8.0
const val MIN_MOTIVO = 20
```

**Efecto automático:**
- La pantalla de horas extra muestra “Tope: 8 h · motivo mínimo 20 caracteres” porque usa `MAX_HORAS_TEXTO`.
- Si escribes 9 h, aparece “Máximo 8 h por solicitud”.
- Si el motivo tiene 15 caracteres, aparece “Mínimo 20 caracteres (llevas 15)”.

**No hay que tocar `HoraExtraScreen.kt`.** Eso demuestra que la regla vive en el dominio y la UI solo la refleja.

**Qué decir:**
> “Cambio el tope en el caso de uso y la pantalla se actualiza sola: no tengo la regla duplicada en la UI.”

---

## 9. Cambiar un texto visible (10 segundos)

**Importante:** en este proyecto **casi todos los textos están escritos directamente en las pantallas Compose**. `res/values/strings.xml` solo contiene `app_name`.

**Ejemplos:**
- Título del login: `ui/login/LoginScreen.kt`, línea 121:
  ```kotlin
  Text("Smart-Volt Asistencia", style = MaterialTheme.typography.headlineMedium)
  ```
- Subtítulo del login: línea 122:
  ```kotlin
  Text("Datos ficticios · uso académico", style = MaterialTheme.typography.bodySmall)
  ```
- Título de marcar: `ui/checkin/CheckInScreen.kt`, línea 123:
  ```kotlin
  title = { Text("Marcar asistencia · ${vm.usuarioId}") }
  ```
- Título de horas extra: `ui/horasextra/HoraExtraScreen.kt`, línea 72:
  ```kotlin
  title = { Text("Solicitar horas extra") }
  ```
- Nombre de la app en el lanzador:
  - `res/values/strings.xml` → `app_name`.
  - Override de la demo: `app/src/demo/res/values/strings.xml`.

**Qué decir:**
> “Los textos de pantalla están en el código Compose; el nombre de la app sí es un recurso, porque lo usa el manifiesto. Si quisiéramos internacionalizar, moveríamos estos textos a `strings.xml`.”

---

## 10. Cambiar el zoom del mapa (10 segundos)

**Archivo:** `ui/mapa/MapaScreen.kt`

**Línea 123:**
```kotlin
zoom(16.0)
```

**Cambia a:**
```kotlin
zoom(12.0)   // más alejado
```
o
```kotlin
zoom(18.0)   // más cerca
```

**Efecto:** el mapa se ve más amplio o más detallado. El punto y el marcador no cambian.

**Ojo:** el mapa necesita internet para los mosaicos. Si no hay red, no lo uses como demo principal.

**Qué decir:**
> “La cámara del mapa está encapsulada en `MapaFaena`; el resto de la pantalla no sabe cómo se configura Mapbox.”

---

## 11. Cambiar los colores del calendario (10 segundos)

**Archivo:** `ui/calendario/CalendarioScreen.kt`

**Líneas 386–391:**
```kotlin
private fun colorDeEstado(estado: EstadoDia): Color = when (estado) {
    EstadoDia.SIN_MARCAS -> MaterialTheme.colorScheme.outline
    EstadoDia.VALIDO -> Ok
    EstadoDia.SOSPECHOSO -> Warn
    EstadoDia.RECHAZADO -> Bad
}
```

**Ejemplo:** cambia `EstadoDia.VALIDO -> Ok` por:
```kotlin
EstadoDia.VALIDO -> MaterialTheme.colorScheme.tertiary
```

**Efecto:** los días válidos del calendario usan el color terciario del tema.

---

## 12. Agregar una pantalla “Acerca de” (3–4 minutos) — solo si piden algo grande

### Opción rápida: diálogo (1 minuto)

En `CheckInScreen.kt`, agrega un estado y un botón:

```kotlin
var mostrarAcerca by remember { mutableStateOf(false) }
```

Y después del botón “Ver mapa”:

```kotlin
OutlinedButton(
    onClick = { mostrarAcerca = true },
    modifier = Modifier.fillMaxWidth(),
) { Text("Acerca de") }

if (mostrarAcerca) {
    AlertDialog(
        onDismissRequest = { mostrarAcerca = false },
        title = { Text("Smart-Volt Asistencia") },
        text = { Text("Demo académica · Datos ficticios · 2026") },
        confirmButton = {
            TextButton(onClick = { mostrarAcerca = false }) { Text("Cerrar") }
        }
    )
}
```

### Opción completa: pantalla + ruta (3–4 minutos)

**Paso 1:** crea `ui/acerca/AcercaScreen.kt`:

```kotlin
package cl.smartvolt.asistencia.ui.acerca

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcercaScreen(onNavUp: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de") },
                navigationIcon = {
                    IconButton(onClick = onNavUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Smart-Volt Asistencia", style = MaterialTheme.typography.headlineSmall)
            Text("Demo académica · Datos ficticios", style = MaterialTheme.typography.bodyMedium)
            Text("Kotlin + Jetpack Compose + Room + Hilt", style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Paso 2:** en `ui/navigation/AppNav.kt`, agrega el import:

```kotlin
import cl.smartvolt.asistencia.ui.acerca.AcercaScreen
```

y la ruta antes del cierre del `NavHost`:

```kotlin
composable("acerca") {
    AcercaScreen(onNavUp = { nav.popBackStack() })
}
```

**Paso 3:** en `CheckInScreen.kt`, agrega el botón (ya tienes `nav` en la línea 120):

```kotlin
OutlinedButton(
    onClick = { nav.navigate("acerca") },
    modifier = Modifier.fillMaxWidth(),
) { Text("Acerca de") }
```

**Efecto:** nueva pantalla con transición y botón “Atrás”.

**Consejo:** si sabes que te lo van a pedir, **crea el archivo `AcercaScreen.kt` antes de la presentación** pero no lo conectes. Así en vivo solo agregas la ruta y el botón, y no pierdes tiempo escribiendo una pantalla completa.

---

## 13. Agregar un campo visual a un formulario (1–2 minutos)

**Petición típica:** “agrega un campo de observación”.

En `ui/horasextra/HoraExtraScreen.kt`, agrega:

```kotlin
var observacion by rememberSaveable { mutableStateOf("") }
```

y después del campo de motivo:

```kotlin
OutlinedTextField(
    value = observacion,
    onValueChange = { observacion = it.take(120) },
    label = { Text("Observación (opcional)") },
    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
    modifier = Modifier.fillMaxWidth(),
)
```

**Sé honesto:** esto solo agrega el campo visual. Para **persistirlo** habría que tocar:
`Entities.kt` → `Daos.kt` (migración) → `AttendanceRepository.kt` → `SolicitarHoraExtraUseCase.kt` → `HoraExtraViewModel.kt` → CSV/PDF → tests.

**Qué decir:**
> “Agregar el campo visual es rápido; persistirlo es un cambio de arquitectura porque atraviesa entidad, migración, repositorio y caso de uso.”

---

## 14. Qué NO cambiar en vivo (o cambiar solo si sobra tiempo)

| Cambio | Por qué evitarlo |
|---|---|
| Nueva regla antifraude | Toca `Rules.kt`, `RuleEngine`, tests y puede cambiar el veredicto de la demo. |
| Nuevo escenario demo | Toca `Sensors.kt`, `FakeSensors.kt` y la lógica de simulación. |
| Nueva entidad/tabla | Requiere migración de Room y recompilar; riesgo de perder datos. |
| Nuevo permiso | Requiere manifiesto, solicitud runtime y probar en dispositivo. |
| Cambiar el token de Mapbox | No aporta a la pauta y puede romper el mapa. |

---

## Orden recomendado si el profesor dice “hazme un cambio”

1. **Color principal** (`Theme.kt`, línea 19) — 10 s, impacto visual inmediato.
2. **Tope de horas** (`SolicitarHoraExtraUseCase.kt`, línea 11) — 30 s, demuestra arquitectura.
3. **Transición de navegación** (`AppNav.kt`, líneas 40–49) — 10 s, demuestra animaciones.
4. **Modo oscuro** (`Theme.kt`, línea 147) — 5 s, si piden “otra cosa”.
5. **Pantalla “Acerca de”** — solo si piden agregar una vista y hay tiempo.

---

## Frases para acompañar cada cambio

- **Color:** “El tema está centralizado; una constante cambia toda la app.”
- **Validación:** “La regla vive en el caso de uso, no en la pantalla; por eso la UI se actualiza sola.”
- **Animación:** “Las transiciones están declaradas en `AppNav`, no repetidas en cada pantalla.”
- **Nueva pantalla:** “Agregar una vista es crear el composable, registrar la ruta y agregar el acceso; la navegación ya maneja la transición y el botón atrás.”
- **Campo nuevo:** “La UI es fácil; lo importante es decidir si el dato se persiste, porque ahí entran Room, migraciones y repositorio.”

---

## Cómo revertir en 5 segundos

1. **Android Studio:** clic derecho en el archivo → **Local History → Show History** → elige la versión anterior → **Revert**.
2. **Git (si hiciste commit previo):**
   ```bat
   git checkout -- app/src/main/java/cl/smartvolt/asistencia/ui/theme/Theme.kt
   ```
3. **Manual:** Ctrl+Z y volver a ejecutar.

---

## Verificación rápida después de cada cambio

```bat
gradlew.bat :app:assembleDemoDebug
```
o simplemente **Run ▶** con `demoDebug`.

Si algo se ve raro:
1. `Build → Clean Project`.
2. `Run ▶`.
3. Si persiste, desinstala la app demo y vuelve a instalar.
