package org.odk.collect.android.pdf

import org.javarosa.core.model.data.StringData
import org.javarosa.core.services.PropertyManager
import org.odk.collect.android.application.Collect
import org.odk.collect.android.javarosawrapper.FormController
import org.odk.collect.settings.keys.ProjectKeys
import timber.log.Timber
import java.io.File
import java.text.DateFormat
import java.util.Date

/**
 * Writes `<instance>.pdf` beside the instance XML when a form is finalized.
 * Failures are logged and never abort finalization.
 *
 * If the form has an optional question named `pdf_report` (binary or text),
 * its answer is set to the PDF file name before the submission XML is written
 * so ODK Central receives the file as a declared attachment. Without that
 * question the PDF stays on the device only: Central rejects undeclared files,
 * and Collect would otherwise upload every file in the instance folder.
 */
object PdfReportGenerator {

    @JvmStatic
    @JvmOverloads
    fun generate(
        formController: FormController,
        attachToSubmission: Boolean = true,
        destination: File? = null
    ) {
        try {
            if (!isEnabled()) {
                return
            }
            val instanceFile = formController.getInstanceFile() ?: return
            val instanceDirectory = instanceFile.parentFile ?: return
            val reportFile = destination ?: PdfReportFiles.reportFile(instanceFile)
            val version = formController.getFormDef()
                ?.mainInstance
                ?.root
                ?.getAttributeValue(null, "version")
            val metadata = formController.getSubmissionMetadata()
            val answers = FormAnswerCollector.collect(formController, instanceDirectory)
            val report = PdfReportBuilder.build(
                formTitle = formController.getFormTitle().orEmpty(),
                formVersion = version,
                instanceName = metadata?.instanceName ?: instanceFile.nameWithoutExtension,
                finalizedAt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date()),
                deviceId = property("deviceid"),
                userId = property("username") ?: metadataUsername(),
                answers = answers
            )
            PdfReportRenderer.render(Collect.getInstance(), report, reportFile)
            if (attachToSubmission) {
                attach(formController, reportFile.name)
            }
        } catch (e: Exception) {
            Timber.e(e, "Survey Record PDF report was not created")
        }
    }

    private fun attach(formController: FormController, fileName: String) {
        val prompt = FormAnswerCollector.findPrompt(formController, PdfReportBuilder.PDF_REPORT_FIELD) ?: return
        val saved = formController.saveAnswer(prompt.index, StringData(fileName))
        if (!saved) {
            Timber.w("Could not store %s on question %s", fileName, PdfReportBuilder.PDF_REPORT_FIELD)
        }
    }

    private fun isEnabled(): Boolean {
        val settings = Collect.getInstance().component?.settingsProvider()?.getUnprotectedSettings()
            ?: return true
        return settings.getBoolean(ProjectKeys.KEY_GENERATE_PDF_ON_FINALIZE)
    }

    private fun metadataUsername(): String? {
        val settings = Collect.getInstance().component?.settingsProvider()?.getUnprotectedSettings()
            ?: return null
        return settings.getString(ProjectKeys.KEY_METADATA_USERNAME)
            ?: settings.getString(ProjectKeys.KEY_USERNAME)
    }

    private fun property(name: String): String? {
        return try {
            PropertyManager.`__`()?.getSingularProperty(name)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
