# Correcciones de Mapa y Beacon - SmartVolt Asistencia

## Resumen de Problemas Identificados y Solucionados

### Problema 1: Mapa no se muestra en el teléfono

**Causas raíz identificadas:**
1. ❌ **Error de sintaxis CSS** en `map.html` - propiedades separadas por `,` en lugar de `;`
2. ❌ **Error de tipo Kotlin** en `MapaScreen.kt` - `string?` en lugar de `String?` en el parámetro `url`
3. ❌ **API WebView obsoluta** - `onReceivedError` con firma antigua en Android 12+
4. ⚠️ **Dependencia de assets locales** - WebViewAssetLoader no sirve correctamente los recursos externos de Mapbox CDN

**Soluciones implementadas:**
- ✅ `map.html` corregido: reemplazado `,` por `;` en todas las reglas CSS
- ✅ `MapaScreen.kt` actualizado: tipo `String?` correcto, API moderna de WebView
- ✅ Mapbox se carga desde CDN (`api.mapbox.com/mapbox-gl-js/v2.15.1/mapbox-gl.js`)
- ✅ Leaflet + OpenStreetMap como fallback cuando Mapbox falla
- ✅ Mejor logging con tag `SmartVoltMap` para depuración

### Problema 2: Beacon no se detecta

**Análisis del flujo completo:**

1. **Permisos AndroidManifest.xml** ✅ CORRECTOS
   - `BLUETOOTH_SCAN` sin `neverForLocation` (requerido para Android 12+)
   - `BLUETOOTH_CONNECT` para Android 12+
   - `BLUETOOTH` / `BLUETOOTH_ADMIN` para Android 11 y previo
   - Ubicación: `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`

2. **Solicitud de permisos en CheckInScreen.kt** ✅ CORRECTO
   - Solicita permisos críticos: `ACCESS_FINE_LOCATION`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
   - Muestra mensaje si faltan permisos

3. **Parseo de UUID en RealSensors.kt** ✅ CORRECTO
   - El beacon Apple usa Manufacturer ID `0x004C`
   - Formato iBeacon: `02 15` + UUID (16 bytes) + Major (2) + Minor (2) + TX Power (1)
   - La extracción del UUID usando `ByteBuffer.wrap(data, 2, 16)` es correcta

4. **Posibles causas del no-detección:**
   - ❌ Beacon no transmitiendo o con batería baja
   - ❌ Rango de RSSI insuficiente (< -80 dBm)
   - ❌ UUID no coincide exactamente
   - ❌ Permisos no concedidos explícitamente por el usuario
   - ❌ Modo "No siempre" para ubicación en Ajustes del sistema

---

## Archivos Modificados

### 1. `app/src/main/assets/map.html`
**Cambios:**
- ✅ Corregido error de sintaxis CSS (`,` → `;` en declaraciones)
- ✅ Mapbox GL JS v2.15.1 desde CDN
- ✅ Fallback a Leaflet/OSM con mensaje visible si se usa

```html
<!-- ANTES (ERROR) -->
#aviso {
    position: absolute; z-index: 1000; left: 8px; right: 8px; top: 8px,
    background: rgba(255, 255, 255, 0.96); padding: 8px 10px; border-radius: 6px,
}

<!-- DESPUÉS (CORRECTO) -->
#aviso {
    position: absolute; z-index: 1000; left: 8px; right: 8px; top: 8px;
    background: rgba(255, 255, 255, 0.96); padding: 8px 10px; border-radius: 6px;
}
```

### 2. `app/src/main/java/cl/smartvolt/asistencia/ui/mapa/MapaScreen.kt`
**Cambios:**
- ✅ Import agregado: `WebResourceError`
- ✅ Corregido `string?` → `String?` (error de tipo)
- ✅ Firma corregida: `onReceivedError(view, request, error)` para Android 12+
- ✅ WebViewClient unificado: combina `shouldInterceptRequest`, `onPageFinished`, `onReceivedError`
- ✅ Logging: tag `SmartVoltMap` con mensajes detallados
- ✅ Eliminado `webViewClient` duplicado

**Error de compilación corregido:**
```
Error: 'onReceivedError' overrides nothing
Ubicación: MapaScreen.kt:173
Causa: Firma antigua con errorCode: Int en lugar de los parámetros modernos (view, request, error)
```

### 3. `app/src/main/AndroidManifest.xml`
**Estado:** ✅ CORRECTO (ya tenía los permisos adecuados)

---

## Cómo Probar en Android Studio

### 1. Probar el Mapa

```bash
# Compilar variante campo (sensores reales)
./gradlew campoDebug install

# Ver Logcat con filtro
# Filtro: "SmartVoltMap"
```

