package at.sellemond.software.rechnung_generator_spring

import at.sellemond.software.rechnung_generator_spring.util.calculateNettobetrag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class NettobetragCalculatorTest {

    @ParameterizedTest
    @CsvSource(value = ["1;10", "1.25;12.50", "1.5;15.0", "1.75;17.50", "2;20"], delimiter = ';')
    fun `should calculate nettopreis with constant stundensatz`(input: BigDecimal, expected: BigDecimal) {
        val calculateNettobetrag = calculateNettobetrag(input, 10)
        assertThat(calculateNettobetrag).isEqualTo(expected)
    }
}