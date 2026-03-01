package at.sellemond.software.rechnung_generator_spring.model

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "abrechnung")
data class AbrechnungProperties(
    val stundensatz: Double,
    val einheit: String,
    val taetigkeit: String,
    val ust: Int,
    val iban: String,
    val zahlungsbedingungen: String
)
