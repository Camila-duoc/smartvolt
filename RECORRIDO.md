# Recorrido guiado del código — Smart-Volt Asistencia

> Para estudiar el proyecto sin perderse: primero el modelo mental, después una ruta de lectura archivo por archivo, y al final qué archivos tocaría un profesor si pide cambios.

La ruta base de todos los archivos Kotlin es:

```text
app/src/main/java/cl/smartvolt/asistencia/
```

Los tests están en:

```text
app/src/test/java/cl/smartvolt/asistencia/
```

---

## 1. Modelo mental en 30 segundos

La app tiene tres capas y una regla de oro:

```text
Pantalla (Compose)
   │  muestra estado y dispara acciones
   ▼
ViewModel (Hilt + StateFlow)
   │  orquesta y expone UiState
   ▼
Caso de uso / Reglas (dominio)
   │  decide qué es válido
   ▼
Repositorio
   │
   ├── Room (verdad local)
   ├── Retrofit / FakeApi (red)
   └── Sensores (GPS/BLE reales o simulados)
```

**Regla de oro:** la pantalla no decide reglas de negocio. Solo pinta lo que el ViewModel le entrega y llama funciones del ViewModel. Las reglas viven en `domain/`.

Otra idea clave: hay dos variantes.

- `demo`: sensores simulados (`FakeSensors`) y API simulada (`FakeApiService`). No necesita hardware ni backend.
- `campo`: sensores reales (`RealSensors`) y Retrofit contra `API_BASE_URL`.

`di/AppModule.kt` decide cuál se inyecta usando `BuildConfig.USE_FAKE_SENSORS` y `BuildConfig.USE_FAKE_API`, definidos en `app/build.gradle.kts`.

---

## 2. El recorrido completo de una marca

Este es el flujo que más vale la pena entender, porque toca casi todo el proyecto.

### Paso 1 — Login

1. `ui/login/LoginScreen.kt` muestra el formulario.
2. `LoginViewModel` llama a `ValidarLoginUseCase.kt` para validar formato de código y PIN.
3. Si el formato es correcto, consulta `AttendanceRepository.usuario(id)`.
4. La implementación real (`RoomAttendanceRepository` en `data/repository/AttendanceRepository.kt`) lee de Room (`ConfigDao.usuario`).
5. El usuario queda en `SessionState` (definido en `LoginScreen.kt`), que es un singleton de Hilt.
6. `AppNav.kt` redirige según el `Rol` (`Models.kt`).

### Paso 2 — Marcar entrada o salida

1. `ui/checkin/CheckInScreen.kt` tiene los botones y pide permisos de ubicación/Bluetooth/notificaciones.
2. `CheckInViewModel.marcar(tipo)` llama a `domain/usecase/RegistrarMarcaUseCase.kt`.
3. `RegistrarMarcaUseCase` pide al repositorio:
   - perímetro de la faena (`perimetro`),
   - beacons autorizados (`beacons`),
   - última marca válida (`ultimaMarcaValida`),
   - último hash (`ultimoHash`).
4. Captura sensores:
   - `LocationProvider` (GPS),
   - `BleScanner` (beacons),
   - `SystemStateProvider` (modo desarrollador y hora automática).
   - En `demo` se usan `sensors/FakeSensors.kt`; en `campo`, `sensors/RealSensors.kt`.
5. Arma una `Evidencia` (`domain/model/Models.kt`) y la pasa a `domain/antifraude/RuleEngine.kt`.
6. `RuleEngine` ejecuta las reglas de `domain/antifraude/Rules.kt` y devuelve un `Veredicto`:
   - `RECHAZADA` si falla una regla bloqueante,
   - `SOSPECHOSA` si falla una advertencia,
   - `VÁLIDA` si todo pasa.
7. `HashMarca.kt` calcula el SHA-256 encadenado con el hash anterior.
8. `AttendanceRepository.guardar(...)` guarda en una transacción de Room:
   - la marca (`MarcaDao.insertMarca`),
   - las lecturas BLE (`insertLecturas`),
   - los eventos antifraude (`insertEventos`).
