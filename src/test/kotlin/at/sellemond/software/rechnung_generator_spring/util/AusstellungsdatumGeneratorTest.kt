package at.sellemond.software.rechnung_generator_spring.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class AusstellungsdatumGeneratorTest {
    @Test
    internal fun `should have current date as ausstellungsdatum`() {
        val ausstellungsdatum = toEuroFormat(LocalDateTime.now())

        val split = ausstellungsdatum.split(".")
        val now = LocalDateTime.now()

        val appendZeroIfLtTen = { x: Int -> if (x < 10) "0$x" else x.toString() }

        assertThat(split[0]).isEqualTo(appendZeroIfLtTen(now.dayOfMonth))
        assertThat(split[1]).isEqualTo(appendZeroIfLtTen(now.monthValue))
        assertThat(split[2]).isEqualTo(now.year.toString())
    }
}