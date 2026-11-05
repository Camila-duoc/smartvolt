# Smart-Volt Asistencia

Aplicación Android de control de asistencia en faena, escrita en **Kotlin + Jetpack Compose + Room + Hilt**. Funciona offline-first: las marcas se validan con reglas antifraude en el dispositivo, se guardan en Room y se sincronizan después con WorkManager.

Todos los datos son **ficticios**, pensados para una demo académica sin hardware real.

## Integrantes

- Joaquín Rojas Labra
- Camila Cerdad
- Cristopher Campos

## Stack

- Kotlin 2.0.20, Jetpack Compose (BOM 2024.09.00) y Material 3
- Room 2.6.1 con KSP
- Hilt 2.52 (inyección de dependencias, incluido `HiltWorker`)
- WorkManager 2.9.1 para la sincronización
- Retrofit 2.11.0 + kotlinx.serialization para la API
- Navigation Compose 2.8.0
- Mapbox GL JS dentro de un WebView (token público `pk.`)
- AGP 8.5.2 / Gradle 8.9 (según `gradle-wrapper.properties`)
- `compileSdk` y `targetSdk` 35, `minSdk` 26, Java 17

## Variantes de compilación (flavors)

| Flavor | Sensores GPS/BLE | API | Uso |
| --- | --- | --- | --- |
| `demo` | Simulados (`FakeSensors`) | Simulada (`FakeApiService`, acepta todo) | Probar y presentar sin hardware ni backend |
| `campo` | Reales (Fused Location + respaldo `LocationManager` + escáner BLE) | Retrofit contra `API_BASE_URL` | Pruebas en terreno con backend real |

En Android Studio elige la variante en **Build Variants** (por ejemplo `demoDebug`). En `demo` no se piden permisos de ubicación ni Bluetooth, y la pantalla de marcar muestra chips para elegir el escenario simulado.

## Requisitos

- Android Studio reciente con JDK 17 (el JBR incluido sirve).
- SDK de Android 35 instalado.
- Emulador o dispositivo con Android 8.0 (API 26) o superior.
- `local.properties` con la ruta del SDK (Android Studio lo genera al abrir el proyecto).

Abre la carpeta en Android Studio, espera el *Gradle Sync* y pulsa **Run**.

## Acceso demo

El PIN de todos los usuarios de la semilla es **1234**. En la pantalla de login hay tres botones de **acceso rápido de prueba** que ingresan directamente con los usuarios demo.

| Perfil | Código | PIN | Faena | Pantalla inicial |
| --- | --- | --- | --- | --- |
| Operario | `OP-0001` | `1234` | F01 | Marcar asistencia |
| Operario (otra faena) | `OP-0002` | `1234` | F02 | Marcar asistencia |
| Capataz | `CAP-0001` | `1234` | F01 | Panel de presencia de F01 |
| Capataz (otra faena) | `CAP-0002` | `1234` | F02 | Panel de presencia de F02 |
| Administración | `ADM-0001` | `1234` | F01 | Resumen administrativo |

La semilla (`assets/seed.json`) incluye 4 faenas, 11 frentes, 11 beacons, 27 usuarios (`OP-0001` a `OP-0024`, `CAP-0001`, `CAP-0002`, `ADM-0001`) y 122 marcas. Se carga la primera vez que se abre la pantalla de login. Al cargarla, las fechas se desplazan para que la última marca quede en la fecha actual y el calendario muestre actividad reciente.

### Flujo recomendado para probar los tres roles

1. **Operario `OP-0001`**
   - Marcar `IN` y `OUT`.
   - Volver a marcar `IN` para ver el aviso flotante de entrada duplicada.
   - Crear una solicitud en **Mis horas extra**.
   - Revisar **Mi historial** (calendario) y **Ver mapa** (Mapbox).
   - Probar el botón atrás del sistema: debe volver al login, no cerrar la app.
