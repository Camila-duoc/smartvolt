# Smart-Volt Asistencia

Aplicación Android para registrar entrada y salida de trabajadores en faena, validando cada marca con reglas antifraude, sensores del dispositivo y almacenamiento local. El objetivo no es solo marcar asistencia, sino **confiar en que la marca ocurrió realmente en el lugar y momento correctos**.

Proyecto académico desarrollado con Kotlin, Jetpack Compose y Material 3.

## Integrantes

- **Camila Cerda**
- **Christopher Campos**

## Descripción del proyecto

Smart-Volt Asistencia está pensada para tres roles:

- **Operario:** marca su entrada o salida, revisa su historial y solicita horas extra.
- **Capataz:** supervisa la presencia de su faena, visa marcas sospechosas y resuelve solicitudes de horas extra.
- **Administración:** revisa métricas globales, alertas, integridad de los registros y exporta reportes.

Cada marca se valida con reglas como ubicación simulada, geocerca, proximidad a balizas Bluetooth, secuencia entrada/salida, modo desarrollador y coherencia del reloj. El resultado puede ser **VÁLIDA**, **SOSPECHOSA** o **RECHAZADA**. Además, cada registro se encadena con SHA-256 para detectar alteraciones.

La app funciona **offline-first**: las marcas se guardan en Room y se sincronizan con WorkManager cuando hay conexión.

## Funcionalidades implementadas

### Autenticación y roles

- Login con código de usuario y PIN.
- Validación por campo: formato del código y PIN de 4 dígitos.
- Mensajes de error junto a cada campo y error general de autenticación.
- Redirección automática según el rol: operario, capataz o administración.
- Sesión compartida en memoria mediante `SessionState`.

### Marcaje de asistencia

- Botones grandes de **MARCAR ENTRADA** y **MARCAR SALIDA**, pensados para uso en terreno.
- Solicitud de permisos de ubicación, Bluetooth y notificaciones en el momento de uso.
- Captura de ubicación con Fused Location Provider y respaldo con `LocationManager`.
- Escaneo de balizas BLE/iBeacon y lectura de RSSI.
- Validación antifraude antes de guardar la marca.
- Resultado animado con estado y detalle de cada regla evaluada.
- Indicador de marcas pendientes de sincronización.

### Validación antifraude

Reglas implementadas:

- Detección de ubicación simulada (*mock location*).
- Geocerca: ubicación dentro del perímetro de la faena, con margen configurable.
- Proximidad a balizas Bluetooth autorizadas.
- Secuencia lógica de entrada y salida.
- Presencia física y coherencia de la evidencia.
- Detección de modo desarrollador activo.
- Validación del reloj del dispositivo.

Las reglas bloqueantes generan un rechazo; las de advertencia generan una marca sospechosa.

### Integridad de los registros

- Hash SHA-256 encadenado por usuario usando `hashPrevio` y `hash`.
- Verificación de toda la cadena desde administración.
- Detección de registros alterados o enlaces rotos.

### Persistencia y sincronización

- Base de datos local Room (`smartvolt.db`, versión 4).
- 10 entidades: usuarios, faenas, frentes, beacons, marcas, lecturas BLE, eventos antifraude, visaciones, horas extra y ubicación compartida.
- Migraciones 1→2, 2→3 y 3→4.
- Carga inicial desde `assets/seed.json` con faenas, frentes, beacons, usuarios y marcas de ejemplo.
- Marcas nuevas guardadas como `PENDIENTE`.
- Sincronización con WorkManager al recuperar conexión, con backoff exponencial.
- Notificación local al completar la sincronización.

### Horas extra

- Solicitud de horas extra con fecha, cantidad de horas y motivo.
- Validación por campo: máximo 12 horas por solicitud y motivo de al menos 10 caracteres.
- Listado de solicitudes propias para el operario.
- Aprobación o rechazo por parte de capataz/administración.
- Resumen de solicitudes por estado.

### Historial y calendario

- Calendario mensual con marcas agrupadas por día.
- Colores semánticos según el estado del día: válido, sospechoso, rechazado o sin marcas.
- Leyenda de estados y detalle de las marcas de un día.
- Vista adaptada al rol: historial propio, de la faena o general.

### Mapa y ubicación compartida

- Mapa de la faena con Mapbox Maps SDK.
- Visualización de la ubicación compartida.
- Administración puede seleccionar un punto en el mapa y guardarlo para todos los roles.
- Operarios y capataces solo consultan la ubicación.

### Panel de capataz

- Presencia por faena con la última marca de cada operario.
- Conteo de marcas sospechosas pendientes de visación.
- Aprobación o rechazo de marcas sospechosas.
- Acceso al detalle de la marca y su visación.
- Acceso a horas extra, calendario y mapa.

### Panel de administración

- Métricas animadas de marcas: total, válidas, sospechosas, rechazadas y pendientes de sincronización.
- Métricas de horas extra: solicitudes, pendientes, aprobadas y rechazadas.
- Porcentaje de marcas no válidas por faena.
- Alertas recientes con navegación al detalle.
- Verificación del estado de la cadena de hashes.
- Exportación de marcas a CSV.
- Exportación de resumen a PDF.
- Uso de `FileProvider` y selector de aplicaciones del sistema para compartir archivos.

### Interfaz y experiencia de usuario

- Jetpack Compose con Material 3.
- Tema claro y oscuro automático según el sistema.
- Colores semánticos: verde para válida, ámbar para sospechosa y rojo para rechazada.
- Tipografía y formas centralizadas en el tema.
- Transiciones animadas entre pantallas.
- Animaciones funcionales para carga, veredictos, errores, éxito y contadores.
- Íconos con `contentDescription` y botones de 88 dp para accesibilidad y uso con guantes.

