# Planificación del proyecto · Smart-Volt Asistencia

Este documento resume la planificación usada por el equipo y su relación con los commits del repositorio. El tablero operativo del equipo se gestiona en **Trello** (agregar el enlace del tablero en el README); este archivo sirve como respaldo para la evaluación y para reconstruir el tablero si fuera necesario.

## Tablero sugerido (columnas)

| Columna | Contenido |
| --- | --- |
| **Backlog** | Requisitos y mejoras detectadas en la revisión de rúbrica. |
| **Por hacer** | Tareas priorizadas para la entrega. |
| **En progreso** | Trabajo activo de cada integrante. |
| **En revisión** | Cambios listos para probar en dispositivo/emulador. |
| **Hecho** | Tareas integradas y verificadas con build + tests. |

## Tareas y evidencias

| Tarea | Responsable sugerido | Evidencia en el repo |
| --- | --- | --- |
| Modelo de dominio, reglas antifraude y motor de evaluación | Equipo | `domain/antifraude/Rules.kt`, `RuleEngine.kt`, `RuleEngineTest.kt` |
| Persistencia local con Room y migraciones | Equipo | `data/local/Entities.kt`, `Daos.kt`, `AppDatabase.kt` |
| Repositorio y carga de semilla demo | Equipo | `data/repository/AttendanceRepository.kt`, `assets/seed.json` |
| Login con PIN y sesión por rol | Equipo | `ui/login/LoginScreen.kt`, `SessionState` |
| Marcación de entrada/salida y veredicto | Equipo | `ui/checkin/CheckInScreen.kt`, `RegistrarMarcaUseCase.kt` |
| GPS real con respaldo `LocationManager` | Equipo | `sensors/RealSensors.kt` |
| Escaneo BLE de iBeacons | Equipo | `sensors/RealSensors.kt` |
| Panel de capataz y visación | Equipo | `ui/foreman/ForemanScreen.kt`, `ui/visacion/DetalleVisacionScreen.kt` |
| Horas extra: solicitud, resumen y aprobación | Equipo | `ui/horasextra/*`, `domain/usecase/SolicitarHoraExtraUseCase.kt` |
| Calendario mensual de marcas | Equipo | `ui/calendario/CalendarioScreen.kt`, `domain/calendario/CalendarioUtils.kt` |
| Administración, métricas y exportaciones | Equipo | `ui/admin/AdminScreen.kt`, `ExportarMarcasCsv.kt`, `ExportarReportePdf.kt` |
| Mapa de faena con Mapbox | Equipo | `ui/mapa/MapaScreen.kt`, `assets/map.html` |
| Validación centralizada de formularios | Equipo | `domain/validation/ValidacionFormularios.kt`, `ValidacionFormulariosTest.kt` |
| Animaciones y navegación coherente | Equipo | `ui/navigation/AppNav.kt`, pantallas con `AnimatedContent` / `AnimatedVisibility` |
| Aviso de entrada duplicada / salida sin entrada | Equipo | `ui/checkin/CheckInScreen.kt` (Snackbar + regla `SECUENCIA`) |
| Documentación y credenciales demo | Equipo | `README.md`, `docs/PLANIFICACION.md` |

## Commits representativos

| Commit | Contenido |
| --- | --- |
| `2bc70e9` | Respaldo antes de aplicar cambios. |
| `3992690` | Correcciones y funcionalidades iniciales. |
| `82e7e38` | Integración de la rama de un integrante. |
| `6194005` | Avances de interfaz y lógica. |
| `ecba57b` | Calendario y grilla mensual. |

> Al agregar nuevas tareas, crea la tarjeta en Trello, asígnala a un integrante y enlaza el commit o pull request correspondiente. La rúbrica IE 2.3.2 evalúa la actividad real y la distribución de tareas, no solo la existencia del repositorio.
