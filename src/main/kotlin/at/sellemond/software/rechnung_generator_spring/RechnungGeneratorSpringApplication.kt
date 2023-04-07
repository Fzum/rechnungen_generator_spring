package at.sellemond.software.rechnung_generator_spring

import UnternehmenPropertiesParser
import at.sellemond.software.rechnung_generator_spring.termination.email.EmailInitiator
import at.sellemond.software.rechnung_generator_spring.prompt.LeistungszeitraumPrompt
import at.sellemond.software.rechnung_generator_spring.prompt.StundenleistungPrompt
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenUserPrompt
import at.sellemond.software.rechnung_generator_spring.termination.pathopener.PathOpener
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
class RechnungGeneratorCommandLineRunner(
    private val emailInitiator: EmailInitiator,
    private val pathOpener: PathOpener,
    private val pdfCreator: PdfCreator
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val unternehmenPromptResult = UnternehmenUserPrompt().promptUser()
        val leistungszeitraumPromptResult = LeistungszeitraumPrompt().promptUser()

        val pdfFileName = pdfCreator.createRechnung(
            unternehmenPromptResult,
            leistungszeitraumPromptResult,
            StundenleistungPrompt().prompt()
        )

        val zeitraum = leistungszeitraumPromptResult.zeitraum
        val kunde = UnternehmenPropertiesParser.parse(unternehmenPromptResult.kundenProperties)

        emailInitiator.initiateEmail(kunde, zeitraum)

        pathOpener.openRechnungFile(pdfFileName);
        pathOpener.openRechnungFolder(pdfFileName);
    }
}

