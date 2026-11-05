# Smart-Volt Asistencia

Aplicación Android de control de asistencia en faena, escrita en **Kotlin + Jetpack Compose + Room + Hilt**. Funciona offline-first: las marcas se validan con reglas antifraude en el dispositivo, se guardan en Room y se sincronizan después con WorkManager.

Todos los datos son **ficticios**, pensados para una demo académica sin hardware real.

> Documentación detallada del proyecto y de cada archivo: [DOCUMENTACION.md](DOCUMENTACION.md).
> Ruta de estudio y mapa de cambios: [RECORRIDO.md](RECORRIDO.md).
> Cómo conectar el emulador o un celular: [CONECTAR_CELULAR.md](CONECTAR_CELULAR.md).
> Cómo poner el logo de Figma como icono de la app: [ICONO.md](ICONO.md).

## Stack

- Kotlin 2.0.20, Jetpack Compose (BOM 2024.09.00) y Material 3
- Room 2.6.1 con KSP
- Hilt 2.52 (inyección de dependencias, incluido `HiltWorker`)
- WorkManager 2.9.1 para la sincronización
- Retrofit 2.11.0 + kotlinx.serialization para la API
- Navigation Compose 2.8.0
- AGP 8.5.2 / Gradle 8.9 (según `gradle-wrapper.properties`)
- `compileSdk` y `targetSdk` 35, `minSdk` 26, Java 17

## Variantes de compilación (flavors)

| Flavor | Sensores GPS/BLE | API | Uso |
| --- | --- | --- | --- |
| `demo` | Simulados (`FakeSensors`) | Simulada (`FakeApiService`, acepta todo) | Probar y presentar sin hardware ni backend |
| `campo` | Reales (Fused Location + escáner BLE) | Retrofit contra `API_BASE_URL` | Pruebas en terreno con backend real |

En Android Studio elige la variante en **Build Variants** (por ejemplo `demoDebug`). En `demo` no se piden permisos de ubicación ni Bluetooth, y la pantalla de marcar muestra chips para elegir el escenario simulado.

## Requisitos

- Android Studio reciente con JDK 17 (el JBR incluido sirve).
- SDK de Android 35 instalado.
- Emulador o dispositivo con Android 8.0 (API 26) o superior.
- `local.properties` con la ruta del SDK (Android Studio lo genera al abrir el proyecto).

Abre la carpeta en Android Studio, espera el *Gradle Sync* y pulsa **Run**.

## Acceso demo

El PIN de todos los usuarios de la semilla es **1234**.

| Perfil | Código de ejemplo | Pantalla inicial |
| --- | --- | --- |
| Operario | `OP-0001` | Marcar asistencia |
| Capataz | `CAP-0001`, `CAP-0002` | Panel de presencia de su faena |
| Administración | `ADM-0001` | Resumen administrativo |

La semilla (`assets/seed.json`) incluye 4 faenas, 11 frentes, 11 beacons, 27 usuarios y 122 marcas, y se carga la primera vez que se abre la pantalla de login.

## Funcionalidades

### Operario
- **Marcar entrada y salida** con validación automática: cada marca pasa por el motor de reglas antifraude (`RuleEngine`) y muestra el veredicto (VÁLIDA, SOSPECHOSA o RECHAZADA) con el detalle de cada regla.
- **Reglas**: ubicación simulada (mock location), secuencia entrada/salida, geocerca, beacon autorizado, presencia física, modo desarrollador y reloj.
- **Cadena de hash** por marca (`HashMarca`) para detectar alteraciones, con verificación de integridad y encadenamiento.
- **Mi historial** (calendario) y **mapa** de la faena.
- **Horas extra**: solicitar y consultar el estado de las propias solicitudes, con validación por campo, tope de 12 h y motivo mínimo de 10 caracteres.
- Contador de marcas pendientes de envío.
- **Notificación local** cuando una marca queda sospechosa y requiere visación.

### Capataz
- Panel con la última marca de cada operario de su faena.
- **Visación** de marcas sospechosas (aprobar o rechazar), con el detalle de la evidencia.
- **Aprobación de horas extra**, calendario y mapa.

### Administración
- Resumen de marcas (total, válidas, sospechosas, rechazadas, pendientes de sincronizar) y por faena, usuario y estado.
- **Marcas no válidas por faena** (porcentaje de sospechosas y rechazadas sobre el total de cada faena).
- **Alertas recientes** en solo lectura (últimas marcas sospechosas o rechazadas).
- Resumen de horas extra (solicitudes, pendientes, aprobadas y horas aprobadas, rechazadas).
- **Exportación** a CSV (todas las marcas) y a PDF (resumen), compartidos con el selector de Android mediante `FileProvider`.
- **Contadores animados** y aparición animada de las alertas.
- **Verificación de la cadena de hashes**: indica si el registro es íntegro y cuántas marcas de la semilla no son verificables.
- **Selección de la ubicación del mapa**: el administrador toca el mapa y guarda el punto que verán operarios y capataces.

