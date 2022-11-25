import model.Adresse
import model.Contact
import model.Unternehmen
import java.util.Properties

class UnternehmenPropertiesParser {
    companion object {
        fun parse(properties: Properties) = Unternehmen(
            properties.getProperty("name"),
            getAdresse(properties),
            properties.getProperty("uid"),
            getContactIfPresent(properties)
        )

        private fun getAdresse(properties: Properties) = Adresse(
            properties.getProperty("strasse"),
            properties.getProperty("stadt"),
            properties.getProperty("land")
        )

        private fun getContactIfPresent(properties: Properties) = Contact(
            properties.getProperty("telNr", ""),
            properties.getProperty("website", ""),
        )
    }
}