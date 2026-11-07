# Guion de presentación — Smart-Volt Asistencia

**Duración objetivo:** 16 minutos + preguntas.
**Variante para presentar:** `demoDebug` (ícono **Smart-Volt Demo**).
**Credenciales:** `OP-0001`, `CAP-0001`, `ADM-0001` · PIN `1234`.

---

## Antes de empezar (checklist de 5 minutos)

- [ ] Abrir **Smart-Volt Demo** (no la app de campo).
- [ ] Confirmar que se ven los chips demo (`VALIDA`, `FAKE_GPS`, …). Si no aparecen, deslizar hacia abajo.
- [ ] Dejar el escenario en `VALIDA` al inicio.
- [ ] Permitir notificaciones en el teléfono.
- [ ] Tener internet para el mapa (Mapbox) o avisar que el mapa necesita red.
- [ ] Abrir en pestañas: GitHub del proyecto, tablero de Trello y Android Studio.
- [ ] Tener a mano capturas/video de respaldo por si falla la demo en vivo.
- [ ] No actualizar la app ni borrar datos antes de presentar.

---

## Estructura y tiempos

| Minutos | Bloque | Ítem de la pauta |
|---|---|---|
| 0:00–0:45 | Apertura: problema y roles | Contexto |
| 0:45–3:15 | Interfaz, jerarquía y navegación | Ítem 1 |
| 3:15–5:45 | Formularios y validación visual | Ítem 2 |
| 5:45–7:30 | Validación centralizada y estado | Ítem 3 |
| 7:30–9:00 | Animaciones funcionales | Ítem 4 |
| 9:00–11:15 | Modularidad y persistencia local | Ítem 5 |
| 11:15–13:15 | Recursos del dispositivo | Ítem 7 |
| 13:15–15:00 | GitHub y Trello | Ítem 6 |
| 15:00–15:45 | Cierre y preguntas | — |

---

## 0. Apertura — 0:00 a 0:45

**Di:**
> “Hola, presento Smart-Volt Asistencia, una app Android para registrar entrada y salida en faena. El problema no es solo marcar: es **confiar** en la marca. En terreno existe fake GPS, gente que marca desde su casa o fuera del perímetro. Por eso cada marca se valida con reglas antifraude, se guarda en el teléfono y se sincroniza cuando hay red. La app tiene tres roles: operario, capataz y administración.”

**Muestra:** ícono **Smart-Volt Demo** → pantalla de login.

**Código:**
- `app/src/main/java/.../MainActivity.kt` → `setContent { SmartVoltTheme { AppNav() } }`.
- `README.md` → resumen del stack y credenciales.

---

## 1. Interfaz estructurada, jerárquica y navegación — 0:45 a 3:15

**Di:**
> “La interfaz está hecha en Jetpack Compose con Material 3. Tengo un tema propio: azul institucional, y colores semánticos: verde para válida, ámbar para sospechosa y rojo para rechazada. La tipografía tiene jerarquía clara: título de pantalla, títulos de sección, cuerpo y texto auxiliar. También hay modo oscuro automático según el sistema.”

> “Todas las pantallas comparten la misma estructura: `Scaffold`, barra superior con título y acciones, y el contenido organizado en tarjetas y secciones. Eso hace que el usuario no tenga que reaprender la app en cada vista.”

> “La navegación es un solo `NavHost` con rutas por rol. El login decide el destino: operario → marcar asistencia, capataz → panel de presencia, administración → resumen. Las transiciones son slide + fade de 320 ms, y al volver se usa la transición inversa. Los botones de marcar miden 88 dp para poder usarlos con guantes, y los íconos tienen `contentDescription` para accesibilidad.”

**Muestra:**
1. Login con `OP-0001` / `1234`.
2. Pantalla “Marcar asistencia”.
3. Abrir “Ver mapa” y volver.
4. Abrir “Mi historial” (calendario) y volver.
5. Opcional: cambiar el teléfono a modo oscuro.

**Código:**
- `ui/theme/Theme.kt` → colores (19–28), tipografía y formas (73–143), tema (146–154).
- `ui/navigation/AppNav.kt` → rutas y transiciones (34–93).
- `ui/view/CheckInScreen.kt` → barra superior y botones grandes (69–93).
- `ui/view/CalendarioScreen.kt` → grilla mensual, leyenda y detalle del día (52–204, 245–384).

