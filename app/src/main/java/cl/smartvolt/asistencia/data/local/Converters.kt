package cl.smartvolt.asistencia.data.local

import androidx.room.TypeConverter
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.domain.model.Rol
import cl.smartvolt.asistencia.domain.model.Severidad
import cl.smartvolt.asistencia.domain.model.SyncEstado
import cl.smartvolt.asistencia.domain.model.TipoMarca
import cl.smartvolt.asistencia.domain.model.TipoPerimetro

/**
 * Room no guarda enums por sí solo: se persisten como su [Enum.name] (TEXT),
 * que es justo el formato que usan las consultas de [MarcaDao] y [ConfigDao]
 * (`estado != 'RECHAZADA'`, `syncEstado = 'PENDIENTE'`, etc.).
 */
class Converters {
    @TypeConverter fun rolATexto(v: Rol): String = v.name
    @TypeConverter fun textoARol(v: String): Rol = enumValueOf(v)

    @TypeConverter fun tipoMarcaATexto(v: TipoMarca): String = v.name
    @TypeConverter fun textoATipoMarca(v: String): TipoMarca = enumValueOf(v)

    @TypeConverter fun estadoMarcaATexto(v: EstadoMarca): String = v.name
    @TypeConverter fun textoAEstadoMarca(v: String): EstadoMarca = enumValueOf(v)

    @TypeConverter fun syncEstadoATexto(v: SyncEstado): String = v.name
    @TypeConverter fun textoASyncEstado(v: String): SyncEstado = enumValueOf(v)

    @TypeConverter fun severidadATexto(v: Severidad): String = v.name
    @TypeConverter fun textoASeveridad(v: String): Severidad = enumValueOf(v)

    @TypeConverter fun tipoPerimetroATexto(v: TipoPerimetro): String = v.name
    @TypeConverter fun textoATipoPerimetro(v: String): TipoPerimetro = enumValueOf(v)
}