9. `CheckInScreen` muestra el resultado con `AnimatedContent`.
10. Si la marca es sospechosa, `notifications/Notificaciones.kt` lanza una notificación local.
11. `sync/SyncWorker.kt` se encola con `SyncScheduler.enqueue`.

### Paso 3 — Sincronización

1. `SyncWorker.doWork()` llama a `AttendanceRepository.sincronizarMarcas()`.
2. El repositorio toma las marcas `PENDIENTE`/`ERROR` (`MarcaDao.pendientes`), arma los DTOs (`data/remote/Api.kt`) y llama a `ApiService.enviarLote`.
3. En `demo` responde `data/remote/FakeApi.kt`; en `campo`, Retrofit.
4. Según la respuesta, actualiza el estado de sincronización a `ENVIADA` o `ERROR`.
5. Si envió algo, `Notificaciones.sincronizacionCompletada` avisa.

### Paso 4 — Visación del capataz

1. `ui/foreman/ForemanScreen.kt` muestra la última marca de cada operario de la faena.
2. `ForemanViewModel` combina `repo.marcasDeFaena` y `repo.visaciones`.
3. Al aprobar/rechazar, llama a `repo.visar(...)`, que guarda en `VisacionEntity` (`data/local/Entities.kt`).
4. `ui/visacion/DetalleVisacionScreen.kt` + `DetalleVisacionViewModel.kt` muestran el detalle en solo lectura.

### Paso 5 — Horas extra

1. `ui/horasextra/HoraExtraScreen.kt` valida horas y motivo con `SolicitarHoraExtraUseCase.kt`.
2. `HoraExtraViewModel.kt` llama al caso de uso, que guarda en `HoraExtraEntity`.
3. `ui/horasextra/MisHorasExtraScreen.kt` + `MisHorasExtraViewModel.kt` muestran las solicitudes del operario.
4. `ui/horasextra/AprobarHoraExtraScreen.kt` permite al capataz aprobar o rechazar.
5. `ui/horasextra/ResumenHorasExtraScreen.kt` es el contenido reutilizable del resumen.

### Paso 6 — Administración

1. `ui/admin/AdminViewModel.kt` lee todas las marcas y horas extra desde el repositorio.
2. Calcula métricas, porcentajes por faena, alertas y verifica la cadena con `HashMarca.verificarCadena`.
3. `ui/admin/AdminScreen.kt` muestra todo, anima los contadores y permite exportar:
   - CSV con `domain/usecase/ExportarMarcasCsv.kt`,
   - PDF con `domain/usecase/ExportarReportePdf.kt`,
   - y comparte con `FileProvider` (`res/xml/file_paths.xml`).
4. Al tocar una alerta navega a `detalle-visacion/{marcaId}`.

---

## 3. Ruta de estudio recomendada (orden de lectura)

### Fase 0 — Configuración del proyecto

Lee esto solo para entender con qué está construido. No lo memorices.

| Archivo | Qué mirar |
|---|---|
| `settings.gradle.kts` | Nombre del proyecto, módulo `:app`, repositorios. |
| `build.gradle.kts` | Plugins de Android, Kotlin, Compose, KSP y Hilt. |
| `app/build.gradle.kts` | `compileSdk`, `minSdk`, flavors `demo`/`campo`, `BuildConfig` y dependencias. |
| `gradle/libs.versions.toml` | Versiones de Compose, Room, Hilt, Retrofit, etc. |
| `gradle.properties` | Flags de AndroidX y memoria de Gradle. |
| `gradle/wrapper/gradle-wrapper.properties` | Versión de Gradle (8.9). |
| `local.properties` | Ruta local del SDK (no se versiona). |

### Fase 1 — Punto de entrada y modelos

