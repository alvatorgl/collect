package org.odk.collect.android.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.odk.collect.android.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Draws a [PdfReport] with [PdfDocument]. No network and no print dialog.
 */
object PdfReportRenderer {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val FOOTER_TOP = PAGE_HEIGHT - 36f
    private const val CONTENT_BOTTOM = FOOTER_TOP - 12f
    private const val FOOTER = "Generado con Survey Record (basado en ODK Collect)"

    private const val INK = 0xFF040914.toInt()
    private const val BODY = 0xFF3A4A60.toInt()
    private const val PRIMARY = 0xFF23769C.toInt()
    private const val SECONDARY = 0xFF7465A2.toInt()
    private const val PAGE_BG = 0xFFFAFBFD.toInt()
    private const val MUTED = 0xFF7A899C.toInt()

    fun render(context: Context, report: PdfReport, destination: File) {
        destination.parentFile?.mkdirs()
        val document = PdfDocument()
        val unbounded = ResourcesCompat.getFont(context, R.font.unbounded_bold) ?: Typeface.DEFAULT_BOLD
        val inter = ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.SANS_SERIF
        val interMedium = ResourcesCompat.getFont(context, R.font.inter_medium) ?: inter
        val logo = logoBitmap(context)
        val state = PageState(document)
        try {
            startPage(state, logo, unbounded, firstPage = true)
            drawTitleBlock(state, report, unbounded, inter, interMedium)
            report.blocks.forEach { block ->
                when (block) {
                    is PdfBlock.Heading -> drawHeading(state, block, unbounded, interMedium, logo)
                    is PdfBlock.Question -> drawQuestion(state, block, inter, interMedium, logo)
                }
            }
            finishPage(state, inter)
            FileOutputStream(destination).use { document.writeTo(it) }
        } finally {
            document.close()
            logo?.recycle()
        }
    }

    private class PageState(val document: PdfDocument) {
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y: Float = MARGIN
        var number: Int = 0
    }

