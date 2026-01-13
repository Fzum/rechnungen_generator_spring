package at.sellemond.software.rechnung_generator_spring

import at.sellemond.software.rechnung_generator_spring.util.calculateNettobetrag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class NettobetragCalculatorTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "1;10;10",
            "1.25;10;12.50",
            "1.5;10;15.0",
            "1.75;10;17.50",
            "2;10;20",
            "1;96.82;96.82",
            "2;96.82;193.64",
            "0.333333;10;3.33",
            "0.666667;10;6.67",
        ],
        delimiter = ';'
    )
    fun `should calculate nettopreis with constant stundensatz`(
        input: BigDecimal,
        stundensatz: Double,
        expected: BigDecimal
    ) {
        val calculateNettobetrag = calculateNettobetrag(input, stundensatz)
        assertThat(calculateNettobetrag.stripTrailingZeros()).isEqualTo(expected.stripTrailingZeros())
    }
}