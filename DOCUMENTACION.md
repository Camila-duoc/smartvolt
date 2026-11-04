# Smart-Volt Asistencia — Guía del proyecto y de archivos

> Documento de apoyo para entender, ejecutar y defender el proyecto.
> Aplicación Android académica de control de asistencia en faena.

---

## 1. Qué es y qué problema resuelve

**Smart-Volt Asistencia** es una app Android que registra la entrada y salida del personal en una faena minera/industrial. El problema que ataca es la **suplantación y el fraude en el registro de asistencia**: las apps tradicionales confían ciegamente en el GPS, y eso se vulnera con aplicaciones de *Fake GPS*, relojes manipulados o marcas hechas desde casa.

La solución combina varias señales para confirmar que la persona realmente está en el lugar:

- **GPS** con geocerca (círculo o polígono).
- **Beacons BLE** autorizados (presencia física near-field).
- **Estado del teléfono**: modo desarrollador y hora automática.
- **Secuencia** entrada/salida.
- **Cadena de hashes SHA-256** para que una marca guardada no pueda alterarse sin dejar rastro.

Todo funciona **offline-first**: las marcas se validan y guardan en el teléfono (Room) y después se sincronizan con un backend mediante WorkManager. En la variante `demo` no se necesita hardware ni servidor.

---

## 2. Flujo funcional por rol

### Operario (`OP-0001`, PIN `1234`)

1. Inicia sesión.
2. Presiona **MARCAR ENTRADA** o **MARCAR SALIDA**.
3. La app captura GPS + BLE + estado del sistema, evalúa las reglas y muestra el veredicto:
   - **VÁLIDA**: todo correcto.
   - **SOSPECHOSA**: requiere visación del capataz.
   - **RECHAZADA**: falló una regla bloqueante (por ejemplo, Fake GPS).
4. Puede ver su historial, el mapa de la faena y solicitar horas extra.

### Capataz (`CAP-0001`, `CAP-0002`, PIN `1234`)

1. Ve el **panel de presencia** con la última marca de cada operario de su faena.
2. Puede **aprobar o rechazar** marcas sospechosas (visación).
3. Puede revisar y resolver solicitudes de horas extra.
4. Accede al calendario y al mapa de la faena.

### Administración (`ADM-0001`, PIN `1234`)

1. Ve métricas globales: marcas por estado, por faena, por usuario, horas extra y porcentaje de marcas no válidas.
2. Ve **alertas recientes** (sospechosas/rechazadas) y puede abrir su detalle en solo lectura.
3. Ve el estado de la **cadena de hashes** (“Registro inalterable”).
4. Exporta un **CSV** de marcas y un **PDF** de resumen usando el selector de compartir de Android.

---

## 3. Cómo funciona por dentro

### 3.1 Arquitectura en capas

```
UI (Compose + ViewModels)
        │  StateFlow / collectAsState
        ▼
Dominio (casos de uso + reglas antifraude + modelos)
        │
        ▼
Datos (Room + Retrofit + repositorio)
        │
        ▼
Sensores (GPS/BLE reales o simulados)
```

- **UI**: pantallas Compose y ViewModels con Hilt. La UI no contiene reglas de negocio; solo muestra estado y dispara acciones.
- **Dominio**: reglas puras y testeables (`RuleEngine`, `GeoUtils`, `HashMarca`, casos de uso). No depende de Compose.
- **Datos**: `AttendanceRepository` como contrato; `RoomAttendanceRepository` es la implementación real con Room + Retrofit.
- **Sensores**: interfaces `LocationProvider`, `BleScanner` y `SystemStateProvider`; se inyectan versiones simuladas o reales según el *flavor*.
- **Inyección de dependencias**: Hilt (`di/AppModule.kt`).

### 3.2 Flujo de una marca

1. `CheckInScreen` llama a `CheckInViewModel.marcar(tipo)`.
2. `RegistrarMarcaUseCase` obtiene perímetro y beacons desde Room.
3. Captura ubicación (`LocationProvider`), lecturas BLE (`BleScanner`) y estado del sistema.
4. Construye una `Evidencia` y la pasa al `RuleEngine`.
5. El motor devuelve un `Veredicto` (VÁLIDA / SOSPECHOSA / RECHAZADA) con el detalle de cada regla.
6. Calcula el hash SHA-256 encadenado con el hash de la marca anterior.
7. Guarda en Room, en una sola transacción: marca + lecturas BLE + eventos antifraude.
8. La UI muestra el resultado con animación; si es sospechosa, lanza una notificación local.
9. `SyncScheduler` encola `SyncWorker` para enviar las marcas pendientes cuando haya red.

