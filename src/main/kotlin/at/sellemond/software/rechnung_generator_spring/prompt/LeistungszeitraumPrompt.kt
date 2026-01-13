package at.sellemond.software.rechnung_generator_spring.prompt

import at.sellemond.software.rechnung_generator_spring.util.toEuroFormat
import java.time.LocalDateTime
import java.time.Month.JANUARY
import java.time.temporal.TemporalAdjusters.lastDayOfMonth

data class LeistungszeitraumPromptResult(val year: Int, val month: Int, val zeitraum: String)

class LeistungszeitraumPrompt {
    fun promptUser(): LeistungszeitraumPromptResult {
        val now = LocalDateTime.now()

        val defaultYear = if (now.month == JANUARY) now.year - 1 else now.year
        val defaultMonth = if (now.monthValue == 1) 12 else now.monthValue - 1

        println("\nLeistungszeitraum festlegen")
        println("Standardvorschlag: $defaultMonth/$defaultYear (${getLeistungszeitraumFormatted(defaultYear, defaultMonth)})")
        print("Übernehmen mit ENTER oder neuen Monat eingeben (1-12): ")

        val inputMonth = readln()
        if (inputMonth.isEmpty()) {
            return LeistungszeitraumPromptResult(defaultYear, defaultMonth, getLeistungszeitraumFormatted(defaultYear, defaultMonth))
        }

        val month = try {
            inputMonth.toInt()
        } catch (_: NumberFormatException) {
            println("Ungültiger Monat. Verwende Standardwert $defaultMonth.")
            defaultMonth
        }

        print("Jahr eingeben [$defaultYear]: ")
        val inputYear = readln()
        val year = if (inputYear.isEmpty()) {
            defaultYear
        } else {
            try {
                inputYear.toInt()
            } catch (_: NumberFormatException) {
                println("Ungültiges Jahr. Verwende Standardwert $defaultYear.")
                defaultYear
            }
        }

        return LeistungszeitraumPromptResult(year, month, getLeistungszeitraumFormatted(year, month))
    }

    private fun getLeistungszeitraumFormatted(year: Int, month: Int): String {
        val lastDayOfMonth = LocalDateTime
            .of(year, month, 1, 0, 0)
            .with(lastDayOfMonth())

        return "1.-${toEuroFormat(lastDayOfMonth)}"
    }
}
