package at.sellemond.software.rechnung_generator_spring

import PdfCreator
import at.sellemond.software.rechnung_generator_spring.prompt.LeistungszeitraumPrompt
import at.sellemond.software.rechnung_generator_spring.prompt.StundenleistungPrompt
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenUserPrompt
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RechnungGeneratorSpringApplication

fun main(args: Array<String>) {
    runApplication<RechnungGeneratorSpringApplication>(*args)
    PdfCreator.createRechnung(
        UnternehmenUserPrompt().promptUser(),
        LeistungszeitraumPrompt().promptUser(),
        StundenleistungPrompt().prompt()
    )

    openEmailClient()
}

private fun openEmailClient() {
    // only works on mac -> check OS
    val leistungszeitraum = "todo"
    val subject = "Rechnung $leistungszeitraum"
    val to = "verrechnung@sw-xperts.com" // load from props
    val body = """
        Sg. Damen und Herren,

        Anbei meine Rechnung und der Leistungsnachweis für $leistungszeitraum.
        Mit freundlichen Grüßen,
        Sellemond
    """.trimIndent()

    val command = "open mailto:$to?subject=$subject&body=$body"
    println(command)
    Runtime.getRuntime().exec(command)
}
