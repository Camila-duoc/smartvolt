package cl.smartvolt.asistencia.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.usecase.RegistrarMarcaUseCase
import cl.smartvolt.asistencia.modelo.EstadoMarca
import cl.smartvolt.asistencia.modelo.TipoMarca
import cl.smartvolt.asistencia.modelo.Veredicto
import cl.smartvolt.asistencia.notifications.Notificaciones
import cl.smartvolt.asistencia.sync.SyncScheduler
import cl.smartvolt.asistencia.ui.comun.sesion.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckInUi(val cargando: Boolean = false, val veredicto: Veredicto? = null, val error: String? = null)

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val registrar: RegistrarMarcaUseCase,
    repo: AttendanceRepository,
    private val session: SessionState,
    @ApplicationContext private val ctx: Context,
) : ViewModel() {
    private val _ui = MutableStateFlow(CheckInUi())
    val ui: StateFlow<CheckInUi> = _ui
    val pendientes = repo.contarPendientes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val usuarioId get() = session.usuario.value?.id.orEmpty()

    fun marcar(tipo: TipoMarca) {
        val u = session.usuario.value ?: return
        viewModelScope.launch {
            _ui.value = CheckInUi(cargando = true)
            runCatching { registrar(u.id, u.faenaId, tipo) }
                .onSuccess {
                    _ui.value = CheckInUi(veredicto = it)
                    if (it.estado == EstadoMarca.SOSPECHOSA) {
                        Notificaciones.marcaSospechosa(ctx, u.id, it.motivo)
                    }
                    SyncScheduler.enqueue(ctx)
                }
                .onFailure {
                    // Mensaje entendible para el usuario final; el detalle técnico
                    // (por ejemplo el código 17 de GMS) queda en los logs.
                    val mensaje = when (it) {
                        is SecurityException -> "Permiso de ubicación denegado. Actívalo en Ajustes."
                        else -> it.message ?: "Error inesperado"
                    }
                    _ui.value = CheckInUi(error = mensaje)
                }
        }
    }

    fun cerrarSesion() = session.cerrarSesion()
}