### 3.3 Reglas antifraude

| Regla | Qué controla | Severidad |
|---|---|---|
| `MockLocationRule` | Ubicación simulada (Fake GPS) | Bloqueante |
| `SequenceRule` | No hay salida sin entrada ni entrada duplicada | Bloqueante |
| `GeofenceRule` | Posición dentro del perímetro, con margen según precisión | Advertencia |
| `BeaconRule` | Beacon autorizado con RSSI suficiente | Advertencia |
| `PresenciaFisicaRule` | Al menos GPS o BLE confirman presencia | Bloqueante |
| `DeveloperModeRule` | Modo desarrollador activo | Advertencia |
| `ClockRule` | Hora automática desactivada | Advertencia |

- Si falla una **bloqueante** → `RECHAZADA`.
- Si solo fallan **advertencias** → `SOSPECHOSA` (requiere visación).
- Si todas pasan → `VÁLIDA`.

### 3.4 Cadena de hashes (registro inalterable)

Cada marca guarda:

- `hashPrevio`: hash de la marca anterior del mismo usuario (`GENESIS` si es la primera).
- `hash`: SHA-256 de `hashPrevio|id|usuarioId|tipo|lat|lon|timestamp|estado`.

`HashMarca.verificarCadena(...)` recorre las marcas por usuario y comprueba:

1. Que el hash guardado coincida con el contenido actual.
2. Que `hashPrevio` apunte al hash de la marca anterior.

Las marcas de la semilla no traen hash: se informan como **no verificables** para no reportar falsos positivos.

### 3.5 Persistencia y offline-first

- **Room** es la fuente de verdad local. Base `smartvolt.db`, versión 3, con migraciones 1→2 y 2→3.
- 9 entidades: usuarios, faenas, frentes, beacons, marcas, lecturas BLE, eventos antifraude, visaciones y horas extra.
- La primera vez se carga `assets/seed.json` (4 faenas, 11 frentes, 11 beacons, 27 usuarios y 122 marcas).
- Las marcas nuevas quedan `PENDIENTE`; `SyncWorker` las envía y las marca `ENVIADA` o `ERROR`.
- En `demo`, `FakeApiService` acepta todo; en `campo`, Retrofit apunta a `API_BASE_URL`.

### 3.6 Notificaciones locales

- Canales creados al iniciar la app (`SmartVoltApp`).
- Aviso de **marca sospechosa** que requiere visación.
- Aviso de **sincronización completada** con la cantidad de marcas enviadas.
- En Android 13+ se pide `POST_NOTIFICATIONS` desde `CheckInScreen`.

### 3.7 Validación de formularios

Hay dos formularios:

1. **Login** (`LoginScreen` + `LoginViewModel` + `ValidarLoginUseCase`).
2. **Solicitud de horas extra** (`HoraExtraScreen` + `SolicitarHoraExtraUseCase`).

Las reglas viven en el dominio:

- `ValidarLoginUseCase`: formato del código (`OP-0001`, `CAP-0001`, `ADM-0001`) y PIN de 4 dígitos.
- `SolicitarHoraExtraUseCase`: horas > 0, máximo 12 h, motivo mínimo 10 caracteres.

La UI solo decide **cuándo mostrar** el error y lo pinta por campo con:

- `isError` (borde/etiqueta roja).
- `supportingText` (mensaje bajo el campo).
- `leadingIcon` (ícono del campo).
- `trailingIcon` (`ErrorOutline` cuando hay error).
- Íconos de éxito/error en los mensajes generales.

### 3.8 Animaciones

- Transiciones de pantalla en `AppNav` (slide + fade).
- Aparición animada del resultado de la marca (`AnimatedContent`).
- Carga/error/éxito con `AnimatedVisibility`.
- Contadores animados en el panel de administración (`animateIntAsState`).
- Alertas animadas en administración.

---

## 4. Cómo ejecutar el proyecto

### Requisitos

- Android Studio reciente.
- JDK 17.
- Android SDK 35.
- Emulador o teléfono con Android 8.0 (API 26) o superior.
- `local.properties` con la ruta del SDK (Android Studio lo genera).

### Variantes

