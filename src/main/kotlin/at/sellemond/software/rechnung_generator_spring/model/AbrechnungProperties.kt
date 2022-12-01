package at.sellemond.software.rechnung_generator_spring.model

import java.util.Properties

class AbrechnungProperties {
    var stundensatz: Int
    var einheit: String
    var taetigkeit: String
    var ust: Int
    var iban: String
    var zahlungsbedingungen: String

    init {
        val prop = Properties()
        prop.load(AbrechnungProperties::class.java.classLoader.getResourceAsStream("abrechnung.properties"))

        stundensatz = prop.getProperty("stundensatz")!!.toInt()
        einheit = prop.getProperty("abrechnungseinheit")!!
        taetigkeit = prop.getProperty("taetigkeit")!!
        ust = prop.getProperty("ust")!!.toInt()
        iban = prop.getProperty("iban")
        zahlungsbedingungen = prop.getProperty("zahlungsbedingungen")
    }
}