    private fun startPage(state: PageState, logo: Bitmap?, unbounded: Typeface, firstPage: Boolean) {
        state.page?.let { finishPageOnly(state) }
        state.number += 1
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, state.number).create()
        val page = state.document.startPage(pageInfo)
        state.page = page
        state.canvas = page.canvas
        state.canvas?.drawColor(PAGE_BG)
        if (firstPage) {
            drawHero(state, logo, unbounded)
        } else {
            drawContinuationBar(state, unbounded)
        }
    }

    private fun finishPage(state: PageState, inter: Typeface) {
        val canvas = state.canvas ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED
            textSize = 9f
            typeface = inter
        }
        canvas.drawText(FOOTER, MARGIN, PAGE_HEIGHT - 18f, paint)
        val pageLabel = state.number.toString()
        val width = paint.measureText(pageLabel)
        canvas.drawText(pageLabel, PAGE_WIDTH - MARGIN - width, PAGE_HEIGHT - 18f, paint)
        state.document.finishPage(state.page)
        state.page = null
        state.canvas = null
    }

    private fun finishPageOnly(state: PageState) {
        // Continuation pages still need a footer. The caller passes the body font via finishPage.
    }

    private fun ensureSpace(state: PageState, needed: Float, logo: Bitmap?, unbounded: Typeface, inter: Typeface) {
        if (state.y + needed <= CONTENT_BOTTOM) {
            return
        }
        finishPage(state, inter)
        startPage(state, logo, unbounded, firstPage = false)
    }

    private fun drawHero(state: PageState, logo: Bitmap?, unbounded: Typeface) {
        val canvas = state.canvas ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = PRIMARY
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 78f, paint)
        logo?.let {
            val dst = RectF(MARGIN, 16f, MARGIN + 46f, 62f)
            canvas.drawBitmap(it, null, dst, paint)
        }
        paint.color = Color.WHITE
        paint.typeface = unbounded
        paint.textSize = 16f
        canvas.drawText("SURVEY RECORD", MARGIN + 58f, 40f, paint)
        paint.textSize = 10f
        paint.color = 0xFFE7EEF8.toInt()
        canvas.drawText("2027", MARGIN + 58f, 56f, paint)
        state.y = 98f
    }

    private fun drawContinuationBar(state: PageState, unbounded: Typeface) {
        val canvas = state.canvas ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PRIMARY
            typeface = unbounded
            textSize = 11f
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 28f, Paint().apply { color = PRIMARY })
        paint.color = Color.WHITE
        canvas.drawText("SURVEY RECORD", MARGIN, 18f, paint)
        state.y = 48f
    }

    private fun drawTitleBlock(
        state: PageState,
        report: PdfReport,
        unbounded: Typeface,
        inter: Typeface,
        interMedium: Typeface
    ) {
        val canvas = state.canvas ?: return
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textSize = 18f
            typeface = unbounded
        }
        val wrapped = wrap(report.formTitle, title, PAGE_WIDTH - 2 * MARGIN)
        wrapped.forEach { line ->
            canvas.drawText(line, MARGIN, state.y, title)
            state.y += 22f
        }
        state.y += 4f
        val meta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BODY
            textSize = 11f
            typeface = inter
        }
        val strong = Paint(meta).apply { typeface = interMedium; color = INK }
        drawMeta(canvas, state, strong, meta, "Registro", report.instanceName)
        report.formVersion?.let { drawMeta(canvas, state, strong, meta, "Versión", it) }
        drawMeta(canvas, state, strong, meta, "Finalizado", report.finalizedAt)
        report.userId?.let { drawMeta(canvas, state, strong, meta, "Usuario", it) }
        report.deviceId?.let { drawMeta(canvas, state, strong, meta, "Dispositivo", it) }
        state.y += 8f
        canvas.drawRect(MARGIN, state.y, PAGE_WIDTH - MARGIN, state.y + 1.5f, Paint().apply { color = SECONDARY })
        state.y += 16f
    }

    private fun drawMeta(canvas: Canvas, state: PageState, strong: Paint, meta: Paint, label: String, value: String) {
        canvas.drawText(label, MARGIN, state.y, strong)
        canvas.drawText(value, MARGIN + 88f, state.y, meta)
        state.y += 16f
    }

    private fun drawHeading(
        state: PageState,
        heading: PdfBlock.Heading,
        unbounded: Typeface,
        interMedium: Typeface,
        logo: Bitmap?
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (heading.level == 0) SECONDARY else PRIMARY
            textSize = if (heading.level == 0) 13f else 12f
            typeface = if (heading.level == 0) unbounded else interMedium
        }
        ensureSpace(state, 28f, logo, unbounded, interMedium)
        state.y += 6f
        state.canvas?.drawText(heading.title, MARGIN, state.y, paint)
        state.y += 18f
    }

    private fun drawQuestion(
        state: PageState,
        question: PdfBlock.Question,
        inter: Typeface,
        interMedium: Typeface,
        logo: Bitmap?
    ) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textSize = 11f
            typeface = interMedium
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BODY
            textSize = 11f
            typeface = inter
        }
        val labelLines = wrap(question.label, labelPaint, PAGE_WIDTH - 2 * MARGIN)
        ensureSpace(state, 18f * labelLines.size + 20f, logo, interMedium, inter)
        labelLines.forEach { line ->
            state.canvas?.drawText(line, MARGIN, state.y, labelPaint)
            state.y += 15f
        }
        when (val answer = question.answer) {
            is PdfAnswer.Text -> drawWrapped(state, answer.value, bodyPaint, inter, interMedium, logo)
            is PdfAnswer.Location -> {
                val accuracy = answer.accuracy?.let { " · precisión ${formatNumber(it)} m" }.orEmpty()
                val altitude = answer.altitude?.let { " · alt ${formatNumber(it)} m" }.orEmpty()
                val line = "${formatNumber(answer.latitude)}, ${formatNumber(answer.longitude)}$altitude$accuracy"
                drawWrapped(state, line, bodyPaint, inter, interMedium, logo)
            }
            is PdfAnswer.Image -> drawImage(state, answer, bodyPaint, inter, interMedium, logo)
        }
        state.y += 8f
    }

    private fun drawWrapped(
        state: PageState,
        text: String,
        paint: Paint,
        inter: Typeface,
        interMedium: Typeface,
        logo: Bitmap?
    ) {
        wrap(text, paint, PAGE_WIDTH - 2 * MARGIN).forEach { line ->
            ensureSpace(state, 16f, logo, interMedium, inter)
            state.canvas?.drawText(line, MARGIN, state.y, paint)
            state.y += 15f
        }
    }

    private fun drawImage(
        state: PageState,
        answer: PdfAnswer.Image,
        bodyPaint: Paint,
        inter: Typeface,
        interMedium: Typeface,
        logo: Bitmap?
    ) {
        val bitmap = decodeThumbnail(answer.absolutePath)
        if (bitmap == null) {
            drawWrapped(state, answer.caption, bodyPaint, inter, interMedium, logo)
            return
        }
        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        val maxHeight = 180f
        val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height, 1f)
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        ensureSpace(state, height + 8f, logo, interMedium, inter)
        val dst = RectF(MARGIN, state.y, MARGIN + width, state.y + height)
        state.canvas?.drawBitmap(bitmap, null, dst, Paint(Paint.ANTI_ALIAS_FLAG))
        bitmap.recycle()
        state.y += height + 6f
    }

    private fun decodeThumbnail(path: String): Bitmap? {
        val file = File(path)
        if (!file.isFile) {
            return null
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }
        var sample = 1
        while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(path, options)
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) {
            return listOf("—")
        }
        val lines = mutableListOf<String>()
        text.split('\n').forEach { paragraph ->
            var current = ""
            paragraph.split(Regex("\\s+")).forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) {
                        lines += current
                    }
                    current = word
                }
            }
            if (current.isNotEmpty()) {
                lines += current
            }
        }
        return lines.ifEmpty { listOf("—") }
    }

    private fun formatNumber(value: Double): String {
        val rounded = (value * 100000.0).roundToInt() / 100000.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    private fun logoBitmap(context: Context): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_survey_record_mark) ?: return null
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }
}