### Variantes de compilación

- **demo:** sensores GPS/BLE simulados y API simulada. Ideal para presentaciones sin hardware.
- **campo:** sensores reales y Retrofit contra `API_BASE_URL`.

## Escenarios de demostración

En la variante `demo`, la pantalla de marcaje permite cambiar el escenario simulado:

| Escenario | Comportamiento esperado |
|---|---|
| `VALIDA` | Ubicación y beacon correctos; marca válida. |
| `FAKE_GPS` | Ubicación simulada detectada; marca rechazada. |
| `FUERA_PERIMETRO` | Ubicación fuera de la geocerca; marca rechazada. |
| `SIN_BEACON` | Dentro del perímetro pero sin beacon; marca sospechosa. |
| `MODO_DESARROLLADOR` | Modo desarrollador activo; marca sospechosa. |

## Stack tecnológico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Arquitectura:** MVVM + separación por capas
- **Inyección de dependencias:** Hilt
- **Persistencia local:** Room
- **Sincronización:** WorkManager
- **Red:** Retrofit + Kotlinx Serialization
- **Mapas:** Mapbox Maps SDK + maps-compose
- **Ubicación:** Google Play Services Location + `LocationManager`
- **Bluetooth:** BLE / iBeacon
- **Pruebas:** JUnit + kotlinx-coroutines-test
- **SDK:** `compileSdk 35`, `minSdk 26`, `targetSdk 35`
- **Java:** 17

## Estructura del proyecto

```text
app/src/main/java/cl/smartvolt/asistencia/
├── MainActivity.kt, SmartVoltApp.kt
├── data/
│   ├── remote/          # ApiService (Retrofit) y FakeApi de demo
│   └── repository/      # AttendanceRepository + implementación Room
├── di/                  # AppModule (Hilt)
├── domain/
│   ├── antifraude/      # RuleEngine y reglas antifraude
│   ├── calendario/      # utilidades de calendario
│   ├── geo/             # cálculos de geocerca
│   └── usecase/         # registrar marca, hash, login, horas extra, exportar
├── modelo/              # Modelos, Entidades, DAOs y DTOs
├── notifications/       # canales y notificaciones locales
├── sensors/             # sensores reales y simulados
├── sync/                # SyncWorker y SyncScheduler
├── ui/
│   ├── compoments/      # componentes reutilizables
│   ├── navigation/      # AppNav
│   ├── theme/           # tema Material 3
│   └── view/            # pantallas Compose
└── viewmodel/           # AppDatabase y ViewModels
```

## Cómo ejecutar

### Requisitos

- Android Studio instalado.
- JDK 17.
- Android SDK 35.
- Emulador o dispositivo con Android 8.0 (API 26) o superior.
- Conexión a internet para cargar los mosaicos del mapa.

### Android Studio (recomendado)

1. Clonar el repositorio:

   ```bash
   git clone https://github.com/Camila-duoc/smartvolt.git
   ```

2. Abrir la carpeta del proyecto en Android Studio.

3. Esperar a que Gradle sincronice las dependencias.

4. Seleccionar la variante de compilación `demoDebug`:
   - Menú **Build → Select Build Variant**.
   - Elegir `demoDebug`.

5. Ejecutar la app con **Run ▶** sobre un emulador o dispositivo.

6. Al iniciar, conceder los permisos de ubicación, Bluetooth y notificaciones cuando corresponda.

> La variante `demo` usa sensores y API simulados, por lo que no necesita hardware real ni un backend para funcionar.

### Línea de comandos (opcional)

El repositorio no incluye `gradlew`, por lo que se recomienda usar Android Studio o un Gradle 8.9 instalado localmente con JDK 17:

```bash
gradle :app:assembleDemoDebug
gradle :app:installDemoDebug
```

Para compilar la variante de campo:

```bash
gradle :app:assembleCampoDebug
```

### Credenciales de demostración

| Rol | Código | PIN |
|---|---|---|
| Operario | `OP-0001` | `1234` |
| Capataz | `CAP-0001` | `1234` |
| Administración | `ADM-0001` | `1234` |

También existen otros usuarios de ejemplo en `app/src/main/assets/seed.json`.

## Pruebas

Las pruebas unitarias están en `app/src/test/java/cl/smartvolt/asistencia/`.

Desde Android Studio:

- Clic derecho sobre la carpeta `test` → **Run 'Tests in ...'**.

Desde Gradle:

```bash
gradle :app:testDemoDebugUnitTest
```

Las pruebas cubren, entre otros:

- Validación de login y horas extra.
- Reglas antifraude y geocercas.
- Hash encadenado y detección de alteraciones.
- Calendario y agrupación por fecha.
- Métricas y reportes de administración.
- ViewModels de horas extra, mapa, visación y administración.
- Exportación CSV y API simulada.

## Limitaciones conocidas

- No hay backend real: en `demo` la API es simulada y en `campo` se espera un servidor en `API_BASE_URL`.
- La autenticación es de demostración, con PIN fijo `1234`.
- La sesión vive en memoria y no persiste al cerrar la app.
- La base de datos aún no está cifrada; SQLCipher queda como mejora futura.
- El reloj de referencia no se ancla a la hora de un servidor.
- El mapa requiere internet para descargar los mosaicos.

## Repositorio

- GitHub: [https://github.com/Camila-duoc/smartvolt](https://github.com/Camila-duoc/smartvolt)
