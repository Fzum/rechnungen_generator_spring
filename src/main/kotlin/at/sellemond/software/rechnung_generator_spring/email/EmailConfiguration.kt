package at.sellemond.software.rechnung_generator_spring.email

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.PropertySource

// todo this does not read in the values yet fix!
@PropertySource("classpath:email.yaml")
@ConfigurationProperties(prefix = "email")
data class EmailConfiguration(
    var subject: String = "",
    var body: String = ""
)