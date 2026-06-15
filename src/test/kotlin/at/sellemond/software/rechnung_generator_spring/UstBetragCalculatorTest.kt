package at.sellemond.software.rechnung_generator_spring

import at.sellemond.software.rechnung_generator_spring.util.calculateUstBetrag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class UstBetragCalculatorTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "100.00;20;20",
            "14250.00;20;2850",
            "96.82;20;19.364",
            "100.00;10;10",
            "250.00;7;17.5",
            "0;20;0",
        ],
        delimiter = ';'
    )
    fun `calculates ust as net times percent`(net: BigDecimal, ust: Int, expected: BigDecimal) {
        assertThat(calculateUstBetrag(net, ust)).isEqualByComparingTo(expected)
    }
}
