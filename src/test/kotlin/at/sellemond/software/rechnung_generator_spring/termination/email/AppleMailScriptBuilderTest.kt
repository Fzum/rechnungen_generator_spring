package at.sellemond.software.rechnung_generator_spring.termination.email

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

class AppleMailScriptBuilderTest {

    @Test
    fun `builds a visible draft with recipient, subject, body and attachments and never sends`() {
        val script = AppleMailScriptBuilder.build(
            recipient = "kunde@example.com",
            subject = "Rechnung 5/2026",
            body = "Sehr geehrte Damen,\nDank \"X\".",
            attachments = listOf(Path.of("/abs/invoice.pdf"), Path.of("/abs/report.pdf")),
        )

        assertThat(script).contains("make new outgoing message")
        assertThat(script).contains("visible:true")
        assertThat(script).contains("address:\"kunde@example.com\"")
        assertThat(script).contains("subject:\"Rechnung 5/2026\"")
        assertThat(script).contains("POSIX file \"/abs/invoice.pdf\"")
        assertThat(script).contains("POSIX file \"/abs/report.pdf\"")
        assertThat(script).contains("\\\"X\\\"") // body quote escaped
        assertThat(script).doesNotContainIgnoringCase("send ")
    }

    @Test
    fun `escapes backslashes and quotes in subject`() {
        val script = AppleMailScriptBuilder.build(
            recipient = "a@b.c",
            subject = """a"b\c""",
            body = "x",
            attachments = listOf(Path.of("/x.pdf")),
        )
        assertThat(script).contains("""subject:"a\"b\\c"""")
    }
}
