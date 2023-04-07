package at.sellemond.software.rechnung_generator_spring.email

import at.sellemond.software.rechnung_generator_spring.util.replacePlaceholder
import at.sellemond.software.rechnung_generator_spring.util.sanitizeUrl
import model.Unternehmen
import org.springframework.stereotype.Component

private const val LEISTUNGSZEITRAUM = "leistungszeitraum"

@Component
class MacEmailInitiator(private val emailProperties: EmailProperties) : EmailInitiator {
    override fun initiateEmail(kunde: Unternehmen, zeitraum: String) {
        val (subject, body) = getSanitizedContent(zeitraum)
        val command = buildString {
            append("open mailto:${kunde.contact.email}")
            append("?subject=$subject")
            append("&body=$body")
        }

        Runtime.getRuntime().exec(command)
    }

    private fun getSanitizedContent(zeitraum: String): Pair<String, String> {
        val replace = { s: String -> replacePlaceholder(s, LEISTUNGSZEITRAUM, zeitraum) }
        val subject = replace(emailProperties.subject)
        val body = replace(emailProperties.body)
        return Pair(sanitizeUrl(subject), sanitizeUrl(body))
    }
}