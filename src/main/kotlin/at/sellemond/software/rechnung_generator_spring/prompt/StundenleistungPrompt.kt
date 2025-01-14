package at.sellemond.software.rechnung_generator_spring.prompt

import java.math.BigDecimal

class StundenleistungPrompt {
    fun prompt(leistungszeitraumPromptResult: LeistungszeitraumPromptResult): BigDecimal {
        println("\nWie viele Stunden wurden im Leistungszeitraum erbracht? ${leistungszeitraumPromptResult.zeitraum}")
        return readln().toBigDecimal()
    }
}
