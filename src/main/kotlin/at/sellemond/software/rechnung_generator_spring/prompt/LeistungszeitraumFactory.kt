package at.sellemond.software.rechnung_generator_spring.prompt

import at.sellemond.software.rechnung_generator_spring.util.toEuroFormat
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters.lastDayOfMonth

/**
 * Builds a [LeistungszeitraumPromptResult] from a year/month, including the formatted `zeitraum`
 * label. Shared by the interactive prompt and the non-interactive CLI so both produce identical
 * periods.
 */
object LeistungszeitraumFactory {

    fun create(year: Int, month: Int): LeistungszeitraumPromptResult =
        LeistungszeitraumPromptResult(year, month, formatZeitraum(year, month))

    fun formatZeitraum(year: Int, month: Int): String {
        val lastDayOfMonth = LocalDateTime.of(year, month, 1, 0, 0).with(lastDayOfMonth())
        return "1.-${toEuroFormat(lastDayOfMonth)}"
    }
}