| Archivo | Qué mirar |
|---|---|
| `app/src/main/AndroidManifest.xml` | Permisos, `Application`, `MainActivity`, WorkManager y `FileProvider`. |
| `app/src/debug/AndroidManifest.xml` | Permite HTTP claro solo en debug. |
| `SmartVoltApp.kt` | `@HiltAndroidApp`, canales de notificación y configuración de WorkManager. |
| `MainActivity.kt` | Aplica `SmartVoltTheme` y monta `AppNav`. |
| `domain/model/Models.kt` | Enums y data classes: `Rol`, `TipoMarca`, `EstadoMarca`, `Evidencia`, `Veredicto`, etc. |
| `sensors/Sensors.kt` | Interfaces de sensores, escenarios demo y `DemoState`. |

### Fase 2 — Reglas de negocio puras (la parte más importante)

| Archivo | Qué mirar |
|---|---|
| `domain/antifraude/Rules.kt` | Las 7 reglas antifraude, una por clase. |
| `domain/antifraude/RuleEngine.kt` | Cómo se combinan las reglas y cómo se decide el veredicto. |
| `domain/geo/GeoUtils.kt` | Haversine, punto en polígono y margen de precisión. |
| `domain/usecase/HashMarca.kt` | SHA-256 encadenado y verificación de integridad. |
| `app/src/test/.../RuleEngineTest.kt` | Ejemplos ejecutables de cada regla. |
| `app/src/test/.../GeoUtilsTest.kt` | Ejemplos de distancias y perímetros. |
| `app/src/test/.../HashMarcaTest.kt` | Ejemplos de alteración y enlace roto. |

### Fase 3 — Datos y persistencia

| Archivo | Qué mirar |
|---|---|
| `data/local/Entities.kt` | Las 9 entidades Room y sus relaciones. |
| `data/local/Daos.kt` | Consultas, migraciones 1→2 y 2→3, y `AppDatabase`. |
| `data/remote/Api.kt` | DTOs y `ApiService` de Retrofit. |
| `data/remote/FakeApi.kt` | API simulada de `demo`. |
| `data/repository/AttendanceRepository.kt` | Contrato + implementación Room/Retrofit; guardado transaccional, sync, semilla y horas extra. |
| `di/AppModule.kt` | Inyección de DB, repositorio, API y sensores (fake o reales). |
| `app/src/main/assets/seed.json` | Datos iniciales de la demo. |
| `app/src/test/.../FakeApiServiceTest.kt` | Comportamiento de la API simulada. |

### Fase 4 — Casos de uso y sensores

| Archivo | Qué mirar |
|---|---|
| `domain/usecase/RegistrarMarcaUseCase.kt` | El corazón de la marca: evidencia, reglas, hash y guardado. |
| `domain/usecase/SolicitarHoraExtraUseCase.kt` | Validadores puros de horas/motivo y persistencia. |
| `domain/usecase/ValidarLoginUseCase.kt` | Reglas puras del login. |
| `domain/usecase/ExportarMarcasCsv.kt` | Generación del CSV. |
| `domain/usecase/ExportarReportePdf.kt` | Generación del PDF. |
| `sensors/FakeSensors.kt` | Simulación de GPS/BLE según escenario. |
| `sensors/RealSensors.kt` | GPS real, BLE real y estado del sistema. |
| `app/src/test/.../SolicitarHoraExtraUseCaseTest.kt` | Validaciones de horas extra. |
| `app/src/test/.../ValidarLoginUseCaseTest.kt` | Validaciones de login. |
| `app/src/test/.../HoraExtraTest.kt` | Flujo de solicitar/aprobar/rechazar. |

### Fase 5 — Interfaz, estado y navegación