### Interfaz y experiencia
- Tema **Material 3** con tipografía, formas y **modo oscuro** (sigue el ajuste del sistema).
- **Transiciones animadas** entre pantallas y aparición animada del resultado de la marca.
- **Notificaciones locales**: marca sospechosa pendiente de visación y sincronización completada (canales `smartvolt_alertas` y `smartvolt_sync`).
- **Formularios validados** en login y horas extra, con íconos y mensajes de error junto a cada campo mientras se escribe. Las reglas viven en `ValidarLoginUseCase` y `SolicitarHoraExtraUseCase`, no en la pantalla.

## Mapa de la faena

La pantalla **Ver mapa** usa el **Mapbox Maps SDK** con la extensión **maps-compose** (la misma combinación del ejemplo `GeoMapbox` del profesor). Muestra un marcador y una tarjeta con el radio autorizado.

- **Administración** puede tocar el mapa y pulsar **Guardar ubicación para todos**; el punto queda en Room.
- **Operarios y capataces** ven ese mismo punto en modo solo lectura. Si administración todavía no elige uno, se usa el centro del perímetro de la faena.

- Dependencias: `com.mapbox.maps:android-ndk27:11.16.2` y `com.mapbox.extension:maps-compose-ndk27:11.16.2` (declaradas en `gradle/libs.versions.toml`).
- Repositorio Maven de Mapbox: `https://api.mapbox.com/downloads/v2/releases/maven` (en `settings.gradle.kts`).
- Token público: `app/src/main/res/values/mapbox_access_token.xml`.
- El mapa necesita **internet** para descargar los mosaicos; si no hay red, el resto de la app sigue funcionando.

## Sincronización

`SyncScheduler` encola un `SyncWorker` (WorkManager, con restricción de red conectada y reintento exponencial) cada vez que se guarda una marca. El worker envía las marcas pendientes y luego intenta actualizar la configuración. En `demo` la API es simulada, así que las marcas pasan a ENVIADA sin backend. Al terminar, si se enviaron marcas, muestra una notificación local de sincronización completada. Para probar el modo offline, activa el modo avión, marca y vuelve a conectar.

## Estructura del proyecto

```text
app/src/main/java/cl/smartvolt/asistencia/
├── MainActivity.kt, SmartVoltApp.kt
├── data/
│   ├── local/        # Entities.kt, Daos.kt (Room)
│   ├── remote/       # Api.kt (Retrofit + DTOs), FakeApi.kt (API de demo)
│   └── repository/   # AttendanceRepository + RoomAttendanceRepository
├── di/               # AppModule.kt (Hilt)
├── notifications/    # Notificaciones.kt (canales, marca sospechosa, sync)
├── domain/
│   ├── antifraude/   # Rules.kt, RuleEngine.kt
│   ├── geo/          # GeoUtils.kt
│   ├── model/        # Models.kt
│   └── usecase/      # RegistrarMarca, HashMarca, SolicitarHoraExtra, ValidarLogin, ExportarMarcasCsv, ExportarReportePdf
├── sensors/          # GPS/BLE reales y simulados
├── sync/             # SyncWorker y SyncScheduler
└── ui/               # login, checkin, foreman, visacion, admin, horasextra, calendario, mapa, navigation, theme
```

## Pruebas

Las pruebas unitarias están en `app/src/test`. Desde Android Studio, ejecuta la carpeta `test` o la tarea Gradle `testDemoDebugUnitTest`. Cubren:

- Geometría y perímetros (`GeoUtils`).
- Reglas antifraude (`RuleEngine`).
- Integridad del hash de marcas (`HashMarca`): hash correcto, detección de alteraciones, enlaces rotos y marcas sin hash.
- Horas extra (cálculo, validaciones y ViewModels).
- Calendario, visación y exportación CSV.
- Métricas de administración, alertas y resumen para PDF.
- API simulada de demo (`FakeApiService`).

## Limitaciones conocidas

- No hay backend real: en `demo` la API es simulada y en `campo` se espera un servidor en `API_BASE_URL`.
- La autenticación es de demostración (PIN fijo) y la sesión vive en memoria.
- La base de datos no está cifrada todavía (SQLCipher pendiente, RNF-08).
- El reloj de referencia no se ancla a la hora del servidor (pendiente).
- El PDF es un resumen de texto simple; el CSV incluye todas las marcas.
- Los datos son sintéticos y no representan personas reales.

## Reiniciar la demo

Para volver a cargar la semilla, borra los datos de la app desde Ajustes de Android (**Almacenamiento → Borrar datos**) o desinstala y vuelve a instalar.