2. **Capataz `CAP-0001`**
   - Ver la marca del operario en el panel de F01.
   - Abrir **Ver detalle** y visar (aprobar/rechazar).
   - Entrar a **Aprobar horas extra** y resolver la solicitud creada por `OP-0001`.
   - Revisar calendario y mapa.
3. **Administración `ADM-0001`**
   - Ver métricas de marcas y horas extra.
   - Exportar el CSV y el PDF, y compartirlos.
   - Revisar calendario general y alertas.

## Beacons con iOS (Locate Beacon)

La app espera un **iBeacon** (fabricante Apple `0x004C`, prefijo `02 15`) y compara **solo el UUID**. El major/minor no se validan; el RSSI promedio debe ser **≥ -80 dBm** durante el escaneo de 6 s.

> Usa la variante **campo** (`app-campo-debug.apk`). La variante `demo` usa sensores simulados y no escanea Bluetooth real.

Para `OP-0001` (faena F01) configura el beacon con uno de estos UUID:

| UUID | Major | Minor |
| --- | --- | --- |
| `20FDCED4-000F-5342-B70D-D3D16FD767FE` | 1 | 1 |
| `180FBAE4-8D2C-5BFE-A785-8986DB0F0AAF` | 1 | 2 |
| `E73E4C2C-1677-5FE4-92AE-3D64E68DD0D5` | 1 | 3 |

Pasos con **Locate Beacon** (iOS):

1. Abre la app y elige **Transmit** / **Broadcast**.
2. Selecciona el formato **iBeacon**.
3. Pega uno de los UUID de la tabla.
4. Configura Major `1` y Minor `1` (o el que corresponda).
5. Inicia la transmisión y deja el iPhone a 1–2 m del Android.
6. Verifica con **nRF Connect** o **Beacon Scanner** que el UUID se anuncie.
7. En SmartVolt abre **Probar beacon (8 s)** en la tarjeta *Diagnóstico del beacon*. Debe listar el UUID y su RSSI.
8. Marca asistencia: debe aparecer `✔ Beacon autorizado detectado`.

Si usas otro operario, revisa su `faenaId` en `assets/seed.json` y usa los UUID de esa faena. Si tu beacon no permite cambiar el UUID, actualiza `seed.json` con el UUID real y vuelve a compilar.

En la variante `campo`, la pantalla de marcar incluye el botón **Beacon de prueba (Locate Beacon)**, que muestra los UUID autorizados de la faena del usuario para copiarlos directamente en la app iOS, y una tarjeta de **Diagnóstico del beacon** con una prueba real de escaneo.

### Si el beacon no aparece

1. Confirma que instalaste `app-campo-debug.apk`, no la variante `demo`.
2. Desinstala la versión anterior y vuelve a instalar: el permiso BLE cambió y Android puede conservar la configuración antigua.
3. Acepta los permisos de **ubicación precisa** y **dispositivos cercanos** (Android 12+).
4. Activa **Bluetooth** y **Ubicación**.
5. Deja **Locate Beacon abierto en primer plano**; iOS deja de transmitir si la app se suspende.
6. Usa formato **iBeacon**, no Eddystone ni AltBeacon.
7. Pulsa **Probar beacon (8 s)**. Si no aparece ningún UUID, revisa `Logcat` con el filtro `SmartVoltBLE`; allí se registra si falta un permiso, si Bluetooth está apagado o si el escaneo no devolvió resultados.
8. Si aparece el UUID pero el RSSI es menor a `-80 dBm`, acerca el iPhone o sube la potencia de transmisión.

## Mapbox

El mapa de la faena usa **Mapbox GL JS** dentro de un `WebView` (`assets/map.html`). Se utiliza el token **público** `pk.` del archivo de tokens entregado; el SDK web no requiere el token secreto `sk.` que sí exige el repositorio Maven del SDK nativo.