| Archivo | Qué mirar |
|---|---|
| `ui/theme/Theme.kt` | Material 3: colores claro/oscuro, tipografía y formas. |
| `ui/navigation/AppNav.kt` | Rutas, transiciones y `LocalNavController`. |
| `ui/login/LoginScreen.kt` | `SessionState`, `LoginViewModel` y formulario con validación por campo. |
| `ui/checkin/CheckInScreen.kt` | `CheckInViewModel`, permisos, botones de marca y resultado animado. |
| `ui/foreman/ForemanScreen.kt` | `ForemanViewModel`, panel de presencia y visación. |
| `ui/visacion/DetalleVisacionScreen.kt` | Detalle de una marca y su visación. |
| `ui/visacion/DetalleVisacionViewModel.kt` | Carga marca + visación. |
| `ui/calendario/CalendarioScreen.kt` | Historial por rango. |
| `ui/calendario/CalendarioViewModel.kt` | Agrupación por fecha según rol. |
| `ui/mapa/MapaScreen.kt` | Mapa Mapbox con marcador en el centro de la faena. |
| `ui/mapa/MapaViewModel.kt` | Centro y radio de la faena. |
| `ui/horasextra/HoraExtraScreen.kt` | Formulario de horas extra con íconos y errores por campo. |
| `ui/horasextra/HoraExtraViewModel.kt` | Estado y acciones de horas extra. |
| `ui/horasextra/AprobarHoraExtraScreen.kt` | Supervisión de solicitudes. |
| `ui/horasextra/MisHorasExtraScreen.kt` | Solicitudes del operario. |
| `ui/horasextra/MisHorasExtraViewModel.kt` | Carga de las solicitudes propias. |
| `ui/horasextra/ResumenHorasExtraScreen.kt` | Contenido reutilizable del resumen. |
| `ui/admin/AdminScreen.kt` | Panel admin, métricas animadas, alertas, integridad y exportación. |
| `ui/admin/AdminViewModel.kt` | Cálculo de métricas, alertas y líneas del reporte. |
| `app/src/test/.../HoraExtraViewModelTest.kt` | Estado del ViewModel de horas extra. |
| `app/src/test/.../AdminViewModelTest.kt` | Métricas del panel admin. |
| `app/src/test/.../AdminReporteTest.kt` | Líneas del reporte. |
| `app/src/test/.../CalendarioTest.kt` | Agrupación por fecha. |
| `app/src/test/.../DetalleVisacionViewModelTest.kt` | Detalle de visación. |
| `app/src/test/.../MainDispatcherRule.kt` | Utilidad para probar ViewModels. |

### Fase 6 — Sincronización, notificaciones y recursos

| Archivo | Qué mirar |
|---|---|
| `sync/SyncWorker.kt` | `SyncWorker` y `SyncScheduler`. |
| `notifications/Notificaciones.kt` | Canales y notificaciones. |
| `app/src/main/res/drawable/ic_notificacion.xml` | Ícono de las notificaciones. |
| `app/src/main/res/values/themes.xml` | Tema XML claro. |
| `app/src/main/res/values-night/themes.xml` | Tema XML oscuro. |
| `app/src/main/res/values/strings.xml` | Nombre de la app. |
| `app/src/main/res/xml/file_paths.xml` | Ruta `cache/exports` para compartir CSV/PDF. |
| `app/src/main/assets/map.html` | Mapa Leaflet/OpenStreetMap anterior (sin uso tras migrar a Mapbox). |
| `app/src/test/.../ExportarMarcasCsvTest.kt` | Formato y escape del CSV. |

---

## 4. Si un profesor pide cambios, ¿qué archivos tocaría?

Esta es la parte más útil para una defensa: casi ningún cambio toca un solo archivo. Estas son las cadenas típicas.

### Cambiar reglas de validación

| Si piden... | Archivos |
|---|---|
| Cambiar el máximo de horas o el mínimo del motivo | `SolicitarHoraExtraUseCase.kt` (constantes y validadores), `HoraExtraScreen.kt` (textos), `SolicitarHoraExtraUseCaseTest.kt`. |
| Cambiar el formato del código o el largo del PIN | `ValidarLoginUseCase.kt`, `LoginScreen.kt` (usa `LARGO_PIN`), `ValidarLoginUseCaseTest.kt`, `seed.json` si cambian los códigos. |
| Que el botón de login se comporte distinto | `LoginScreen.kt` (estado de UI), `LoginViewModel` dentro del mismo archivo. |