**Idea fuerza:** “La jerarquía visual y la navegación coherente reducen la carga cognitiva; el usuario siempre sabe dónde está y cuál es la acción principal.”

---

## 2. Formularios y validación visual por campo — 3:15 a 5:45

**Di:**
> “Los formularios de captura de datos son dos: login y solicitud de horas extra. Los dos validan **campo por campo**, no solo al final.”

**Demo en vivo (login):**
1. Escribir `OP` en el código → aparece el error: “Formato esperado: OP-0001, CAP-0001 o ADM-0001”.
2. Escribir `12` en el PIN → “El PIN debe tener 4 dígitos”.
3. Mostrar que el botón **INGRESAR** está deshabilitado.
4. Corregir a `OP-0001` / `1234` e ingresar.

**Demo en vivo (horas extra):**
1. Ir a “Mis horas extra” → “Solicitar horas extra”.
2. Escribir `13` → “Máximo 12 h por solicitud”.
3. Escribir un motivo de 3 letras → “Mínimo 10 caracteres (llevas 3)”.
4. Corregir a `2.5` y un motivo real → aparece el mensaje de éxito con ícono de check.
5. Volver.

**Di:**
> “Cada campo tiene un ícono de referencia: credencial, candado, reloj y notas. Cuando hay error, el campo cambia con `isError`, aparece el mensaje en `supportingText` y se agrega el ícono `ErrorOutline` a la derecha. El botón se deshabilita mientras el formulario no es válido. Los mensajes generales usan `ErrorOutline` o `CheckCircle` y se animan al aparecer.”

> “El login también tiene un error general de autenticación: ‘Código o PIN incorrecto’, separado de los errores de formato de cada campo.”

**Código:**
- `viewmodel/LoginViewModel.kt` → estado `LoginUi` (26–32) y validación (46–49).
- `ui/view/LoginScreen.kt` → validación reactiva (36), campos e íconos (55–101), error general animado (103–125), botón (128–139).
- `ui/view/HoraExtraScreen.kt` → validación por campo (56–68), campos (89–130), botón (147–160), éxito/error animados (162–211).
- `domain/usecase/ValidarLoginUseCase.kt` → reglas (19–42).
- `domain/usecase/SolicitarHoraExtraUseCase.kt` → reglas (9–47, 50–64).

**Idea fuerza:** “La validación no es un mensaje genérico: es un error puntual, junto al campo que lo produce, con ícono y texto.”

---

## 3. Validación centralizada y UI reactiva — 5:45 a 7:30

**Di:**
> “La validación no vive en la pantalla. Vive en casos de uso del dominio: `ValidarLoginUseCase` y `SolicitarHoraExtraUseCase`. La pantalla solo decide cuándo mostrar el error y cómo pintarlo.”

> “El flujo es: el usuario escribe → `LaunchedEffect` llama al ViewModel → el ViewModel llama al caso de uso → el caso de uso devuelve errores por campo → el ViewModel publica un `StateFlow` → Compose lo observa con `collectAsState` y se recompone solo. No hay `if` de validación regados por la UI.”

> “Además, el caso de uso **revalida** dentro de `invoke` antes de guardar. Así, aunque otra parte del código llame al repositorio, no puede saltarse la regla. Eso es defensa en profundidad.”

**Muestra:**
- Abrir `ValidarLoginUseCase.kt` y señalar la regex y el largo del PIN.
- Abrir `viewmodel/LoginViewModel.kt` y mostrar `validar()`.
- Opcional: correr `ValidarLoginUseCaseTest` y `SolicitarHoraExtraUseCaseTest` en verde.

**Código:**
- `viewmodel/LoginViewModel.kt` → `validar()` (46–49) e `ingresar()` (58–74).
- `ui/view/LoginScreen.kt` → `LaunchedEffect` (36).
- `domain/usecase/ValidarLoginUseCase.kt` (19–42).
- `domain/usecase/SolicitarHoraExtraUseCase.kt` → `validarHorasTexto`, `validarHoras`, `validarMotivo` y revalidación en `invoke` (24–78).
- `viewmodel/HoraExtraViewModel.kt` → estado y llamada al caso de uso (67–85).
- Tests: `app/src/test/java/.../ValidarLoginUseCaseTest.kt`, `SolicitarHoraExtraUseCaseTest.kt`.

