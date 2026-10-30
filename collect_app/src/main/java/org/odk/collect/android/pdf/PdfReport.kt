package org.odk.collect.android.pdf

/**
 * Offline summary of a finalized form instance. Built without Android so the
 * mapping from answers to this model can be unit tested.
 */
data class PdfReport(
    val formTitle: String,
    val formVersion: String?,
    val instanceName: String,
    val finalizedAt: String,
    val deviceId: String?,
    val userId: String?,
    val blocks: List<PdfBlock>
)

sealed class PdfBlock {
    data class Heading(val title: String, val level: Int) : PdfBlock()
    data class Question(val label: String, val answer: PdfAnswer) : PdfBlock()
}

sealed class PdfAnswer {
    data class Text(val value: String) : PdfAnswer()
    data class Image(val absolutePath: String, val caption: String) : PdfAnswer()
    data class Location(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double?,
        val accuracy: Double?
    ) : PdfAnswer()
}

data class GroupHeading(val label: String, val repeatNumber: Int?)

/**
 * One relevant, answered question as collected from the form, already using
 * choice labels rather than choice values.
 */
data class CapturedAnswer(
    val name: String,
    val label: String,
    val groupPath: List<GroupHeading>,
    val isNote: Boolean,
    val kind: CapturedAnswerKind,
    val text: String,
    val imagePath: String? = null
)

enum class CapturedAnswerKind {
    TEXT,
    IMAGE,
    LOCATION
}