### Cambiar o agregar reglas antifraude

| Si piden... | Archivos |
|---|---|
| Agregar una regla nueva | `Rules.kt` (nueva clase), `RuleEngine.kt` (agregarla a `porDefecto`), `RuleEngineTest.kt`. Si necesita un dato nuevo: `Models.kt`, `RegistrarMarcaUseCase.kt` y `Sensors.kt`/`RealSensors.kt`. |
| Hacer bloqueante una regla de advertencia | `Rules.kt` (cambiar `Severidad`), `RuleEngineTest.kt`. |
| Cambiar el radio o el margen de la geocerca | `Rules.kt` (`margenMaxM`), `GeoUtils.kt`, `seed.json` (radio), `GeoUtilsTest.kt` / `RuleEngineTest.kt`. |
| Cambiar la secuencia entrada/salida | `Rules.kt` (`SequenceRule`), `RuleEngineTest.kt`. |

### Cambiar la base de datos o las marcas

| Si piden... | Archivos |
|---|---|
| Agregar un campo a la marca (batería, modelo, etc.) | `Entities.kt`, `Daos.kt` (subir versión y crear migración 3→4), `Models.kt` (`Evidencia`), `RegistrarMarcaUseCase.kt`, `HashMarca.kt` si el campo entra al hash, `Api.kt` (`MarcaDto`), `AttendanceRepository.kt`, `ExportarMarcasCsv.kt`, `DetalleVisacionScreen.kt` y tests. |
| Agregar una tabla nueva | `Entities.kt`, `Daos.kt` (DAO + migración + versión), `AppModule.kt` si se expone el DAO, `AttendanceRepository.kt` si se usa desde la UI. |
| Cambiar los datos de la semilla | `assets/seed.json`; si cambia la estructura, `Api.kt` (`SeedDto`) y `AttendanceRepository.kt` (`cargarSemillaSiVacio`). |
| Cifrar la base (SQLCipher) | `app/build.gradle.kts` (dependencia), `AppModule.kt` (constructor de la DB), `Daos.kt` si cambia la inicialización. |

### Cambiar la interfaz

| Si piden... | Archivos |
|---|---|
| Cambiar colores, tipografía o modo oscuro | `ui/theme/Theme.kt`, `res/values/themes.xml`, `res/values-night/themes.xml`. |
| Agregar una pantalla o ruta | `ui/navigation/AppNav.kt`, nueva `Screen.kt` + `ViewModel.kt`, botón de acceso en `CheckInScreen.kt` / `ForemanScreen.kt` / `AdminScreen.kt`, `strings.xml` si hay textos nuevos. |
| Cambiar animaciones o transiciones | `AppNav.kt` (transiciones), `CheckInScreen.kt` (resultado/carga), `AdminScreen.kt` (contadores/alertas), `HoraExtraScreen.kt` (éxito/error). |
| Cambiar textos visibles | La pantalla correspondiente y `res/values/strings.xml`. |
| Cambiar el mapa | `ui/mapa/MapaScreen.kt`, `ui/mapa/MapaViewModel.kt`, `res/values/mapbox_access_token.xml`. |

### Cambiar roles, login o sesión

| Si piden... | Archivos |
|---|---|
| Agregar un rol nuevo | `Models.kt` (`Rol`), `AppNav.kt` (destino), `LoginScreen.kt` (redirección), `HoraExtraViewModel.kt`, `CalendarioViewModel.kt`, `AdminViewModel.kt`, `ForemanScreen.kt`, `seed.json` y tests. |
| Autenticación real (PIN por usuario o biometría) | `ValidarLoginUseCase.kt`, `LoginScreen.kt` (ViewModel), `AttendanceRepository.kt` (`usuario`), `Entities.kt` (campo de hash de PIN + migración), `seed.json`, posiblemente `Api.kt`. |
| Que la sesión persista al cerrar la app | `LoginScreen.kt` (`SessionState`), `MainActivity.kt` o `SmartVoltApp.kt` para cargarla, y `AttendanceRepository.kt` si se guarda en Room/DataStore. |

