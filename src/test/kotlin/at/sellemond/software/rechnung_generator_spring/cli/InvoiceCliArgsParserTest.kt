package at.sellemond.software.rechnung_generator_spring.cli

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments

class InvoiceCliArgsParserTest {

    private val parser = InvoiceCliArgsParser()

    private fun args(vararg a: String) = DefaultApplicationArguments(*a)

    @Test
    fun `parses a valid manual invocation`() {
        val result = parser.parse(
            args(
                "--issuer=test-unternehmen", "--customer=test-kunde",
                "--period=2026-05", "--hours=142.5", "--mail=false", "--format=json"
            )
        )

        assertThat(result.issuer).isEqualTo("test-unternehmen")
        assertThat(result.customer).isEqualTo("test-kunde")
        assertThat(result.year).isEqualTo(2026)
        assertThat(result.month).isEqualTo(5)
        assertThat(result.period).isEqualTo("2026-05")
        assertThat(result.hours).isEqualByComparingTo("142.5")
        assertThat(result.hoursSource).isEqualTo(HoursSource.MANUAL)
        assertThat(result.mail).isFalse()
        assertThat(result.open).isTrue()
        assertThat(result.format).isEqualTo(OutputFormat.JSON)
    }

    @Test
    fun `defaults format to text and open to true`() {
        val result = parser.parse(
            args("--issuer=test-unternehmen", "--customer=test-kunde", "--period=2026-05", "--hours=1", "--mail=false")
        )
        assertThat(result.format).isEqualTo(OutputFormat.TEXT)
        assertThat(result.open).isTrue()
    }

    @Test
    fun `rejects unknown issuer`() = expectCode(
        "UNKNOWN_ISSUER",
        "--issuer=nope", "--customer=test-kunde", "--period=2026-05", "--hours=1", "--mail=false"
    )

    @Test
    fun `rejects unknown customer`() = expectCode(
        "UNKNOWN_CUSTOMER",
        "--issuer=test-unternehmen", "--customer=nope", "--period=2026-05", "--hours=1", "--mail=false"
    )

    @Test
    fun `rejects future period`() = expectCode(
        "INVALID_PERIOD",
        "--issuer=test-unternehmen", "--customer=test-kunde", "--period=2099-12", "--hours=1", "--mail=false"
    )

    @Test
    fun `rejects malformed period`() = expectCode(
        "INVALID_PERIOD",
        "--issuer=test-unternehmen", "--customer=test-kunde", "--period=2026-13", "--hours=1", "--mail=false"
    )

    @Test
    fun `requires hours for manual source`() = expectCode(
        "MISSING_HOURS",
        "--issuer=test-unternehmen", "--customer=test-kunde", "--period=2026-05", "--mail=false"
    )

    @Test
    fun `requires report pdf when mail enabled`() = expectCode(
        "MISSING_REPORT_PDF",
        "--issuer=test-unternehmen", "--customer=test-kunde", "--period=2026-05", "--hours=1"
    )

    private fun expectCode(code: String, vararg a: String) {
        assertThatThrownBy { parser.parse(args(*a)) }
            .isInstanceOfSatisfying(CliValidationException::class.java) { assertThat(it.code).isEqualTo(code) }
    }
}
