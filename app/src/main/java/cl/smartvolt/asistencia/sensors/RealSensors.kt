package cl.smartvolt.asistencia.sensors

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.provider.Settings
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
 * Ubicación real con dos motores:
 *
 * 1. **Fused Location Provider** (Google Play Services) cuando está disponible y sano.
 * 2. **LocationManager del sistema** como respaldo.
 *
 * El respaldo cubre emuladores AOSP y equipos sin Google Play Services válidos,
 * donde Fused falla con el código 17 (`SERVICE_INVALID`). Así la app no depende
 * de GMS para poder marcar asistencia.
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

/** Lee balizas iBeacon (fabricante Apple 0x004C, prefijo 0x02 0x15) y promedia el RSSI por UUID. */
class BleScannerImpl(private val context: Context) : BleScanner {
    @SuppressLint("MissingPermission")
    override suspend fun escanear(autorizados: List<BeaconAutorizado>, ventanaMs: Long): List<LecturaBle> {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: return emptyList()
        val acumulado = mutableMapOf<String, MutableList<Int>>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord?.getManufacturerSpecificData(0x004C) ?: return
                if (data.size < 18 || data[0] != 0x02.toByte() || data[1] != 0x15.toByte()) return
                val bb = ByteBuffer.wrap(data, 2, 16)
                val uuid = UUID(bb.long, bb.long).toString().uppercase()
                synchronized(acumulado) { acumulado.getOrPut(uuid) { mutableListOf() }.add(result.rssi) }
            }
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(null, settings, callback)
        try { delay(ventanaMs) } finally { scanner.stopScan(callback) }
        return synchronized(acumulado) {
            acumulado.map { (uuid, rssis) -> LecturaBle(uuid, rssis.average().toInt(), rssis.size) }
        }
    }
}

class SystemStateProviderImpl(private val context: Context) : SystemStateProvider {
    override fun estado(): EstadoSistema {
        val cr = context.contentResolver
        val dev = Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        val auto = Settings.Global.getInt(cr, Settings.Global.AUTO_TIME, 1) == 1
        return EstadoSistema(modoDesarrollador = dev, horaAutomatica = auto)
    }
}