### Cambiar sincronización, notificaciones o exportación

| Si piden... | Archivos |
|---|---|
| Cambiar cuándo o cómo sincroniza | `SyncWorker.kt`, `AttendanceRepository.kt`, `Api.kt`, `FakeApi.kt`, `AppModule.kt`, `FakeApiServiceTest.kt`. |
| Agregar o cambiar notificaciones | `Notificaciones.kt`, `SmartVoltApp.kt` (canales), `CheckInScreen.kt` (permiso), `SyncWorker.kt` (aviso de sync), `AndroidManifest.xml`, `ic_notificacion.xml`. |
| Agregar columnas al CSV | `ExportarMarcasCsv.kt`, `ExportarMarcasCsvTest.kt`; si sale de la UI, `AdminViewModel.kt`. |
| Cambiar el PDF | `ExportarReportePdf.kt`, `AdminViewModel.kt` (`comoLineas`), `AdminReporteTest.kt`. |
| Cambiar los escenarios demo | `Sensors.kt` (enum `Escenario`), `FakeSensors.kt`, `CheckInScreen.kt` (chips), `AppModule.kt`. |

---

## 5. Ejercicios para practicar (de menor a mayor)

1. **Cambiar el tope de horas** de 12 a 8 en `SolicitarHoraExtraUseCase.kt` y correr `SolicitarHoraExtraUseCaseTest.kt`. Vas a ver que la UI se adapta sola porque usa `MAX_HORAS_TEXTO`.
2. **Hacer bloqueante `DeveloperModeRule`**: cambia `Severidad.ADVERTENCIA` por `Severidad.BLOQUEANTE` en `Rules.kt` y ajusta `RuleEngineTest.kt`.
3. **Agregar un escenario demo** `SIN_GPS` en `Sensors.kt`, `FakeSensors.kt` y `CheckInScreen.kt`.
4. **Agregar un campo `bateria` a la marca**: sigue la cadena de la tabla anterior (entidad → migración → evidencia → hash → DTO → repo → CSV → test). Es el ejercicio más completo para entender el proyecto.
5. **Agregar una pantalla "Acerca de"**: nueva Screen + ViewModel, ruta en `AppNav.kt` y botón en `CheckInScreen.kt`.

---

## 6. Chuleta para la defensa

Si te preguntan “¿dónde está…?”:

| Concepto | Archivo |
|---|---|
| Reglas antifraude | `domain/antifraude/Rules.kt` y `RuleEngine.kt` |
| Geocerca | `domain/geo/GeoUtils.kt` |
| Hash encadenado | `domain/usecase/HashMarca.kt` |
| Flujo de una marca | `domain/usecase/RegistrarMarcaUseCase.kt` |
| Validación de login | `domain/usecase/ValidarLoginUseCase.kt` |
| Validación de horas extra | `domain/usecase/SolicitarHoraExtraUseCase.kt` |
| Persistencia | `data/local/Entities.kt`, `Daos.kt`, `AttendanceRepository.kt` |
| Sensores reales/simulados | `sensors/RealSensors.kt`, `FakeSensors.kt` |
| Sincronización | `sync/SyncWorker.kt` |
| Notificaciones | `notifications/Notificaciones.kt` |
| Navegación | `ui/navigation/AppNav.kt` |
| Tema Material 3 | `ui/theme/Theme.kt` |
| Estado de pantallas | Los `*ViewModel.kt` de cada carpeta `ui/` |

Frase para cerrar:

> “La UI solo muestra estado; las reglas están en `domain/`, la persistencia en `data/`, y los sensores se inyectan según el flavor. Por eso puedo cambiar una regla o un sensor sin tocar la pantalla.”
