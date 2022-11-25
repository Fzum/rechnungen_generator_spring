package at.sellemond.software.rechnung_generator_spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import at.sellemond.software.rechnung_generator_spring.prompt.LeistungszeitraumPrompt
import at.sellemond.software.rechnung_generator_spring.prompt.StundenleistungPrompt
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenUserPrompt

@SpringBootApplication
class RechnungGeneratorSpringApplication

fun main(args: Array<String>) {
	runApplication<RechnungGeneratorSpringApplication>(*args)
	PdfCreator.createRechnung(
		UnternehmenUserPrompt().promptUser(),
		LeistungszeitraumPrompt().promptUser(),
		StundenleistungPrompt().prompt()
	)
}
