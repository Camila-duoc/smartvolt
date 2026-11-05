package cl.smartvolt.asistencia.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import cl.smartvolt.asistencia.domain.model.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.resume

/**
 * PROVEEDOR DE UBICACIÓN REAL usando Fused Location Provider + GPS del sistema.
 *
 * Arquitectura de Ubicación:
 * ==========================
 * 1. FusedLocationProviderImpl (Google Play Services)
 *    - Usa el sensor combinado de ubicación de Google (GPS + WiFi + red)
 *    - Más rápido y preciso en dispositivos con GMS
 *    - Fallback automático a PlatformLocationProvider si GMS falla
 *
 * 2. PlatformLocationProvider (LocationManager del sistema)
 *    - Usa los sensores del sistema Android directamente
 *    - Funciona en emuladores AOSP y dispositivos sin Google Play Services
 *    - Fallback cuando Fused falla con SERVICE_INVALID (código 17)
 *
 * Flujo de uso típico:
 * - App solicita permisos de ubicación
 * - RegistrarMarcaUseCase llama a location.obtener()
 * - Se captura ubicación y se evalúa con el RuleEngine
 */
class FusedLocationProviderImpl(private val context: Context) : LocationProvider {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val plataforma by lazy { PlatformLocationProvider(context) }

    @SuppressLint("MissingPermission") // El permiso se solicita en la UI antes de marcar.
    override suspend fun obtener(referencia: LatLon): UbicacionCapturada? {
        // Sin GMS válido ni siquiera se intenta Fused: se va directo al GPS del sistema.
        if (!googlePlayServicesDisponible()) {
            return plataforma.obtener(referencia)
        }

        val token = CancellationTokenSource()
        return try {
            val loc = withTimeoutOrNull(8_000) {
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
            }
            // Si Fused no entrega una lectura, se intenta el respaldo antes de rendirse.
            loc?.aUbicacionCapturada() ?: plataforma.obtener(referencia)
        } catch (e: ApiException) {
            // Código 17 (SERVICE_INVALID) u otro fallo de GMS: usar el GPS del sistema.
            plataforma.obtener(referencia)
        } catch (e: SecurityException) {
            // Permiso denegado: el respaldo también fallaría, así que se propaga.
            throw e
        } finally {
            token.cancel()
        }
    }

    private fun googlePlayServicesDisponible(): Boolean =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
}

/**
 * Respaldo basado en `LocationManager` (GPS/red). No depende de Google Play Services,
 * por lo que funciona en emuladores AOSP y equipos sin GMS.
 *
 * Características:
 * - Usa GPS cuando está disponible para mayor precisión
 * - Fallback a red (NETWORK_PROVIDER) si GPS no está activo
 * - Timeout de 8 segundos para evitar bloqueos
 */
class PlatformLocationProvider(private val context: Context) : LocationProvider {
    @SuppressLint("MissingPermission")
    override suspend fun obtener(referencia: LatLon): UbicacionCapturada? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        // Ubicación desactivada a nivel de sistema: no hay proveedor que consultar.
        if (!LocationManagerCompat.isLocationEnabled(lm)) return null

        // Se prefiere GPS por precisión; si no está activo, se usa la red.
        val proveedor = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }

        // getCurrentLocation funciona en API 30+ y tiene compatibilidad retroactiva
        // en androidx.core. Se cancela solo al vencer el timeout.
        val loc = withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cancelacion = CancellationSignal()
                LocationManagerCompat.getCurrentLocation(
                    lm,
                    proveedor,
                    cancelacion,
                    ContextCompat.getMainExecutor(context),
                ) { location ->
                    if (cont.isActive) cont.resume(location)
                }
                cont.invokeOnCancellation { cancelacion.cancel() }
            }
        }
        // Último recurso: una lectura previa del mismo proveedor.
        return (loc ?: lm.getLastKnownLocation(proveedor))?.aUbicacionCapturada()
    }
}

/** Convierte una `Location` del sistema al modelo de dominio. */
private fun Location.aUbicacionCapturada(): UbicacionCapturada {
    val mock = if (Build.VERSION.SDK_INT >= 31) {
        isMock
    } else {
        @Suppress("DEPRECATION") isFromMockProvider
    }
    return UbicacionCapturada(latitude, longitude, accuracy, mock)
}

/**
 * ESCANNER BLE - Lectura de iBeacons iBeacon (Eddystone/AltBeacon)
 *
 * formato de iBeacon:
 * ==================
 * - Manufacturer ID: 0x004C (Apple)
 * - Tipo: 0x02 (Tipo de paquete)
 * - Sub-tipo: 0x15 (UUID Length = 16)
 * - UUID: 16 bytes (identificador del beacon)
 * - Major: 2 bytes (identificador de grupo/frente)
 * - Minor: 2 bytes (identificador individual)
 * - TX Power: 1 byte (potencia a 1m)
 *
 * Notas de compatibilidad:
 * - Android 12+ (API 31+): requiere BLUETOOTH_SCAN y BLUETOOTH_CONNECT
 *   Importante: NO usar neverForLocation, de lo contrario el escaneo
 *   filtrará anuncios que podrían revelar ubicación.
 * - Android 11 e inferior (API 30-): requiere permiso de ubicación
 * - Se registra el resultado en Logcat con el tag `SmartVoltBLE` para diagnosticar.
 *
 * Flujo de escaneo:
 * 1. Se solicita permiso BLUETOOTH_SCAN (Android 12+) y BLUETOOTH_CONNECT
 * 2. Se verifica que Bluetooth esté habilitado
 * 3. Se inicia escaneo en modo LOW_LATENCY (rápido pero consume más batería)
 * 4. Se acumulan lecturas RSSI por UUID durante la ventana de tiempo
 * 5. Se calcula RSSI promedio y número de muestras
 */
