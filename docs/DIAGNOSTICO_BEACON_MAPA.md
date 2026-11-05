# Guía de Diagnóstico - Mapa y Beacon

## Si el mapa no carga

### Paso 1: Verificar Logcat
```
Filtro en Android Studio Logcat:
- "SmartVoltMap" (tag)
- "WebView" (para errores de carga)
- "mapbox" (mensajes de Mapbox)
```

### Paso 2: Mensajes de error comunes

| Mensaje | Causa | Solución |
|---------|-------|----------|
| "Mapa alternativo (OpenStreetMap). Mapbox rechazó el token..." | Token inválido | Usar token válido o conectar a internet |
| "Este equipo no soporta WebGL para Mapbox" | Dispositivo antiguo | Usar variante demo o actualizar |
| "Error de mapa: [referenced CSS]" | Error de sintaxis CSS | Revisar comillas y puntos y comas |
| "Página cargada: file:///..." | Carga local fallida | Verificar WebViewAssetLoader |

### Paso 3: Verificar Internet
- El mapa requiere conexión a internet para descargar Mapbox tiles
- Prueba con modo avión desactivado

---

## Si el beacon no se detecta

### Paso 1: Verificar permisos
Abre una terminal y ejecuta:
```bash
adb shell dumpsys bluetooth_manager | grep -A 5 "Scan mode"
```

### Paso 2: Usar nRF Connect
- Instala nRF Connect desde Google Play
- Busca servicios BLE activos
- Debes ver tu beacon con el UUID correcto

### Paso 3: Verificar Logcat
```
Filtro: "SmartVoltBLE"
```

### Mensajes esperados al escanear:
```
I/SmartVoltBLE: Iniciando escaneo BLE por 6000ms (4 beacons autorizados)
D/SmartVoltBLE: iBeacon detectado: 20FDCED4-000F-5342-B70D-D3D16FD767FE RSSI=-58
W/SmartVoltBLE: Escaneo BLE sin resultados  (si no detecta nada)
```

### Mensajes de error comunes

| Mensaje | Causa | Solución |
|---------|-------|----------|
| "Permiso BLUETOOTH_SCAN no concedido" | Permiso denegado | Ir a Ajustes > Permisos > Conceder |
| "Permiso BLUETOOTH_CONNECT no concedido" | Permiso denegado | Ir a Ajustes > Permisos > Conceder |
| "Bluetooth apagado; no se puede escanear" | Bluetooth apagado | Encender Bluetooth |
| "Ubicación desactivada; BLE no puede escanear" | Ubicación en Android 11- | Activar ubicación |

---

## Verificación rápida desde la app

En la pantalla de "Marcar asistencia", hay una sección "Diagnóstico del beacon":

1. Toca "Probar beacon (8 s)"
2. Espera a que aparezca el resultado
3. Si ves beacons detectados pero no autorizados:
   - El UUID del beacon no coincide
   - El RSSI es muy bajo (< -80 dBm)
4. Si no ves ningún beacon:
   - Bluetooth apagado
   - Permisos denegados
   - Beacon no transmitiendo

---

## Para desarrolladores (Android Studio)

### Verificar que la variante correcta está instalada

```
Build > Select Build Variant > campoDebug
```

La variante `demo` usa sensores simulados y no escanea Bluetooth real.

### Verificar permisos en tiempo de ejecución

```kotlin
// En CheckInScreen.kt
private fun faltanPermisos(c: Context) = !BuildConfig.USE_FAKE_SENSORS && permisosCriticos()
    .any { ContextCompat.checkSelfPermission(c, it) != PackageManager.PERMISSION_GRANTED }
```

Si `USE_FAKE_SENSORS = true` (variante demo), no se piden permisos.

---

## Preguntas frecuentes

**¿Por qué el mapa se ve pero no responde a toques?**
- El WebView tiene `user-scalable=no` y no permite zoom
- Es intencional para esta pantalla de mapa de faena

**¿Cómo saber si mi beacon está transmitiendo?**
- Usa nRF Connect o similar
- Debes ver un servicio GATT con el UUID del beacon
- El RSSI debe ser > -80 dBm en test

**¿Puedo usar el mapa sin internet?**
No directamente. Mapbox y Leaflet necesitan descargar tiles. Para uso offline:
1. Usar MAPBOX_DOWNLOAD para obtener tiles
2. Servir desde servidor local
3. Modificar map.html para apuntar al servidor local