- El HTML se sirve con `WebViewAssetLoader` desde `https://appassets.androidplatform.net/assets/map.html`, no desde `file://`. Esto evita el bloqueo de peticiones cross-origin que dejaba el mapa en blanco.
- Mapbox GL JS y Leaflet están **empaquetados en `assets/`**, así que no dependen de un CDN externo.
- Centro y radio se leen desde el perímetro de la faena en Room.
- Se dibuja un marcador y un polígono aproximado del radio de cobertura.
- Si Mapbox falla (token restringido, WebGL no disponible o timeout), el mapa cae automáticamente a **Leaflet + OpenStreetMap**, que no requiere token.
- El botón de recarga de la barra superior vuelve a crear el `WebView`.
- Si no hay conexión, se muestra un aviso sobre el mapa.
- Para cambiar de cuenta, reemplaza `mapboxgl.accessToken` en `assets/map.html` por otro token `pk.` válido.

## Cumplimiento de la rúbrica

| Indicador | Evidencia en el proyecto |
| --- | --- |
| **IE 2.1.1** Interfaz coherente, jerárquica y navegación funcional | Material 3, tema claro/oscuro, `TopAppBar`, tarjetas, botones grandes para terreno, `NavHost` con transiciones y botón atrás que vuelve a la pantalla anterior. |
| **IE 2.1.2** Formularios completos, validaciones visuales por campo e íconos | Login (`Person`, `Lock`, `Error`) y solicitud de horas extra (`Timer`, `Notes`, `Error`) con `isError`, `supportingText`, íconos y mensajes por campo. |
| **IE 2.2.1** Validación centralizada y desacoplada de la UI | `domain/validation/ValidacionFormularios.kt` concentra las reglas de login y horas extra; las pantallas solo muestran el resultado. Pruebas unitarias en `ValidacionFormulariosTest`. |
| **IE 2.2.2** Animaciones funcionales | Transiciones `slide + fade` entre pantallas, `AnimatedContent` del veredicto, `AnimatedVisibility` de errores/progreso, contadores animados en administración y capataz, `animateContentSize` en tarjetas. |
| **IE 2.3.1** Modularidad, MVVM y persistencia local | Capas `data` / `domain` / `ui`, ViewModels con `StateFlow`, repositorio con interfaz + implementación Room, Hilt, Room v3 con migraciones y WorkManager. |
| **IE 2.3.2** GitHub y Trello | Repositorio `https://github.com/Camila-duoc/smartvolt.git` con commits técnicos (verificar que esté público antes de la entrega). Planificación respaldada en `docs/PLANIFICACION.md`; el tablero de Trello del equipo debe enlazarse en la sección de planificación. |
| **IE 2.4.1** Al menos dos recursos nativos integrados | GPS (`FusedLocationProvider` + respaldo `LocationManager`), Bluetooth LE (escaneo de iBeacons), notificaciones locales, `FileProvider` para compartir CSV/PDF y `WebView` para el mapa. |

## Funcionalidades

### Operario
- **Marcar entrada y salida** con validación automática: cada marca pasa por el motor de reglas antifraude (`RuleEngine`) y muestra el veredicto (VÁLIDA, SOSPECHOSA o RECHAZADA) con el detalle de cada regla.
- **Aviso flotante** cuando intenta registrar una entrada duplicada o una salida sin entrada previa.
- **Reglas**: ubicación simulada (mock location), secuencia entrada/salida, geocerca, beacon autorizado, presencia física y reloj. El modo desarrollador ya no penaliza la marca.
- **Cadena de hash** por marca (`HashMarca`) para detectar alteraciones, con verificación de integridad y encadenamiento.
- **Mi historial** (calendario) y **mapa** de la faena con Mapbox.
- **Horas extra**: solicitar y consultar el estado de las propias solicitudes, con validación por campo, tope de 12 h y motivo mínimo de 10 caracteres.
- Contador de marcas pendientes de envío.
- **Notificación local** cuando una marca queda sospechosa y requiere visación.

### Capataz
- Panel con la última marca de cada operario de su faena.
- **Visación** de marcas sospechosas (aprobar o rechazar), con el detalle de la evidencia.
- **Aprobación de horas extra**, calendario y mapa.
- Contadores animados de operarios y marcas sospechosas por visar.

