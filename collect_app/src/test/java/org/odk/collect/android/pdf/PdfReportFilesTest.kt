package org.odk.collect.android.pdf

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PdfReportFilesTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    @Test
    fun `report sits next to the instance xml`() {
        val instance = File(folder.root, "Household_2026-10-30.xml")
        val report = PdfReportFiles.reportFile(instance)

        assertThat(report.parentFile, equalTo(instance.parentFile))
        assertThat(report.name, equalTo("Household_2026-10-30.pdf"))
    }

    @Test
    fun `plain submission that names the pdf is uploaded`() {
        val instance = folder.newFile("record.xml")
        val pdf = folder.newFile("record.pdf")
        instance.writeText("<data><pdf_report>record.pdf</pdf_report></data>")

        assertThat(PdfReportFiles.isReportForInstance(pdf, instance), equalTo(true))
        assertThat(PdfReportFiles.submissionReferencesReport(instance, pdf.name), equalTo(true))
    }

    @Test
    fun `plain submission that does not name the pdf stays local`() {
        val instance = folder.newFile("record.xml")
        val pdf = folder.newFile("record.pdf")
        instance.writeText("<data><name>Ada</name></data>")

        assertThat(PdfReportFiles.submissionReferencesReport(instance, pdf.name), equalTo(false))
    }

    @Test
    fun `encrypted submission is not treated as a reference`() {
        val instance = folder.newFile("record.xml")
        instance.writeBytes(byteArrayOf(0x01, 0x02, 0x03, 0x04))

        assertThat(PdfReportFiles.submissionReferencesReport(instance, "record.pdf"), equalTo(false))
    }
}
