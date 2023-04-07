package at.sellemond.software.rechnung_generator_spring

import PdfCreator
import UnternehmenPropertiesParser
import at.sellemond.software.rechnung_generator_spring.email.EmailInitiator
import at.sellemond.software.rechnung_generator_spring.prompt.LeistungszeitraumPrompt
import at.sellemond.software.rechnung_generator_spring.prompt.StundenleistungPrompt
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenUserPrompt
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan
class RechnungGeneratorSpringApplication

fun main(args: Array<String>) {
    runApplication<RechnungGeneratorSpringApplication>(*args)
}

@Component
class MyCommandLineRunner(
    private val emailInitiator: EmailInitiator,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val unternehmenPromptResult = UnternehmenUserPrompt().promptUser()
        val leistungszeitraumPromptResult = LeistungszeitraumPrompt().promptUser()
        val zeitraum = leistungszeitraumPromptResult.zeitraum

        PdfCreator.createRechnung(
            unternehmenPromptResult,
            leistungszeitraumPromptResult,
            StundenleistungPrompt().prompt()
        )

        val kunde = UnternehmenPropertiesParser.parse(unternehmenPromptResult.kundenProperties)
        emailInitiator.initiateEmail(kunde, zeitraum)
    }
}