**Idea fuerza:** “La UI es tonta y el dominio es testeable. Eso permite cambiar una regla sin tocar la pantalla.”

---

## 4. Animaciones funcionales — 7:30 a 9:00

**Di:**
> “Las animaciones no son decoración; tienen una función: orientar, dar feedback y suavizar los cambios de estado.”

- **Navegación:** slide horizontal + fade de 320 ms; salida de 180 ms.
- **Marcar asistencia:** barra de progreso mientras valida; el veredicto aparece con `AnimatedContent` (fade + slide hacia arriba); la tarjeta crece con `animateContentSize`.
- **Login:** el error general aparece con fade.
- **Horas extra:** éxito y error se expanden y contraen.
- **Administración:** los contadores se animan con `animateIntAsState` (700 ms) y las alertas se expanden.

**Demo en vivo:**
1. En “Marcar asistencia”, con `VALIDA`, marcar entrada → ver el veredicto animado.
2. Cambiar el chip a `FAKE_GPS` y marcar → veredicto sospechoso + notificación.
3. Entrar como `ADM-0001` → ver contadores y alertas animadas.

**Código:**
- `ui/navigation/AppNav.kt` (40–49).
- `ui/view/CheckInScreen.kt` → carga, error, veredicto y tarjeta (94–122, 157–167).
- `ui/view/LoginScreen.kt` (103–125).
- `ui/view/HoraExtraScreen.kt` (162–211).
- `ui/view/AdminScreen.kt` → alertas (205–217) y contadores (293–301).

**Idea fuerza:** “Cada animación acompaña un cambio de estado real; ninguna existe solo para llamar la atención.”

---

## 5. Modularidad, separación de responsabilidades y persistencia — 9:00 a 11:15

**Di:**
> “El proyecto está separado en capas. `ui/view/` tiene las pantallas Compose y `ui/compoments/` los componentes reutilizables; `viewmodel/` tiene los ViewModels y la base Room; `modelo/` tiene modelos, entidades y DAOs; `domain/` tiene reglas, geometría, hash y casos de uso; `data/` tiene Retrofit/API simulada y el repositorio; `di/` configura Hilt; `sensors/` abstrae GPS y BLE; `sync/` tiene WorkManager; `notifications/` los canales locales.”

> “La UI no conoce Room ni Retrofit. Habla con el ViewModel, el ViewModel con el repositorio y el repositorio con los DAO o la API. Hilt inyecta las implementaciones. Los flavors `demo` y `campo` deciden si los sensores y la API son simulados o reales, sin cambiar las pantallas.”

> “Room es la fuente de verdad local: base `smartvolt.db`, versión 4, 10 entidades y migraciones 1→2, 2→3 y 3→4. La primera vez se carga `assets/seed.json` con faenas, frentes, beacons, usuarios y marcas. Cada marca nueva queda `PENDIENTE`; `SyncWorker` la envía con WorkManager cuando hay red y reintenta con backoff exponencial. Eso es offline-first.”

**Muestra:**
- Árbol de carpetas `modelo / viewmodel / ui / domain / data / di / sensors / sync`.
- `viewmodel/AppDatabase.kt` → `@Database`, versión 4 y migración 3→4.
- `AppModule.kt` → cómo se elige GPS/BLE/API real o simulado.
- `SyncWorker.kt` → restricción de red, reintento y notificación.
- Demo opcional: modo avión, marcar, ver “Marcas pendientes”; reconectar y ver la notificación de sincronización.

**Código:**
- `modelo/Entidades.kt` → tablas y relaciones.
- `viewmodel/AppDatabase.kt` → migraciones (24–72) y base de datos (74–97).
- `data/repository/AttendanceRepository.kt` → interfaz + implementación Room.
- `di/AppModule.kt` (26–64).
- `sync/SyncWorker.kt` (14–41).
- `domain/usecase/RegistrarMarcaUseCase.kt` → orquestación de sensores, reglas, hash y persistencia (21–63).
- Tests: `app/src/test/java/...` (16 archivos, incluidos `RuleEngineTest`, `HashMarcaTest`, `GeoUtilsTest`, `AdminViewModelTest`, `MapaViewModelTest`).

