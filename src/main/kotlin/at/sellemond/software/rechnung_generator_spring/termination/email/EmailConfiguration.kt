package at.sellemond.software.rechnung_generator_spring.termination.email

import at.sellemond.software.rechnung_generator_spring.termination.OperatingSystem.*
import at.sellemond.software.rechnung_generator_spring.termination.getOperatingSystem
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "email")
data class EmailProperties(val subject: String, val body: String)

@Configuration
class EmailInitiatorConfiguration {

    @Bean
    fun emailInitiator(emailProperties: EmailProperties) =
        when (getOperatingSystem()) {
            MAC -> MacEmailInitiator(emailProperties)
            WINDOWS -> WindowsEmailInitiator()
            OTHER -> throw IllegalStateException("OS not supported")
        }
}