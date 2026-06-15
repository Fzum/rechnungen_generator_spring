package at.sellemond.software.rechnung_generator_spring.prompt

import java.time.LocalDateTime
import java.time.Month.JANUARY

data class LeistungszeitraumPromptResult(val year: Int, val month: Int, val zeitraum: String)

class LeistungszeitraumPrompt {
    fun promptUser(): LeistungszeitraumPromptResult {
        val now = LocalDateTime.now()

        val defaultYear = if (now.month == JANUARY) now.year - 1 else now.year
        val defaultMonth = if (now.monthValue == 1) 12 else now.monthValue - 1

        println("\nLeistungszeitraum festlegen")
        println("Standardvorschlag: $defaultMonth/$defaultYear (${LeistungszeitraumFactory.formatZeitraum(defaultYear, defaultMonth)})")
        print("Übernehmen mit ENTER oder neuen Monat eingeben (1-12): ")

        val inputMonth = readln()
        if (inputMonth.isEmpty()) {
            return LeistungszeitraumFactory.create(defaultYear, defaultMonth)
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

        return LeistungszeitraumFactory.create(year, month)
    }
}
