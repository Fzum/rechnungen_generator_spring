package at.sellemond.software.rechnung_generator_spring.prompt

import at.sellemond.software.rechnung_generator_spring.util.toEuroFormat
import java.time.LocalDateTime
import java.time.Month.JANUARY
import java.time.temporal.TemporalAdjusters.lastDayOfMonth

data class LeistungszeitraumPromptResult(val year: Int, val month: Int, val zeitraum: String)

class LeistungszeitraumPrompt {
    fun promptUser(): LeistungszeitraumPromptResult {
        val now = LocalDateTime.now()

        val year = if (now.month == JANUARY) now.year - 1 else now.year
        val month = if (now.monthValue == 1) 12 else now.monthValue - 1

        return LeistungszeitraumPromptResult(year, month, getLeistungszeitraumFormatted(year, month))
    }

    private fun getLeistungszeitraumFormatted(year: Int, month: Int): String {
        val lastDayOfMonth = LocalDateTime
            .of(year, month, 1, 0, 0)
            .with(lastDayOfMonth())

        return "1.-${toEuroFormat(lastDayOfMonth)}"
    }
}
