package org.odk.collect.android.pdf

import java.io.File

/**
 * The report sits next to the instance XML: `My_Form_2026-10-30.xml` ->
 * `My_Form_2026-10-30.pdf`.
 *
 * ODK Central rejects a submission that contains a file the form does not
 * declare. Collect otherwise uploads every file in the instance directory, so
 * the report is included only when the submission XML names it. That happens
 * when the form has an optional question named [PdfReportBuilder.PDF_REPORT_FIELD].
 */
object PdfReportFiles {

    @JvmStatic
    fun reportFile(instanceXml: File): File {
        val baseName = instanceXml.name.removeSuffix(".xml")
        return File(instanceXml.parentFile, "$baseName.pdf")
    }

    fun isReportForInstance(file: File, instanceXml: File): Boolean {
        val report = reportFile(instanceXml)
        return file.name == report.name && file.parentFile == report.parentFile
    }

    /**
     * @param submissionFile the XML that will be posted. Encrypted submissions
     * are not plain XML and must not carry the plaintext report.
     */
    fun submissionReferencesReport(submissionFile: File, reportName: String): Boolean {
        if (!submissionFile.isFile || reportName.isBlank()) {
            return false
        }
        if (submissionFile.length() > MAX_XML_BYTES) {
            return false
        }
        val header = ByteArray(64)
        val read = submissionFile.inputStream().use { it.read(header) }
        if (read <= 0) {
            return false
        }
        val start = String(header, 0, read, Charsets.UTF_8).trimStart()
        if (!start.startsWith("<")) {
            return false
        }
        return submissionFile.readText().contains(reportName)
    }

    private const val MAX_XML_BYTES = 8L * 1024L * 1024L
}
