package org.odk.collect.android.pdf

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class PdfReportBuilderTest {

    @Test
    fun `includes labels and text answers in order`() {
        val report = report(
            answer("name", "Name", "Ada"),
            answer("city", "City", "Bogotá")
        )

        assertThat(report.blocks.size, equalTo(2))
        val first = report.blocks[0] as PdfBlock.Question
        val second = report.blocks[1] as PdfBlock.Question
        assertThat(first.label, equalTo("Name"))
        assertThat((first.answer as PdfAnswer.Text).value, equalTo("Ada"))
        assertThat(second.label, equalTo("City"))
        assertThat((second.answer as PdfAnswer.Text).value, equalTo("Bogotá"))
    }

    @Test
    fun `skips blank answers and notes without values`() {
        val report = report(
            answer("empty", "Empty", ""),
            answer("note", "Read this", "", isNote = true),
            answer("kept", "Kept", "yes")
        )

        assertThat(report.blocks.size, equalTo(1))
        assertThat((report.blocks[0] as PdfBlock.Question).label, equalTo("Kept"))
    }

    @Test
    fun `uses select labels that were already resolved`() {
        val report = report(answer("color", "Color", "Azul, Violeta"))

        val question = report.blocks.single() as PdfBlock.Question
        assertThat((question.answer as PdfAnswer.Text).value, equalTo("Azul, Violeta"))
    }

    @Test
    fun `emits group and repeat headings only when they contain an answer`() {
        val household = GroupHeading("Household", null)
        val member = GroupHeading("Member", 2)
        val report = report(
            answer(
                name = "age",
                label = "Age",
                text = "4",
                groupPath = listOf(household, member)
            )
        )

        assertThat(report.blocks.size, equalTo(3))
        assertThat((report.blocks[0] as PdfBlock.Heading).title, equalTo("Household"))
        assertThat((report.blocks[0] as PdfBlock.Heading).level, equalTo(0))
        assertThat((report.blocks[1] as PdfBlock.Heading).title, equalTo("Member (2)"))
        assertThat((report.blocks[2] as PdfBlock.Question).label, equalTo("Age"))
    }

    @Test
    fun `does not repeat a heading for the next question in the same group`() {
        val group = GroupHeading("Location", null)
        val report = report(
            answer("lat", "Latitude", "4.6", groupPath = listOf(group)),
            answer("lon", "Longitude", "-74.0", groupPath = listOf(group))
        )

        assertThat(report.blocks.filterIsInstance<PdfBlock.Heading>().size, equalTo(1))
        assertThat(report.blocks.filterIsInstance<PdfBlock.Question>().size, equalTo(2))
    }

    @Test
    fun `parses GPS into latitude longitude altitude and accuracy`() {
        val report = report(
            CapturedAnswer(
                name = "where",
                label = "Where",
                groupPath = emptyList(),
                isNote = false,
                kind = CapturedAnswerKind.LOCATION,
                text = "4.711  -74.072  2600  5.5"
            )
        )

        val location = (report.blocks.single() as PdfBlock.Question).answer as PdfAnswer.Location
        assertThat(location.latitude, equalTo(4.711))
        assertThat(location.longitude, equalTo(-74.072))
        assertThat(location.altitude, equalTo(2600.0))
        assertThat(location.accuracy, equalTo(5.5))
    }

    @Test
    fun `keeps unparseable coordinates as text`() {
        val parsed = PdfReportBuilder.parseLocation("not a point")
        assertThat(parsed, instanceOf(PdfAnswer.Text::class.java))
    }

    @Test
    fun `embeds an image path and skips the pdf_report field`() {
        val report = report(
            CapturedAnswer(
                name = "photo",
                label = "Photo",
                groupPath = emptyList(),
                isNote = false,
                kind = CapturedAnswerKind.IMAGE,
                text = "photo.jpg",
                imagePath = "/instances/demo/photo.jpg"
            ),
            answer(PdfReportBuilder.PDF_REPORT_FIELD, "PDF", "demo.pdf")
        )

        assertThat(report.blocks.size, equalTo(1))
        val image = (report.blocks.single() as PdfBlock.Question).answer as PdfAnswer.Image
        assertThat(image.absolutePath, equalTo("/instances/demo/photo.jpg"))
        assertThat(image.caption, equalTo("photo.jpg"))
    }

    @Test
    fun `blank device and user identifiers are omitted`() {
        val report = PdfReportBuilder.build(
            formTitle = "Census",
            formVersion = " ",
            instanceName = "Ada",
            finalizedAt = "30 Oct 2026",
            deviceId = "  ",
            userId = null,
            answers = listOf(answer("q", "Q", "a"))
        )

        assertThat(report.formVersion, nullValue())
        assertThat(report.deviceId, nullValue())
        assertThat(report.userId, nullValue())
        assertThat(report.formTitle, equalTo("Census"))
    }

    private fun report(vararg answers: CapturedAnswer) = PdfReportBuilder.build(
        formTitle = "Census",
        formVersion = "3",
        instanceName = "Ada",
        finalizedAt = "30 Oct 2026, 11:00",
        deviceId = "device-1",
        userId = "julian",
        answers = answers.toList()
    )

    private fun answer(
        name: String,
        label: String,
        text: String,
        isNote: Boolean = false,
        groupPath: List<GroupHeading> = emptyList()
    ) = CapturedAnswer(
        name = name,
        label = label,
        groupPath = groupPath,
        isNote = isNote,
        kind = CapturedAnswerKind.TEXT,
        text = text
    )
}
