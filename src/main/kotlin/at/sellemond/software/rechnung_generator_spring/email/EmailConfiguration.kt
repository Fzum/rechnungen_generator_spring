package at.sellemond.software.rechnung_generator_spring.email

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "email")
data class EmailProperties(val subject: String, val body: String)

@Configuration
class EmailInitiatorConfiguration {

    @Bean
    fun emailInitiator(emailProperties: EmailProperties): EmailInitiator =
        if (System.getProperty("os.name").contains("Mac")) {
            MacEmailInitiator(emailProperties)
        } else {
            WindowsEmailInitiator()
        }
}