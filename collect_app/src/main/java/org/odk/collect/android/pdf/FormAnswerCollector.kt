package org.odk.collect.android.pdf

import org.javarosa.core.model.Constants
import org.javarosa.core.model.FormIndex
import org.javarosa.core.model.data.SelectMultiData
import org.javarosa.core.model.data.SelectOneData
import org.javarosa.core.model.data.helper.Selection
import org.javarosa.form.api.FormEntryController
import org.javarosa.form.api.FormEntryPrompt
import org.odk.collect.android.javarosawrapper.FormController
import org.odk.collect.android.javarosawrapper.JavaRosaFormController
import java.io.File

/**
 * Walks a filled form and records relevant questions in screen order.
 * The original form index is restored afterwards.
 */
object FormAnswerCollector {

    fun collect(formController: FormController, instanceDirectory: File): List<CapturedAnswer> {
        val savedIndex = formController.getFormIndex()
        val answers = mutableListOf<CapturedAnswer>()
        val groups = ArrayDeque<Pair<Int, GroupHeading>>()
        try {
            formController.jumpToIndex(FormIndex.createBeginningOfFormIndex())
            var event = formController.stepToNextEvent(JavaRosaFormController.STEP_INTO_GROUP)
            var guard = 0
            while (event != FormEntryController.EVENT_END_OF_FORM && guard++ < MAX_EVENTS) {
                when (event) {
                    FormEntryController.EVENT_GROUP -> pushGroup(formController, groups, repeat = false)
                    FormEntryController.EVENT_REPEAT -> pushGroup(formController, groups, repeat = true)
                    FormEntryController.EVENT_QUESTION -> {
                        trimGroupsTo(groups, depth(formController))
                        val prompts = formController.getQuestionPrompts(formController.getFormIndex())
                        val path = groups.map { it.second }
                        prompts.forEach { prompt ->
                            answers += capture(prompt, path, instanceDirectory)
                        }
                    }
                }
                event = formController.stepToNextEvent(JavaRosaFormController.STEP_INTO_GROUP)
            }
        } finally {
            if (savedIndex != null) {
                formController.jumpToIndex(savedIndex)
            }
        }
        return answers
    }

    fun findPrompt(formController: FormController, questionName: String): FormEntryPrompt? {
        val savedIndex = formController.getFormIndex()
        try {
            formController.jumpToIndex(FormIndex.createBeginningOfFormIndex())
            var event = formController.stepToNextEvent(JavaRosaFormController.STEP_INTO_GROUP)
            var guard = 0
            while (event != FormEntryController.EVENT_END_OF_FORM && guard++ < MAX_EVENTS) {
                if (event == FormEntryController.EVENT_QUESTION) {
                    val match = formController.getQuestionPrompts(formController.getFormIndex())
                        .firstOrNull { questionName(it) == questionName }
                    if (match != null) {
                        return match
                    }
                }
                event = formController.stepToNextEvent(JavaRosaFormController.STEP_INTO_GROUP)
            }
            return null
        } finally {
            if (savedIndex != null) {
                formController.jumpToIndex(savedIndex)
            }
        }
    }

    private fun pushGroup(formController: FormController, groups: ArrayDeque<Pair<Int, GroupHeading>>, repeat: Boolean) {
        val depth = depth(formController)
        trimGroupsTo(groups, depth)
        val caption = formController.getCaptionPrompt() ?: return
        val label = caption.longText?.takeIf { it.isNotBlank() }
            ?: caption.shortText?.takeIf { it.isNotBlank() }
            ?: return
        val repeatNumber = if (repeat) caption.multiplicity + 1 else null
        groups.addLast(depth to GroupHeading(label, repeatNumber))
    }

    private fun trimGroupsTo(groups: ArrayDeque<Pair<Int, GroupHeading>>, depth: Int) {
        while (groups.isNotEmpty() && groups.last().first >= depth) {
            groups.removeLast()
        }
    }

    private fun depth(formController: FormController): Int {
        return formController.getFormIndex()?.reference?.size() ?: 0
    }

    private fun capture(prompt: FormEntryPrompt, path: List<GroupHeading>, instanceDirectory: File): CapturedAnswer {
        val name = questionName(prompt)
        val label = prompt.longText?.takeIf { it.isNotBlank() }
            ?: prompt.shortText?.takeIf { it.isNotBlank() }
            ?: name
        val dataType = prompt.dataType
        val isNote = dataType == Constants.DATATYPE_NULL
        val appearance = prompt.appearanceHint.orEmpty()
        val rawText = prompt.answerText?.trim().orEmpty()

        val kind = when {
            dataType == Constants.DATATYPE_GEOPOINT -> CapturedAnswerKind.LOCATION
            isImage(dataType, appearance, rawText) -> CapturedAnswerKind.IMAGE
            else -> CapturedAnswerKind.TEXT
        }

        val text = when (prompt.controlType) {
            Constants.CONTROL_SELECT_ONE, Constants.CONTROL_SELECT_MULTI -> selectLabels(prompt)
            else -> rawText
        }

        val imagePath = if (kind == CapturedAnswerKind.IMAGE && rawText.isNotBlank()) {
            File(instanceDirectory, rawText).absolutePath
        } else {
            null
        }

        return CapturedAnswer(
            name = name,
            label = label,
            groupPath = path,
            isNote = isNote,
            kind = kind,
            text = text,
            imagePath = imagePath
        )
    }

    private fun isImage(dataType: Int, appearance: String, rawText: String): Boolean {
        val hint = appearance.lowercase()
        if (hint.contains("image") || hint.contains("draw") || hint.contains("signature") || hint.contains("annotate")) {
            return true
        }
        if (dataType == Constants.DATATYPE_BINARY) {
            val extension = rawText.substringAfterLast('.', "").lowercase()
            return extension in IMAGE_EXTENSIONS
        }
        return false
    }

    private fun selectLabels(prompt: FormEntryPrompt): String {
        val choices = prompt.selectChoices ?: return prompt.answerText?.trim().orEmpty()
        val values = when (val answer = prompt.answerValue) {
            is SelectOneData -> listOfNotNull(answer.value?.toString())
            is SelectMultiData -> selections(answer)
            else -> return prompt.answerText?.trim().orEmpty()
        }
        if (values.isEmpty()) {
            return ""
        }
        return values.joinToString(", ") { value ->
            val choice = choices.firstOrNull { it.value == value }
            if (choice != null) prompt.getSelectChoiceText(choice) else value
        }
    }

    private fun questionName(prompt: FormEntryPrompt): String {
        return prompt.index?.reference?.nameLast ?: ""
    }

    private fun selections(answer: SelectMultiData): List<String> {
        val value = answer.value as? List<*> ?: return emptyList()
        return value.map { item ->
            when (item) {
                is Selection -> item.value
                else -> item.toString()
            }
        }
    }

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")
    private const val MAX_EVENTS = 10000
}
