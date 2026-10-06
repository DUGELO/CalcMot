package br.com.calcmot.finance.ledger

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

interface ReportExporter {
    fun exportDailyPdf(snapshot: LedgerExportSnapshot, output: OutputStream)
    fun exportDailyXlsx(snapshot: LedgerExportSnapshot, output: OutputStream)
}

class LocalReportExporter : ReportExporter {
    override fun exportDailyPdf(snapshot: LedgerExportSnapshot, output: OutputStream) {
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f; isFakeBoldText = true }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
            val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = 0xff555555.toInt() }
            var y = 52f

            canvas.drawText("CalcMot - Relatorio diario", 42f, y, titlePaint)
            y += 26f
            canvas.drawText("Dados locais confirmados e informados pelo motorista", 42f, y, mutedPaint)
            y += 32f
            canvas.drawText("Ofertas analisadas: ${snapshot.offers.size}", 42f, y, bodyPaint)
            y += 18f
            canvas.drawText("Ganhos confirmados: ${money(snapshot.earnings.sumOf { it.amountCents })}", 42f, y, bodyPaint)
            y += 18f
            canvas.drawText("Custos informados: ${money(snapshot.expenses.sumOf { it.amountCents })}", 42f, y, bodyPaint)
            y += 30f

            canvas.drawText("Horario  App   Tarifa      R$/km    R$/h     Classificacao", 42f, y, bodyPaint)
            y += 16f
            snapshot.offers.take(32).forEach { offer ->
                val totalKm = (offer.pickupDistanceMeters + offer.tripDistanceMeters) / 1000.0
                val totalHours = (offer.pickupTimeSeconds + offer.tripTimeSeconds) / 3600.0
                val perKm = if (totalKm > 0) offer.fareCents / 100.0 / totalKm else 0.0
                val perHour = if (totalHours > 0) offer.fareCents / 100.0 / totalHours else 0.0
                val row = "%s  %-4s  %-10s  %-7.2f  %-7.2f  %s".format(
                    Locale.US,
                    clock(offer.observedAtMillis),
                    offer.platform,
                    money(offer.fareCents),
                    perKm,
                    perHour,
                    offer.classification
                )
                canvas.drawText(row, 42f, y, mutedPaint)
                y += 15f
                if (y > 790f) return@forEach
            }
            document.finishPage(page)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    override fun exportDailyXlsx(snapshot: LedgerExportSnapshot, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            zip.writeEntry("[Content_Types].xml", contentTypes)
            zip.writeEntry("_rels/.rels", rootRelationships)
            zip.writeEntry("xl/workbook.xml", workbook)
            zip.writeEntry("xl/_rels/workbook.xml.rels", workbookRelationships)
            zip.writeEntry("xl/styles.xml", styles)
            zip.writeEntry("xl/worksheets/sheet1.xml", worksheet(snapshot))
        }
    }

    private fun worksheet(snapshot: LedgerExportSnapshot): String {
        val rows = mutableListOf<List<String>>()
        rows += listOf("Horario", "Plataforma", "Tipo", "Valor", "R$/km", "R$/h", "Classificacao", "Confianca")
        snapshot.offers.forEach { offer ->
            val totalKm = (offer.pickupDistanceMeters + offer.tripDistanceMeters) / 1000.0
            val totalHours = (offer.pickupTimeSeconds + offer.tripTimeSeconds) / 3600.0
            rows += listOf(
                clock(offer.observedAtMillis), offer.platform, "Oferta", money(offer.fareCents),
                if (totalKm > 0) "%.2f".format(Locale.US, offer.fareCents / 100.0 / totalKm) else "",
                if (totalHours > 0) "%.2f".format(Locale.US, offer.fareCents / 100.0 / totalHours) else "",
                offer.classification, offer.confidence
            )
        }
        snapshot.earnings.forEach { earning ->
            rows += listOf(clock(earning.occurredAtMillis), earning.platform.orEmpty(), "Ganho", money(earning.amountCents), "", "", earning.kind, earning.confidence)
        }
        snapshot.expenses.forEach { expense ->
            rows += listOf(clock(expense.occurredAtMillis), "", "Custo", money(expense.amountCents), "", "", expense.category, expense.confidence)
        }

        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
            rows.forEachIndexed { rowIndex, cells ->
                append("<row r=\"").append(rowIndex + 1).append("\">")
                cells.forEachIndexed { columnIndex, value ->
                    val cell = "${columnName(columnIndex)}${rowIndex + 1}"
                    append("<c r=\"").append(cell).append("\" t=\"inlineStr\"><is><t>")
                    append(value.escapeXml())
                    append("</t></is></c>")
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, value: String) {
        putNextEntry(ZipEntry(name))
        write(value.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun columnName(index: Int): String {
        var value = index + 1
        val result = StringBuilder()
        while (value > 0) {
            value--
            result.insert(0, ('A'.code + value % 26).toChar())
            value /= 26
        }
        return result.toString()
    }

    private fun String.escapeXml(): String = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun money(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
        .format(cents / 100.0)
    private fun clock(millis: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))

    private val contentTypes = """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""
    private val rootRelationships = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
    private val workbook = """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Relatorio diario" sheetId="1" r:id="rId1"/></sheets></workbook>"""
    private val workbookRelationships = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""
    private val styles = """<?xml version="1.0" encoding="UTF-8"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="1"><font><sz val="11"/><name val="Arial"/></font></fonts><fills count="1"><fill><patternFill patternType="none"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs></styleSheet>"""
}

interface PremiumEntitlementProvider {
    fun hasPremiumAccess(): Boolean
}

object LockedPremiumEntitlementProvider : PremiumEntitlementProvider {
    override fun hasPremiumAccess(): Boolean = false
}
