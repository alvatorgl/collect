package org.odk.collect.android.pdf

/**
 * Turns captured answers into the ordered blocks drawn in the PDF.
 * Irrelevant questions never arrive here. Notes and other questions without
 * a value are dropped, and group headings are emitted only when a following
 * question in that group is included.
 */
object PdfReportBuilder {

    const val PDF_REPORT_FIELD = "pdf_report"

    fun build(
        formTitle: String,
        formVersion: String?,
        instanceName: String,
        finalizedAt: String,
        deviceId: String?,
        userId: String?,
        answers: List<CapturedAnswer>
    ): PdfReport {
        val blocks = mutableListOf<PdfBlock>()
        var previousPath = emptyList<GroupHeading>()

        answers.forEach { answer ->
            val rendered = render(answer) ?: return@forEach
            val path = answer.groupPath
            val shared = previousPath.zip(path).takeWhile { (left, right) -> left == right }.size
            for (index in shared until path.size) {
                val heading = path[index]
                val title = if (heading.repeatNumber != null) {
                    "${heading.label} (${heading.repeatNumber})"
                } else {
                    heading.label
                }
                blocks += PdfBlock.Heading(title, level = index)
            }
            previousPath = path
            blocks += PdfBlock.Question(answer.label.ifBlank { answer.name }, rendered)
        }

        return PdfReport(
            formTitle = formTitle.ifBlank { instanceName },
            formVersion = formVersion?.takeIf { it.isNotBlank() },
            instanceName = instanceName,
            finalizedAt = finalizedAt,
            deviceId = deviceId?.takeIf { it.isNotBlank() },
            userId = userId?.takeIf { it.isNotBlank() },
            blocks = blocks
        )
    }

    private fun render(answer: CapturedAnswer): PdfAnswer? {
        if (answer.name == PDF_REPORT_FIELD) {
            return null
        }
        if (answer.isNote && answer.text.isBlank() && answer.imagePath == null) {
            return null
        }
        return when (answer.kind) {
            CapturedAnswerKind.IMAGE -> {
                val path = answer.imagePath
                if (path.isNullOrBlank()) {
                    answer.text.takeIf { it.isNotBlank() }?.let { PdfAnswer.Text(it) }
                } else {
                    PdfAnswer.Image(path, answer.text.ifBlank { path.substringAfterLast('/') })
                }
            }
            CapturedAnswerKind.LOCATION -> parseLocation(answer.text)
            CapturedAnswerKind.TEXT -> answer.text.takeIf { it.isNotBlank() }?.let { PdfAnswer.Text(it) }
        }
    }

    fun parseLocation(raw: String): PdfAnswer? {
        val parts = raw.trim().split(Regex("\\s+"))
        if (parts.size < 2) {
            return raw.takeIf { it.isNotBlank() }?.let { PdfAnswer.Text(it) }
        }
        val latitude = parts[0].toDoubleOrNull() ?: return PdfAnswer.Text(raw)
        val longitude = parts[1].toDoubleOrNull() ?: return PdfAnswer.Text(raw)
        return PdfAnswer.Location(
            latitude = latitude,
            longitude = longitude,
            altitude = parts.getOrNull(2)?.toDoubleOrNull(),
            accuracy = parts.getOrNull(3)?.toDoubleOrNull()
        )
    }
}
