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

        assertThat(split[0]).isEqualTo(now.dayOfMonth.toString())
        assertThat(split[1]).isEqualTo(now.monthValue.toString())
        assertThat(split[2]).isEqualTo(now.year.toString())
    }
}