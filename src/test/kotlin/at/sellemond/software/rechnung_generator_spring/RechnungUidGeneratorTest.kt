package at.sellemond.software.rechnung_generator_spring
import at.sellemond.software.rechnung_generator_spring.util.RechnungUidGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class RechnungUidGeneratorTest {
    @CsvSource(value = ["unternehmenA:2020:10", "unternehmenB:2020:11"], delimiter = ':')
    @ParameterizedTest
    internal fun `should generate unique id with company name and year month`(
        unternehmen: String,
        year: Int,
        month: Int
    ) {
        assertThat(RechnungUidGenerator.getUid(unternehmen, year, month))
            .isEqualTo("Rechnung ${unternehmen}-$year/$month")
    }

    @CsvSource(value = ["unternehmenA:2020:1", "unternehmenB:2020:2"], delimiter = ':')
    @ParameterizedTest
    internal fun `should append leading zero if month is lt 10`(
        unternehmen: String,
        year: Int,
        month: Int
    ) {
        assertThat(RechnungUidGenerator.getUid(unternehmen, year, month))
            .isEqualTo("Rechnung ${unternehmen}-$year/0$month")
    }
}