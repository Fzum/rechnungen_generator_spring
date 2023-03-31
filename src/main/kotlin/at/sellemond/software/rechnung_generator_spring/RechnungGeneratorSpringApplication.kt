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
    val command = "open mailto:test@test.com?subject=pdf&body=body"
    println(command)
    Runtime.getRuntime().exec(command)
}