| Variante | Sensores | API | Cuándo usarla |
|---|---|---|---|
| `demo` | GPS y BLE simulados | `FakeApiService` (acepta todo) | Presentación y pruebas sin hardware |
| `campo` | GPS y BLE reales | Retrofit contra `API_BASE_URL` | Teléfono físico con backend |

En Android Studio: **Build Variants → demoDebug → Run**.

### Credenciales de la semilla

| Rol | Código | PIN |
|---|---|---|
| Operario | `OP-0001` | `1234` |
| Capataz | `CAP-0001` / `CAP-0002` | `1234` |
| Administración | `ADM-0001` | `1234` |

### Reiniciar la demo

Ajustes de Android → Aplicaciones → Smart-Volt → Almacenamiento → **Borrar datos**.
Al volver a abrir, se recarga `seed.json`.

### Compilar y probar por consola

Si el clon incluye los scripts del wrapper:

```bash
./gradlew :app:assembleDemoDebug
./gradlew :app:testDemoDebugUnitTest
```

> En esta copia del repositorio no están los scripts `gradlew`/`gradlew.bat`; por eso la vía recomendada es Android Studio. Si se usa un Gradle instalado, los comandos son los mismos.

---

## 5. Estructura de carpetas

```
smartvolt/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/            # seed.json y map.html
│       │   ├── res/               # temas, strings, íconos, file_paths
│       │   └── java/cl/smartvolt/asistencia/
│       │       ├── MainActivity.kt
│       │       ├── SmartVoltApp.kt
│       │       ├── data/          # Room, Retrofit, repositorio
│       │       ├── di/            # módulo Hilt
│       │       ├── domain/        # modelos, reglas, casos de uso
│       │       ├── notifications/ # notificaciones locales
│       │       ├── sensors/       # GPS/BLE reales y simulados
│       │       ├── sync/          # WorkManager
│       │       └── ui/            # pantallas, ViewModels, tema, navegación
│       ├── debug/AndroidManifest.xml
│       └── test/                  # pruebas unitarias JUnit
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── README.md
└── DOCUMENTACION.md
```

---

## 6. Función de cada archivo

### 6.1 Raíz y configuración

| Archivo | Función |
|---|---|
| `build.gradle.kts` | Declara los plugins de Android, Kotlin, Compose, serialización, KSP y Hilt para todo el proyecto (sin aplicarlos aquí). |
| `settings.gradle.kts` | Nombre del proyecto, inclusión del módulo `:app` y repositorios de dependencias. |
| `gradle.properties` | Propiedades de Gradle: flags de AndroidX, memoria de la JVM, etc. |
| `gradle/libs.versions.toml` | Catálogo central de versiones y dependencias (Compose, Room, Hilt, Retrofit, etc.). |
| `gradle/wrapper/gradle-wrapper.properties` | Fija la versión de Gradle usada por el proyecto (8.9). |
| `local.properties` | Ruta local del SDK de Android. No se versiona. |
| `README.md` | Resumen rápido: stack, variantes, credenciales y funcionalidades. |
| `DOCUMENTACION.md` | Este documento: explicación del proyecto, arquitectura y función de cada archivo. |
| `app/build.gradle.kts` | Configuración del módulo: `compileSdk`/`minSdk`, flavors `demo`/`campo`, `BuildConfig` y dependencias. |

### 6.2 Recursos, assets y manifests

| Archivo | Función |
|---|---|
| `app/src/main/AndroidManifest.xml` | Permisos (ubicación, Bluetooth, red, notificaciones), `Application`, `MainActivity`, inicialización de WorkManager y `FileProvider` para compartir CSV/PDF. |
| `app/src/debug/AndroidManifest.xml` | Solo debug: permite tráfico HTTP claro para el backend local (`10.0.2.2:8080`). |
| `app/src/main/assets/seed.json` | Datos sintéticos iniciales: faenas, frentes, beacons, usuarios y marcas. |
| `app/src/main/assets/map.html` | Mapa Leaflet + OpenStreetMap del diseño anterior. Ya **no se usa**: la pantalla de mapa ahora es Mapbox (se conserva solo como referencia). |
| `app/src/main/res/values/strings.xml` | Nombre visible de la app. |
| `app/src/main/res/values/themes.xml` | Tema XML base claro (fondo y barra de estado) antes de que Compose tome el control. |
| `app/src/main/res/values-night/themes.xml` | Variante oscura del tema XML para evitar el destello blanco de noche. |
| `app/src/main/res/drawable/ic_notificacion.xml` | Ícono vectorial usado por las notificaciones locales. |
| `app/src/main/res/xml/file_paths.xml` | Define `cache/exports` como ruta compartible por `FileProvider`. |