class BleScannerImpl(private val context: Context) : BleScanner {
    @SuppressLint("MissingPermission")
    override suspend fun escanear(autorizados: List<BeaconAutorizado>, ventanaMs: Long): List<LecturaBle> {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter
        if (adapter == null || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Este equipo no tiene soporte Bluetooth LE")
            return emptyList()
        }

        // Verificar permisos Android 12+
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Permiso BLUETOOTH_CONNECT no concedido")
            return emptyList()
        }

        if (!adapter.isEnabled) {
            Log.w(TAG, "Bluetooth apagado; no se puede escanear")
            return emptyList()
        }

        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Permiso BLUETOOTH_SCAN no concedido")
            return emptyList()
        }

        // En Android 11 e inferior el escaneo BLE exige la ubicación activa.
        if (Build.VERSION.SDK_INT <= 30) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm == null || !LocationManagerCompat.isLocationEnabled(lm)) {
                Log.w(TAG, "Ubicación desactivada; BLE no puede escanear en Android 11 o inferior")
                return emptyList()
            }
        }

        val scanner = adapter.bluetoothLeScanner ?: run {
            Log.w(TAG, "BluetoothLeScanner no disponible")
            return emptyList()
        }

        // Mapa para acumular RSSI por UUID del beacon detectado
        val acumulado = mutableMapOf<String, MutableList<Int>>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // 1. Obtener datos manufacturer específicos (Apple usa 0x004C como manufacturer ID)
                //    Los iBeacons empiezan con el prefijo 0x02 0x15 después del header.
                val data = result.scanRecord?.getManufacturerSpecificData(0x004C) ?: return
                if (data.size < 18 || data[0] != 0x02.toByte() || data[1] != 0x15.toByte()) return

                // 2. EXTRAER EL UUID CORRECTAMENTE: Los 16 bytes del UUID están en posiciones 2-17 del frame.
                //    El método UUID(mostSigBits, leastSigBits) espera:
                //    - mostSigBits = bytes 0-7 del UUID (128 bits superiores)
                //    - leastSigBits = bytes 8-15 del UUID (128 bits inferiores)
                //    ByteBuffer.position() INCREMENTA automáticamente al leer, así que:
                //    - primer long: bytes 2-9 del data (UUID bytes 0-7)
                //    - segundo long: bytes 10-17 del data (UUID bytes 8-15)
                val bb = ByteBuffer.wrap(data, 2, 16)
                val uuid = UUID(bb.long, bb.long).toString().uppercase()
                synchronized(acumulado) {
                    acumulado.getOrPut(uuid) { mutableListOf() }.add(result.rssi)
                }
                Log.d(TAG, "iBeacon detectado: $uuid RSSI=${result.rssi}")
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Fallo de escaneo BLE, código=$errorCode")
            }
        }

        // Configuración del escáner:
        // - SCAN_MODE_LOW_LATENCY: escaneo continuo, rápido pero consume más batería
        // - CALLBACK_TYPE_ALL_MATCHES: devuelve todas las coincidencias
        // - MATCH_MODE_AGGRESSIVE: no filtra por cercanía
        // - REPORT_DELAY=0: devuelve resultados inmediatamente
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)
            .build()

        Log.d(TAG, "Iniciando escaneo BLE por ${ventanaMs}ms (${autorizados.size} beacons autorizados)")
        try {
            scanner.startScan(null, settings, callback)
        } catch (e: Exception) {
            // Bluetooth pudo apagarse o el permiso revocarse entre la validación y el inicio.
            Log.e(TAG, "No se pudo iniciar el escaneo BLE", e)
            return emptyList()
        }
        try {
            delay(ventanaMs)
        } finally {
            runCatching { scanner.stopScan(callback) }
        }

        // Calcular RSSI promedio y número de muestras por beacon
        return synchronized(acumulado) {
            acumulado.map { (uuid, rssis) ->
                LecturaBle(uuid, rssis.average().toInt(), rssis.size)
            }
        }.also { lecturas ->
            if (lecturas.isEmpty()) {
                Log.w(TAG, "Escaneo BLE sin resultados")
            } else {
                Log.d(TAG, "Escaneo BLE: ${lecturas.joinToString { "${it.uuid} ${it.rssiPromedio}dBm" }}")
            }
        }
    }

    private companion object {
        const val TAG = "SmartVoltBLE"
    }
}

/**
 * Proveedor del estado del sistema.
 *
 * Información útil para detección de fraude:
 * - modoDesarrollador: si está activo, podría indicar uso de herramientas de depuración
 * - horaAutomatica: si está desactivada, el usuario pudo manipular la hora del sistema
 */
class SystemStateProviderImpl(private val context: Context) : SystemStateProvider {
    override fun estado(): EstadoSistema {
        val cr = context.contentResolver
        val dev = Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        val auto = Settings.Global.getInt(cr, Settings.Global.AUTO_TIME, 1) == 1
        return EstadoSistema(modoDesarrollador = dev, horaAutomatica = auto)
    }
}