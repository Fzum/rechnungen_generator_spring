package at.sellemond.software.rechnung_generator_spring.prompt

import java.math.BigDecimal

class StundenleistungPrompt {
    fun prompt(): BigDecimal {
        println("Wie viele Stunden wurden im Leistungszeitraum erbracht?")
        return readln().toBigDecimal()
    }
}