### 6.3 Punto de entrada

| Archivo | Función |
|---|---|
| `MainActivity.kt` | Única Activity. Aplica `SmartVoltTheme` y monta `AppNav` con Compose. |
| `SmartVoltApp.kt` | Clase `Application` con Hilt. Crea los canales de notificación y configura WorkManager con `HiltWorkerFactory`. |

### 6.4 Capa de datos

| Archivo | Función |
|---|---|
| `data/local/Entities.kt` | Las 9 entidades Room: usuarios, faenas, frentes, beacons, marcas, lecturas BLE, eventos antifraude, visaciones y horas extra. Define claves, índices y claves foráneas. |
| `data/local/Daos.kt` | DAOs de configuración, marcas y horas extra. Incluye las consultas, las migraciones 1→2 y 2→3, y el singleton `AppDatabase`. |
| `data/remote/Api.kt` | DTOs `@Serializable` y la interfaz Retrofit `ApiService` (envío de lotes y configuración). También define el formato de `seed.json`. |
| `data/remote/FakeApi.kt` | Implementación simulada de `ApiService` para `demo`: acepta todas las marcas y devuelve configuración vacía. |
| `data/repository/AttendanceRepository.kt` | Contrato de acceso a datos + `RoomAttendanceRepository`. Implementa lectura local, guardado transaccional, visación, sincronización, carga de la semilla y horas extra. |

### 6.5 Dominio

| Archivo | Función |
|---|---|
| `domain/model/Models.kt` | Enums (`Rol`, `TipoMarca`, `EstadoMarca`, `SyncEstado`, `Severidad`, `TipoPerimetro`, `EstadoHoraExtra`) y data classes del dominio (`LatLon`, `Perimetro`, `Evidencia`, `Veredicto`, etc.). |
| `domain/antifraude/RuleEngine.kt` | Ejecuta todas las reglas y decide el estado final: RECHAZADA si falla una bloqueante, SOSPECHOSA si falla una advertencia, VÁLIDA si todo pasa. |
| `domain/antifraude/Rules.kt` | Implementa las 7 reglas antifraude (`MockLocationRule`, `SequenceRule`, `GeofenceRule`, `BeaconRule`, `PresenciaFisicaRule`, `DeveloperModeRule`, `ClockRule`). |
| `domain/geo/GeoUtils.kt` | Geofencing en Kotlin puro: distancia Haversine, punto dentro de círculo/polígono y distancia al borde con margen. |
| `domain/usecase/RegistrarMarcaUseCase.kt` | Orquesta la marca: captura sensores, evalúa reglas, calcula el hash encadenado y guarda todo. Incluye `TrustedClock` (reloj del dispositivo). |
| `domain/usecase/HashMarca.kt` | Cálculo SHA-256 encadenado y verificación de integridad de toda la cadena (`verificarCadena`). |
| `domain/usecase/SolicitarHoraExtraUseCase.kt` | Valida horas (positivas, máximo 12) y motivo (mínimo 10 caracteres), y persiste la solicitud. Incluye validadores puros reutilizados por la UI y `ResolverHoraExtraUseCase`. |
| `domain/usecase/ValidarLoginUseCase.kt` | Reglas puras del login: formato del código y PIN de 4 dígitos. Devuelve errores por campo (`ErroresLogin`). |
| `domain/usecase/ExportarMarcasCsv.kt` | Genera el CSV de marcas con encabezado y escape correcto de comas, comillas y saltos de línea. |
| `domain/usecase/ExportarReportePdf.kt` | Genera un PDF A4 de varias páginas con `PdfDocument` a partir de líneas de texto. |

### 6.6 Sensores, sincronización y notificaciones

| Archivo | Función |
|---|---|
| `sensors/Sensors.kt` | Interfaces `LocationProvider`, `BleScanner` y `SystemStateProvider`; enum de escenarios y `DemoState` para el flavor demo. |
| `sensors/FakeSensors.kt` | Sensores simulados: según el escenario devuelven ubicación válida, Fake GPS, fuera de perímetro o sin beacon. |
| `sensors/RealSensors.kt` | Sensores reales: `FusedLocationProviderImpl` (GPS), `BleScannerImpl` (iBeacon por fabricante Apple) y `SystemStateProviderImpl` (modo desarrollador y hora automática). |
| `sync/SyncWorker.kt` | `SyncWorker` envía marcas pendientes y configuración, notifica si envió algo y reintenta con backoff. `SyncScheduler` encola el trabajo con restricción de red. |
| `notifications/Notificaciones.kt` | Crea canales y construye las notificaciones de marca sospechosa y sincronización completada. |