**Resultado esperado:**
- Al iniciar, el mapa muestra un círculo azul con el radio de la faena
- Mensaje "Mapa de la faena" en la barra superior
- Si falla Mapbox, se ve: "Mapa alternativo (OpenStreetMap). [motivo]"

### 2. Probar el Beacon

**Configuración del beacon (iOS Locate Beacon):**
- UUID: `20FDCED4-000F-5342-B70D-D3D16FD767FE`
- Major: 1, Minor: 1
- Transmisión en modo "Broadcast"

**Pasos:**
1. Compilar e instalar variante `campoDebug`
2. Ir a pantalla "Marcar asistencia"
3. Tap "Probar beacon (8 s)"
4. Verificar salida: "Beacon autorizado detectado: 20FDCED4-000F-5342-B70D-D3D16FD767FE (-XX dBm)"
5. Tap "MARCAR ENTRADA"
6. Verificar resultado: VALIDA o SOSPECHOSA

**Verificar Logcat:**
- Filtro: "SmartVoltBLE"
- Debe ver: `iBeacon detectado: 20FDCED4-000F-5342-B70D-D3D16FD767FE RSSI=-XX`

### 3. Verificar Permisos

```bash
# Desde Android Studio:
# Tools > App Inspection > Permissions
# O desde el dispositivo:
# Ajustes > Aplicaciones > SmartVolt > Permisos
```

**Permisos requeridos:**
- ✅ Ubicación: FINE + COARSE (deben estar "Permitidos mientras usas la app")
- ✅ Bluetooth: Scan + Connect (deben estar "Permitidos")

---

## Estructura de Dependencias

```
CheckInScreen.kt
    └── solicita permisos (ACCESS_FINE_LOCATION, BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
        └── si faltan permisos → muestra mensaje y pide permiso
            └── si los permisos están concedidos → ejecuta marca

RegistrarMarcaUseCase.kt
    ├── location.obtener(perimetro.centro)  // GPS (8s timeout)
    └── ble.escanear(beacons)               // BLE (6s timeout)
        └── Ambas capturas en paralelo con coroutineScope

RuleEngine
    ├── MockLocationRule (BLOQUEANTE)
    ├── SequenceRule (BLOQUEANTE)
    ├── GeofenceRule (ADVERTENCIA)
    ├── BeaconRule (ADVERTENCIA)
    └── PresenciaFisicaRule (BLOQUEANTE)  ← Combina GeofenceRule y BeaconRule
```

---

## Preguntas Frecuentes

**Q: ¿Por qué necesito internet para el mapa?**
A: Mapbox GL JS necesita descargar un estilo de mapa (streets-v12) y sus fuentes. Sin internet, se usa Leaflet/OSM que también requiere internet para los tiles.

**Q: ¿Puedo usar el mapa offline?**
A: Sí, pero necesitarías:
1. Descargar el estilo de Mapbox usando `mapbox download` o herramientas como TileServer
2. Servir los tiles desde assets o un servidor local
3. Actualizar map.html para usar el estilo local

**Q: ¿Por qué el beacon no se detecta si está cerca?**
A: Verifica:
1. El UUID coincide exactamente (mayúsculas/minúsculas no importan)
2. El RSSI actual es > -80 dBm
3. Bluetooth está encendido y en uso
4. El beacon está en modo "advertising" continuo
5. Los permisos están concedidos explícitamente
6. No hay "Modo desarrollador" activo en el dispositivo

**Q: ¿Por qué veo "Mapbox rechazó el token"?**
A: El token de Mapbox está inválido, restringido o caducado. Reemplaza `MAPBOX_TOKEN` en `map.html` con un token válido de tu cuenta Mapbox.

**Q: ¿Por qué el mapa se queda en blanco?**
A: Posibles causas:
- WebGL no soportado (verifica con `mapboxgl.supported()`)
- Conexión a internet caída
- CORS bloqueado por WebView (verifica Logcat "SmartVoltMap")

---

## Referencias Técnicas

- [Formato iBeacon](https://developer.apple.com/ibeacon/Getting-Started-with-iBeacon.pdf)
- [Android BLE Scanning](https://developer.android.com/guide/topics/connectivity/bluetooth/permissions)
- [Mapbox GL JS](https://docs.mapbox.com/mapbox-gl-js/)
- [Leaflet.js](https://leafletjs.com/)
- [WebViewAssetLoader](https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader)
- [Android 12+ Bluetooth Permissions](https://developer.android.com/guide/topics/connectivity/bluetooth/permissions#required-permissions)