### Administración
- Resumen de marcas (total, válidas, sospechosas, rechazadas, pendientes de sincronizar) y por faena, usuario y estado.
- **Marcas no válidas por faena** (porcentaje de sospechosas y rechazadas sobre el total de cada faena).
- **Alertas recientes** en solo lectura (últimas marcas sospechosas o rechazadas).
- Resumen de horas extra (solicitudes, pendientes, aprobadas y horas aprobadas, rechazadas).
- **Exportación** a CSV (todas las marcas) y a PDF (resumen), compartidos con el selector de Android mediante `FileProvider`.
- **Contadores animados** y aparición animada de las alertas.
- **Verificación de la cadena de hashes**: indica si el registro es íntegro y cuántas marcas de la semilla no son verificables.

### Interfaz y experiencia
- Tema **Material 3** con tipografía, formas y **modo oscuro** (sigue el ajuste del sistema).
- **Transiciones animadas** entre pantallas y aparición animada del resultado de la marca.
- **Botón atrás coherente**: en pantallas raíz (operario, capataz, admin) vuelve al login; en pantallas secundarias vuelve a la pantalla anterior.
- **Notificaciones locales**: marca sospechosa pendiente de visación y sincronización completada (canales `smartvolt_alertas` y `smartvolt_sync`).
- **Formularios validados** en login y horas extra, con mensajes de error junto a cada campo mientras se escribe.

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
│   ├── usecase/      # RegistrarMarca, HashMarca, SolicitarHoraExtra, ExportarMarcasCsv, ExportarReportePdf
│   └── validation/   # ValidacionFormularios.kt (reglas de login y horas extra)
├── sensors/          # GPS/BLE reales y simulados
├── sync/             # SyncWorker y SyncScheduler
└── ui/               # login, checkin, foreman, visacion, admin, horasextra, calendario, mapa, navigation, theme
```

## Pruebas

Las pruebas unitarias están en `app/src/test`. Desde Android Studio, ejecuta la carpeta `test` o la tarea Gradle `testDemoDebugUnitTest` / `testCampoDebugUnitTest`. Actualmente hay **77 pruebas** que cubren:

- Geometría y perímetros (`GeoUtils`).
- Reglas antifraude (`RuleEngine`).
- Integridad del hash de marcas (`HashMarca`): hash correcto, detección de alteraciones, enlaces rotos y marcas sin hash.
- Horas extra (cálculo, validaciones y ViewModels).
- Calendario, visación y exportación CSV.
- Métricas de administración, alertas y resumen para PDF.
- Validaciones centralizadas de login y horas extra.
- API simulada de demo (`FakeApiService`).

## Planificación y control de versiones

- **GitHub**: `https://github.com/Camila-duoc/smartvolt.git` (debe ser público o tener al docente como colaborador).
- **Trello**: el equipo debe pegar aquí el enlace del tablero antes de la entrega.
- El detalle de tareas, columnas y commits asociados está en `docs/PLANIFICACION.md`.

> La rúbrica exige que el tablero de Trello esté disponible y muestre la distribución de tareas. Mantén el enlace actualizado, asigna las tarjetas a cada integrante y agrega al docente como miembro del tablero.

## Limitaciones conocidas

- No hay backend real: en `demo` la API es simulada y en `campo` se espera un servidor en `API_BASE_URL`.
- La autenticación es de demostración (PIN fijo) y la sesión vive en memoria.
- La base de datos no está cifrada todavía (SQLCipher pendiente, RNF-08).
- El reloj de referencia no se ancla a la hora del servidor (pendiente).
- El PDF es un resumen de texto simple; el CSV incluye todas las marcas.
- El mapa requiere conexión a internet para cargar las teselas de Mapbox o, en el respaldo, de OpenStreetMap.
- Los datos son sintéticos y no representan personas reales.

## Reiniciar la demo

Para volver a cargar la semilla, borra los datos de la app desde Ajustes de Android (**Almacenamiento → Borrar datos**) o desinstala y vuelve a instalar.
