package cl.smartvolt.asistencia.sensors

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.provider.Settings
import cl.smartvolt.asistencia.domain.model.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.util.UUID

class FusedLocationProviderImpl(private val context: Context) : LocationProvider {
    @SuppressLint("MissingPermission") // El permiso se solicita en la UI antes de marcar.
    override suspend fun obtener(referencia: LatLon): UbicacionCapturada? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val token = CancellationTokenSource()
        val loc = withTimeoutOrNull(8_000) {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
        }
        if (loc == null) {
            token.cancel()
            return null
        }
        val mock = if (Build.VERSION.SDK_INT >= 31) loc.isMock else @Suppress("DEPRECATION") loc.isFromMockProvider
        return UbicacionCapturada(loc.latitude, loc.longitude, loc.accuracy, mock)
    }
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