### 6.7 Interfaz de usuario (Compose)

| Archivo | Función |
|---|---|
| `ui/navigation/AppNav.kt` | `NavHost` con todas las rutas, transiciones animadas, `popUpTo` al cerrar sesión y `LocalNavController` para navegar desde cualquier pantalla. |
| `ui/theme/Theme.kt` | Tema Material 3: esquemas claro/oscuro, tipografía, formas y colores de marca (`Ok`, `Warn`, `Bad`). |
| `ui/login/LoginScreen.kt` | Pantalla de login. Contiene `SessionState` (sesión compartida), `LoginViewModel` y el formulario con validación por campo e íconos. |
| `ui/checkin/CheckInScreen.kt` | Pantalla del operario: permisos, botones grandes de entrada/salida, resultado animado, notificación de marca sospechosa y accesos a historial/mapa/horas extra. Incluye `CheckInViewModel`. |
| `ui/foreman/ForemanScreen.kt` | Panel del capataz: última marca por operario, visación de sospechosas y accesos a horas extra, calendario y mapa. Incluye `ForemanViewModel`. |
| `ui/horasextra/HoraExtraScreen.kt` | Formulario de solicitud de horas extra con validación por campo, íconos, tope de horas y motivo mínimo. |
| `ui/horasextra/HoraExtraViewModel.kt` | Estado de las solicitudes y acciones de solicitar/aprobar/rechazar según el rol. |
| `ui/horasextra/AprobarHoraExtraScreen.kt` | Pantalla del capataz para revisar solicitudes pendientes y ver el historial de resoluciones. |
| `ui/horasextra/MisHorasExtraScreen.kt` | Pantalla del operario con sus solicitudes y botón flotante para crear una nueva. |
| `ui/horasextra/MisHorasExtraViewModel.kt` | Carga las solicitudes del usuario activo desde Room. |
| `ui/horasextra/ResumenHorasExtraScreen.kt` | Contenido reutilizable de resumen: tarjetas por estado y lista de solicitudes. |
| `ui/calendario/CalendarioScreen.kt` | Historial agrupado por fecha, con filtros de 7 días, 30 días o todo. |
| `ui/calendario/CalendarioViewModel.kt` | Carga y agrupa las marcas por día según el rol (operario, capataz o admin). |
| `ui/mapa/MapaScreen.kt` | Muestra el mapa de la faena con **Mapbox Maps SDK + maps-compose** (centro, marcador y tarjeta con el radio del perímetro). |
| `ui/mapa/MapaViewModel.kt` | Obtiene de Room el centro, radio y nombre de la faena del usuario. |
| `ui/visacion/DetalleVisacionScreen.kt` | Detalle de solo lectura de una marca y su visación. |
| `ui/visacion/DetalleVisacionViewModel.kt` | Carga la marca y la visación asociada desde el repositorio. |
| `ui/admin/AdminScreen.kt` | Panel de administración: métricas animadas, exportación CSV/PDF, porcentaje de marcas no válidas, alertas con navegación a detalle y estado de la cadena de hashes. |
| `ui/admin/AdminViewModel.kt` | Calcula métricas, porcentajes, alertas, verificación de la cadena y líneas del reporte PDF/CSV. |

### 6.8 Pruebas unitarias

