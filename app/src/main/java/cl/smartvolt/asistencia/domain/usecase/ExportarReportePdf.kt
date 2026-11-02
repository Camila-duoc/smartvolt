package cl.smartvolt.asistencia.domain.usecase

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/** Genera un PDF de texto simple (A4, varias páginas) con el resumen administrativo. */
object ExportarReportePdf {
    private const val ANCHO = 595
    private const val ALTO = 842
    private const val MARGEN = 40f
    private const val INTERLINEADO = 18f

    fun escribir(titulo: String, lineas: List<String>, salida: OutputStream) {
        val doc = PdfDocument()
        val estiloTexto = Paint().apply { textSize = 11f }
        val estiloTitulo = Paint().apply { textSize = 16f; isFakeBoldText = true }

        var numero = 1
        var pagina = doc.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create())
        var y = MARGEN + 16f
        pagina.canvas.drawText(titulo, MARGEN, y, estiloTitulo)
        y += INTERLINEADO * 1.5f

        for (linea in lineas) {
            if (y > ALTO - MARGEN) {
                doc.finishPage(pagina)
                numero++
                pagina = doc.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create())
                y = MARGEN + 16f
            }
            pagina.canvas.drawText(linea, MARGEN, y, estiloTexto)
            y += INTERLINEADO
        }
        doc.finishPage(pagina)
        try {
            doc.writeTo(salida)
        } finally {
            doc.close()
        }
    }
}
