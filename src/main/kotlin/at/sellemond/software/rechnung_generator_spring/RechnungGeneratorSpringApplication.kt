package at.sellemond.software.rechnung_generator_spring

import PdfCreator
import UnternehmenPropertiesParser
import at.sellemond.software.rechnung_generator_spring.email.EmailConfiguration
import at.sellemond.software.rechnung_generator_spring.prompt.LeistungszeitraumPrompt
import at.sellemond.software.rechnung_generator_spring.prompt.StundenleistungPrompt
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenUserPrompt
import at.sellemond.software.rechnung_generator_spring.util.sanitizeUrl
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@SpringBootApplication
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
        val kunde = UnternehmenPropertiesParser.parse(unternehmenPromptResult.kundenProperties)
        val unternehmen = UnternehmenPropertiesParser.parse(unternehmenPromptResult.unternehmenProperties)

        val leistungszeitraumPromptResult = LeistungszeitraumPrompt().promptUser()
        val zeitraum = leistungszeitraumPromptResult.zeitraum

        PdfCreator.createRechnung(
            unternehmenPromptResult,
            leistungszeitraumPromptResult,
            StundenleistungPrompt().prompt()
        )

        emailInitiator.initiateEmail()
    }
}

interface EmailInitiator {
    fun initiateEmail();
}

@Component
class MacEmailInitiator : EmailInitiator {
    override fun initiateEmail() {
        // todo: load from emailConfiguration
        val leistungszeitraum = "todo"
        val subject = "Rechnung $leistungszeitraum"
        val to = "verrechnung@sw-xperts.com" // load from props
        val body =
            "Sg. Damen und Herren,\nAnbei meine Rechnung und der Leistungsnachweis für $leistungszeitraum.\nMit freundlichen Grüßen,\nSellemond"

        val command =
            "open mailto:${to}?subject=${sanitizeUrl(subject)}&body=${sanitizeUrl(body)}"

        println(command)
        Runtime.getRuntime().exec(command)
    }

}

@Component
class WindowsEmailInitiator : EmailInitiator {
    override fun initiateEmail() {
        TODO("Not yet implemented")
    }
}

@Configuration
class EmailInitiatorConfiguration {

    @Bean
    fun emailInitiator(): EmailInitiator = if (System.getProperty("os.name").contains("Mac")) {
        MacEmailInitiator()
    } else {
        WindowsEmailInitiator()
    }
}