**Idea fuerza:** “Puedo cambiar la regla de negocio, el sensor o la base sin tocar la pantalla. Eso es mantenibilidad.”

---

## 6. Recursos del dispositivo — 11:15 a 13:15

**Di:**
> “La app accede a más de dos recursos nativos, y todos se piden en el momento de uso, no todos al abrir la app.”

1. **GPS real:** Fused Location Provider con respaldo de `LocationManager`. Se pide `ACCESS_FINE_LOCATION` antes de marcar. Código: `sensors/RealSensors.kt` (41–112) y `ui/view/CheckInScreen.kt` (49–65).
2. **Bluetooth LE:** escaneo de balizas iBeacon y RSSI. Se pide `BLUETOOTH_SCAN` con `neverForLocation`. Código: `sensors/RealSensors.kt` (125–147) y `AndroidManifest.xml` (9–13).
3. **Notificaciones locales:** canales de “alertas” y “sincronización”; aviso de marca sospechosa y de sync completada. Se pide `POST_NOTIFICATIONS` en Android 13+. Código: `notifications/Notificaciones.kt` (30–116).
4. **Archivos y compartir:** exportación CSV/PDF con `FileProvider` y el selector de apps del sistema. Código: `ui/view/AdminScreen.kt` (57–84, 325–362), `res/xml/file_paths.xml` y el provider del manifiesto (49–57).
5. **Mapbox:** SDK de mapas para mostrar la faena y la ubicación compartida.

**Di:**
> “En la variante `campo` se usan los sensores reales. En `demo` se inyectan versiones simuladas para que la presentación sea repetible y no dependa de hardware. El código real está en `RealSensors.kt`; el cambio se hace en `AppModule` según `BuildConfig.USE_FAKE_SENSORS`.”

> “La seguridad está en: permisos runtime, BLE con `neverForLocation`, `FileProvider` no exportado y con permiso temporal de lectura, y notificaciones que se omiten si el usuario no dio permiso.”

**Muestra:** manifiesto (permisos y provider), `RealSensors.kt`, `Notificaciones.kt`, y el selector de compartir del CSV/PDF en administración.

**Idea fuerza:** “Los recursos están integrados al flujo: el GPS y el BLE alimentan la validación; la notificación avisa; el archivo exporta evidencia.”

---

## 7. GitHub y Trello — 13:15 a 15:00

