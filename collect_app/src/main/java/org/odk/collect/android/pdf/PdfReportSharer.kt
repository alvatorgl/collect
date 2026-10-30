package org.odk.collect.android.pdf

import android.app.Activity
import android.content.Intent
import org.odk.collect.android.BuildConfig
import org.odk.collect.android.utilities.ContentUriProvider
import org.odk.collect.androidshared.ui.ToastUtils
import java.io.File

object PdfReportSharer {

    fun share(activity: Activity, instanceFile: File) {
        val pdf = if (instanceFile.extension.equals("pdf", ignoreCase = true)) {
            instanceFile
        } else {
            PdfReportFiles.reportFile(instanceFile)
        }
        if (!pdf.isFile) {
            ToastUtils.showLongToast(activity.getString(org.odk.collect.strings.R.string.pdf_report_missing))
            return
        }
        val uri = ContentUriProvider().getUriForFile(
            activity,
            BuildConfig.APPLICATION_ID + ".provider",
            pdf
        )
        if (uri == null) {
            ToastUtils.showLongToast(activity.getString(org.odk.collect.strings.R.string.pdf_report_missing))
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(
            Intent.createChooser(send, activity.getString(org.odk.collect.strings.R.string.share_pdf_report))
        )
    }
}