| Archivo | Función |
|---|---|
| `ValidarLoginUseCaseTest.kt` | Verifica formato de códigos, PIN de 4 dígitos y errores por campo del login. |
| `SolicitarHoraExtraUseCaseTest.kt` | Verifica horas inválidas/fuera de tope, motivo vacío/corto, recorte del motivo y paridad entre UI y caso de uso. |
| `HashMarcaTest.kt` | Verifica determinismo del hash, detección de alteraciones, enlaces rotos y marcas de semilla no verificables. |
| `RuleEngineTest.kt` | Prueba las reglas: marca válida, Fake GPS, sin beacon, fuera de perímetro, etc. |
| `GeoUtilsTest.kt` | Prueba Haversine, círculo, polígono y margen de precisión. |
| `ExportarMarcasCsvTest.kt` | Prueba encabezado, una línea por marca y escape de campos. |
| `FakeApiServiceTest.kt` | Prueba que la API demo acepta el lote y conserva el estado enviado. |
| `HoraExtraTest.kt` | Prueba el flujo de solicitar, aprobar y rechazar horas extra con un repositorio falso. |
| `HoraExtraViewModelTest.kt` | Prueba el estado del ViewModel de horas extra por rol y sus acciones. |
| `AdminViewModelTest.kt` | Prueba las métricas del panel de administración con datos falsos. |
| `AdminReporteTest.kt` | Prueba las líneas del reporte: porcentajes, alertas e integridad. |
| `CalendarioTest.kt` | Prueba el filtrado y la agrupación por fecha según el rol. |
| `DetalleVisacionViewModelTest.kt` | Prueba la carga del detalle de una marca y su visación. |
| `MainDispatcherRule.kt` | Regla de JUnit que reemplaza `Dispatchers.Main` por un dispatcher de prueba para los ViewModels. |

---

## 7. Mapa de la pauta al código

| Ítem de la pauta | Dónde se demuestra |
|---|---|
| Diseño Material 3 / navegación clara | `ui/theme/Theme.kt`, `res/values/themes.xml`, `res/values-night/themes.xml`, `ui/navigation/AppNav.kt` y todas las pantallas con `Scaffold`, `TopAppBar`, `Card`, `FilterChip`. |
| Formularios validados con íconos y mensajes | `ui/login/LoginScreen.kt`, `ui/horasextra/HoraExtraScreen.kt`, `domain/usecase/ValidarLoginUseCase.kt`, `domain/usecase/SolicitarHoraExtraUseCase.kt`. |
| Validaciones desde lógica (sin acoplar a la UI) | `ValidarLoginUseCase`, `SolicitarHoraExtraUseCase`, `RuleEngine`, `GeoUtils`, `HashMarca`. |
| Navegación funcional / estructura modular | `ui/navigation/AppNav.kt`, paquetes `data/domain/ui/di/sensors/sync/notifications`, MVVM con Hilt. |
| Gestión de estado | `StateFlow` + `collectAsState` en todos los ViewModels; `SessionState` compartido. |
| Almacenamiento local operativo | `data/local/Entities.kt`, `Daos.kt`, `AttendanceRepository.kt`, `assets/seed.json`. |
| Recursos nativos (≥ 2) | GPS y BLE reales en `sensors/RealSensors.kt`; permisos en `CheckInScreen`; `FileProvider` en `AdminScreen`; SDK de mapas Mapbox en `MapaScreen`; notificaciones en `Notificaciones.kt`. |
| Animaciones | `AppNav.kt`, `CheckInScreen.kt`, `AdminScreen.kt`, `HoraExtraScreen.kt`, `LoginScreen.kt`. |

---

## 8. Pruebas y verificación

Comandos usados para validar el proyecto:

```bash
:app:compileDemoDebugKotlin
:app:compileCampoDebugKotlin
:app:testDemoDebugUnitTest
```

Resultado esperado: compilación sin errores y todas las pruebas unitarias en verde.

---

## 9. Limitaciones y siguientes pasos (Fase 3)

Estas piezas están fuera del alcance del MVP actual y quedan documentadas como mejoras:

- **SQLCipher** para cifrar la base local.
- **Reloj anclado al servidor** (hoy se usa el reloj del dispositivo).
- **Autenticación real** por usuario o biometría (hoy el PIN es fijo `1234` para la demo).
- **Ausentismo real** (hoy se miden marcas no válidas, no ausencias).
- **Backend Spring Boot** real (hoy la variante demo usa `FakeApiService`).
- **Registro de usuarios** desde administración (opcional; requeriría una migración 3→4).
- **Recordar el formulario al rotar** ya está resuelto con `rememberSaveable` en login y horas extra.

---

## 10. Resumen para la defensa

Si tuvieras que explicarlo en 30 segundos:

> Smart-Volt registra asistencia en faena sin confiar solo en el GPS. Cada marca se valida con reglas antifraude (Fake GPS, geocerca, beacons BLE, secuencia, modo desarrollador y reloj), se encadena con SHA-256 para que no se pueda alterar y se guarda en Room. La app funciona offline y sincroniza con WorkManager. La UI está en Compose con Material 3, MVVM, Hilt y validación por campo con íconos. Los sensores reales se usan en el flavor `campo` y se simulan en `demo` para poder presentar sin hardware.
