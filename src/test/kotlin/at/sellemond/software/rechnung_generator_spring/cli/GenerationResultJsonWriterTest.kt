package at.sellemond.software.rechnung_generator_spring.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GenerationResultJsonWriterTest {

    private val writer = GenerationResultJsonWriter()

    @Test
    fun `serializes result with preserved decimal scale and escaping`() {
        val result = GenerationResult(
            invoiceNumber = "Rechnung nook-2026/05",
            pdfPath = "/abs/rechnung.pdf",
            customer = "nook",
            issuer = "sellemond",
            period = "2026-05",
            hours = BigDecimal("142.50"),
            net = BigDecimal("14250.00"),
            vat = BigDecimal("2850.00"),
            gross = BigDecimal("17100.00"),
            attachments = listOf("/abs/rechnung.pdf", "/abs/report.pdf"),
            warnings = listOf("said \"oops\""),
        )

        val json = writer.toJson(result)

        assertThat(json).startsWith("{").endsWith("}")
        assertThat(json.lines()).hasSize(1)
        assertThat(json).contains("\"invoiceNumber\":\"Rechnung nook-2026/05\"")
        assertThat(json).contains("\"period\":\"2026-05\"")
        assertThat(json).contains("\"hours\":142.50")
        assertThat(json).contains("\"net\":14250.00")
        assertThat(json).contains("\"vat\":2850.00")
        assertThat(json).contains("\"gross\":17100.00")
        assertThat(json).contains("\"attachments\":[\"/abs/rechnung.pdf\",\"/abs/report.pdf\"]")
        assertThat(json).contains("\\\"oops\\\"")
    }

    @Test
    fun `serializes error envelope`() {
        val json = writer.toJson(GenerationError("HARVEST_FETCH_FAILED", "boom"))
        assertThat(json).isEqualTo("{\"error\":{\"code\":\"HARVEST_FETCH_FAILED\",\"message\":\"boom\"}}")
    }
}
