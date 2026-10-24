# Smart-Volt Asistencia · Esqueleto del MVP (DSY1105)

Aplicación Android (Kotlin · Jetpack Compose · Material 3 · MVVM · Room · Retrofit · Hilt · WorkManager) para
control de asistencia antifraude con modo offline. Corresponde a la **Propuesta B simplificada** del documento
"Propuestas de Diseño" (motor de reglas + sensores abstraídos), en un solo módulo Gradle.

> **Estado:** compila y las pruebas unitarias pasan (`./gradlew :app:testDemoDebugUnitTest`). El wrapper de
> Gradle está incluido en el repositorio, así que no hay que instalarlo aparte. Las pruebas de `GeoUtils` y
> `RuleEngine` no dependen de Android.

## Cómo abrirlo
1. Android Studio (versión estable reciente) → **Open** → carpeta `SmartVoltAsistencia`.
2. Deja que haga *Gradle Sync* (el wrapper está incluido: Gradle 8.9). Si algún número de versión falla,
   ajústalo en `gradle/libs.versions.toml`.
3. Elige la variante **demoDebug** (sensores simulados) y ejecuta en emulador o teléfono.
4. Ingreso de prueba (PIN `1234` para todos): `OP-0001` (operario), `CAP-0001` (capataz), `ADM-0001` (admin, pantalla pendiente).

## Variantes
| Variante | Sensores | Uso |
|---|---|---|
| `demo` | GPS/BLE/estado del sistema **simulados** (`FakeSensors.kt`) | Desarrollo y defensa sin hardware. Selector de escenarios en la pantalla de marcar. |
| `campo` | `FusedLocationProvider`, `BluetoothLeScanner`, `Settings.Global` reales | Pruebas en teléfono físico con beacons iBeacon. |

## Qué incluye
- `domain/geo/GeoUtils.kt`: Haversine, círculo y polígono (ray casting) con margen por precisión de GPS.
- `domain/antifraude/*`: 7 reglas (mock location, secuencia, geofence, beacon, presencia física, modo desarrollador, reloj) y `RuleEngine`.
- `domain/usecase/RegistrarMarcaUseCase.kt`: captura evidencia, evalúa, calcula hash encadenado y guarda en Room.
- `data/*`: Room (8 entidades + `TypeConverters` para enums), Retrofit (`ApiService`), repositorio offline-first y carga de `assets/seed.json` (datos sintéticos).
- `sync/SyncWorker.kt`: envío por lotes con reintentos y restricción de red.
- `ui/*`: Login, Marcar (operario), Panel de presencia con visación (capataz). Administración queda como TODO.
- `src/test`: pruebas de geofencing y de las reglas antifraude.

## Pendiente / siguientes pasos
- Pantallas de Administración, historial del operario y exportación (HU-ADM-*, HU-OPE-03).
- Autenticación real contra el backend (hoy PIN fijo de demostración).
- Ancla de hora del servidor en `TrustedClock` (RNF-07) y cifrado de Room con SQLCipher (RNF-08).
- Backend Spring Boot según `openapi.yaml` (o un mock: MockWebServer / JSON Server).
- Notificaciones locales y manejo de GPS/Bluetooth apagados con guía al usuario (RF-53, RF-56).

## Pruebas
`./gradlew :app:testDemoDebugUnitTest` (en Windows: `gradlew.bat :app:testDemoDebugUnitTest`), o ejecutar las clases de `src/test` desde Studio.

## Datos
Todos los datos son **ficticios/sintéticos** (Ley 21.719 y Ley 19.628). Los UUID de beacons se generan de forma determinista y no corresponden a hardware real.
