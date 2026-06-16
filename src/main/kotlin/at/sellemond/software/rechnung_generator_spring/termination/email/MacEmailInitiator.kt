package at.sellemond.software.rechnung_generator_spring.termination.email

import at.sellemond.software.rechnung_generator_spring.util.replacePlaceholder
import at.sellemond.software.rechnung_generator_spring.util.replaceUmlauts
import at.sellemond.software.rechnung_generator_spring.util.sanitizeUrl
import model.Unternehmen
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Path

private const val LEISTUNGSZEITRAUM = "leistungszeitraum"

@Component
class MacEmailInitiator(private val emailProperties: EmailProperties) : EmailInitiator {

    override fun initiateEmail(kunde: Unternehmen, zeitraum: String, attachments: List<Path>) {
        if (attachments.isEmpty()) {
            // Preserve the original interactive behaviour: a plain mailto: draft (no attachments).
            openMailtoDraft(kunde, zeitraum)
            return
        }
        // Non-interactive path: an Apple Mail draft pre-loaded with attachments, never sent.
        openAppleMailDraft(kunde, zeitraum, attachments)
    }

    private fun openMailtoDraft(kunde: Unternehmen, zeitraum: String) {
        val subject = sanitizeUrl(replace(emailProperties.subject, zeitraum))
        val body = sanitizeUrl(replace(emailProperties.body, zeitraum))
        Runtime.getRuntime().exec("open mailto:${kunde.contact.email}?subject=$subject&body=$body")
    }

    private fun openAppleMailDraft(kunde: Unternehmen, zeitraum: String, attachments: List<Path>) {
        val subject = replaceUmlauts(replace(emailProperties.subject, zeitraum))
        val body = replaceUmlauts(replace(emailProperties.body, zeitraum))
        val script = AppleMailScriptBuilder.build(kunde.contact.email, subject, body, attachments)
        val scriptFile = File.createTempFile("rechnung-mail", ".scpt")
        scriptFile.writeText(script)
        try {
            // Wait for osascript to finish. In headless mode the JVM exits within ~1s, which would
            // otherwise delete the temp script (and orphan osascript) before the draft is created.
            val process = ProcessBuilder("osascript", scriptFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            check(exitCode == 0) { "osascript exited $exitCode: ${output.trim()}" }
        } finally {
            scriptFile.delete()
        }
    }

    private fun replace(text: String, zeitraum: String) = replacePlaceholder(text, LEISTUNGSZEITRAUM, zeitraum)
}
