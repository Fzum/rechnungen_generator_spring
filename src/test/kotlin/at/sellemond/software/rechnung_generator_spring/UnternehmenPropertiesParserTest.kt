package at.sellemond.software.rechnung_generator_spring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.*

internal class UnternehmenPropertiesParserTest {
    @Test
    internal fun `should read properties without contact data`() {
        val properties = Properties()
        val propertyStream = getResourceAsStream("kunden/kunde.properties")
        properties.load(propertyStream)

        val unternehmen = UnternehmenPropertiesParser.parse(properties)

        assertThat(unternehmen.uid).isEqualTo("uid")
        assertThat(unternehmen.name).isEqualTo("name")

        assertThat(unternehmen.contact.telNr).isEmpty()
        assertThat(unternehmen.contact.website).isEmpty()

        assertThat(unternehmen.adresse.land).isEqualTo("land")
        assertThat(unternehmen.adresse.stadt).isEqualTo("stadt")
        assertThat(unternehmen.adresse.strasse).isEqualTo("strasse")
    }

    @Test
    internal fun `should read properties with contact data`() {
        val properties = Properties()
        val propertyStream = getResourceAsStream("unternehmen/unternehmen.properties")
        properties.load(propertyStream)

        val unternehmen = UnternehmenPropertiesParser.parse(properties)

        assertThat(unternehmen.uid).isEqualTo("uid")
        assertThat(unternehmen.name).isEqualTo("name")

        assertThat(unternehmen.contact.telNr).isEqualTo("telNr")
        assertThat(unternehmen.contact.website).isEqualTo("website")

        assertThat(unternehmen.adresse.land).isEqualTo("land")
        assertThat(unternehmen.adresse.stadt).isEqualTo("stadt")
        assertThat(unternehmen.adresse.strasse).isEqualTo("strasse")
    }

    private fun getResourceAsStream(path: String): InputStream? =
        UnternehmenPropertiesParserTest::class.java.classLoader.getResourceAsStream(path)
}