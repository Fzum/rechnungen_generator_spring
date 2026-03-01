import model.Adresse
import model.Contact
import model.Unternehmen
import java.util.*

class UnternehmenPropertiesParser {
    companion object {
        fun parse(properties: Properties) = Unternehmen(
            properties.getProperty("short-name"),
            properties.getProperty("name"),
            getAdresse(properties),
            properties.getProperty("uid"),
            getContact(properties)
        )

        private fun getAdresse(properties: Properties) = Adresse(
            properties.getProperty("strasse"),
            properties.getProperty("stadt"),
            properties.getProperty("land")
        )

        private fun getContact(properties: Properties) = Contact(
            properties.getProperty("telNr", ""),
            properties.getProperty("website", ""),
            properties.getProperty("email")
        )
    }
}