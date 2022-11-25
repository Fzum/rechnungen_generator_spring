package at.sellemond.software.rechnung_generator_spring.prompt

import exception.SelectionOutOfBoundException
import at.sellemond.software.rechnung_generator_spring.util.toEuroFormat
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters.*

data class LeistungszeitraumPromptResult(val year: Int, val month: Int, val zeitraum: String)

private fun printSelection(x: Map.Entry<Int, Int>) = println("${x.key}: ${x.value}")

class LeistungszeitraumPrompt {
    fun promptUser(): LeistungszeitraumPromptResult {
        println("Für welches Jahr soll die Rechnung erzeugt werden?")
        val now = LocalDateTime.now()
        val currentYear = now.year

        val yearSelectionOptions = mapOf(0 to currentYear, 1 to currentYear - 1)
        yearSelectionOptions.forEach { printSelection(it) }

        val yearSelection = readLine()?.toInt()!!
        if (yearSelection < 0 || yearSelection > yearSelectionOptions.size - 1) {
            throw SelectionOutOfBoundException()
        }

        println("Für welches Monat soll die Rechnung erzeugt werden?")
        val currentMonth = now.monthValue

        val monthSelectionOptions = mapOf(0 to currentMonth, 1 to currentMonth - 1, 2 to currentMonth - 2)
        monthSelectionOptions.forEach { printSelection(it) }

        val monthSelection = readLine()?.toInt()!!
        if (monthSelection < 0 || monthSelection > monthSelectionOptions.size - 1) {
            throw SelectionOutOfBoundException()
        }

        val year = yearSelectionOptions[yearSelection]!!
        val month = monthSelectionOptions[monthSelection]!!

        return LeistungszeitraumPromptResult(year, month, getLeistungszeitraumFormatted(year, month))
    }

    private fun getLeistungszeitraumFormatted(year: Int, month: Int): String {
        val lastDayOfMonth = LocalDateTime
            .of(year, month, 1, 0, 0)
            .with(lastDayOfMonth())

        return "1.-${toEuroFormat(lastDayOfMonth)}"
    }
}