**Di:**
> “El proyecto está versionado en GitHub. La rama `main` es la estable; cada funcionalidad se trabajó en una rama y se integró con Pull Request. Hay commits de tres integrantes y dos merges de PR reales (#1 y #2).”

**Muestra en GitHub:**
- Repositorio: `https://github.com/Camila-duoc/smartvolt`.
- Ramas: `main`, `Cami_pauta`, `crissss`, `crissto`, `fix/revision-general`, `errores`, `criss_pp`.
- Commits representativos:
  - `b06d87a` — “Validación del login movida a la lógica e Iconos”.
  - `fc5c13a` / `127833c` — mapa y elección de ubicación.
  - `a804aa1` — icono.
  - `2c14439` — “solo info”.
- Pull Requests #1 y #2 (merges `82e7e38` y `f1a4dcc`).
- Contributors: Camila (9), criatop (8), Cristopher Campos (2) — según `git shortlog -sn --all`.

**Muestra en Trello:**
- Columnas: `Backlog` · `En curso` · `En revisión` · `Hecho`.
- Tarjetas con criterio de aceptación y checklist. Ejemplos que puedes dejar preparados:
  1. “Validación de login por campo” → commit `b06d87a`.
  2. “Ubicación compartida en el mapa” → commits `fc5c13a` / `127833c`.
  3. “Icono e identidad visual” → commit `a804aa1`.
  4. “Escenarios demo para presentación” → `CheckInScreen.kt` + `Sensors.kt`.

**Di:**
> “Cada tarjeta tiene su criterio de aceptación y se cierra cuando el commit o el PR está integrado. Así se ve la planificación y la trazabilidad entre Trello, GitHub y el código.”

> Si el repositorio tiene issues cerrados, muestra dos: “issue → commit → PR”. Si no, apóyate en las tarjetas de Trello y en los PR; no inventes issues que no existen.

**Idea fuerza:** “El trabajo no fue un solo commit gigante: hay ramas, autores distintos, PR y tareas planificadas.”

---

## 8. Cierre — 15:00 a 15:45

**Di:**
> “En resumen: Smart-Volt no confía solo en el GPS. Valida cada marca con reglas antifraude, la encadena con SHA-256 para detectar alteraciones, la guarda en Room y la sincroniza con WorkManager. La interfaz está en Compose con Material 3, validación por campo y estado reactivo; la arquitectura está separada en capas y el trabajo está trazado en GitHub y Trello. Gracias.”

**Cierre visual:** marcar entrada con `VALIDA` y dejar el veredicto en pantalla.

---

## Chuleta: pauta → archivo → línea

| Ítem de la pauta | Archivo principal | Líneas |
|---|---|---|
| Interfaz, jerarquía y navegación | `ui/theme/Theme.kt`, `ui/navigation/AppNav.kt` | 19–154, 34–93 |
| Formularios y validación visual | `ui/view/LoginScreen.kt`, `ui/view/HoraExtraScreen.kt` | 27–139, 40–211 |
| Validación centralizada | `domain/usecase/ValidarLoginUseCase.kt`, `SolicitarHoraExtraUseCase.kt` | 19–42, 24–78 |
| Estado reactivo | `viewmodel/LoginViewModel.kt`, `viewmodel/HoraExtraViewModel.kt` | 26–75, 67–85 |
| Animaciones | `AppNav.kt`, `CheckInScreen.kt`, `AdminScreen.kt` | 40–49, 94–122 y 157–167, 205–301 |
| Modularidad y persistencia | `modelo/Entidades.kt`, `viewmodel/AppDatabase.kt`, `AppModule.kt`, `SyncWorker.kt` | 1–95, 24–97, 26–64, 14–41 |
| Recursos del dispositivo | `sensors/RealSensors.kt`, `notifications/Notificaciones.kt`, `AndroidManifest.xml` | 41–147, 30–116 |
| GitHub / Trello | Repositorio, ramas, PR #1/#2, commits | — |

---

## Plan B si falla la demo

1. **Si no aparecen los chips demo:** estás en la variante `campo`. Cambia a `demoDebug` o abre **Smart-Volt Demo**.
2. **Si no hay red:** el mapa no carga, pero el resto sí. Muestra el mapa con captura y sigue con la demo offline.
3. **Si falla el login:** verifica que estés en `demo` (PIN `1234`) y que la semilla esté cargada.
4. **Si el teléfono se traba:** usa el video/capturas de respaldo y sigue el guion.
5. **Si te preguntan por el backend:** di la verdad: en `demo` la API es simulada (`FakeApiService`); en `campo` se usa Retrofit contra `API_BASE_URL`.

---

## Preguntas probables (respuestas cortas)

- **¿Dónde está la validación?** En los casos de uso del dominio; la UI solo muestra el estado. Tests: `ValidarLoginUseCaseTest` y `SolicitarHoraExtraUseCaseTest`.
- **¿Cómo evitan el fake GPS?** `RuleEngine` combina ubicación mock, geocerca, beacons BLE, secuencia entrada/salida, modo desarrollador y reloj.
- **¿Qué pasa sin internet?** Room guarda la marca como `PENDIENTE`; WorkManager sincroniza al volver la red y notifica.
- **¿Por qué dos flavors?** `demo` simula sensores/API para presentar sin hardware; `campo` usa GPS/BLE reales y Retrofit. El código de UI es el mismo.
- **¿Cómo se protege la integridad?** Cada marca tiene `hashPrevio` y `hash` SHA-256; `HashMarca.verificarCadena` detecta alteraciones.
- **¿La app es accesible?** Colores con contraste, texto de error además del color, `contentDescription` en íconos y botones de 88 dp.
- **¿La base está cifrada?** Todavía no; SQLCipher está documentado como mejora futura.
- **¿La sesión persiste?** No; vive en memoria (`SessionState`). Es una limitación conocida del MVP.
