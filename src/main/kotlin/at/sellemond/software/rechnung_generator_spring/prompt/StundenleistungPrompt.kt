package at.sellemond.software.rechnung_generator_spring.prompt

class StundenleistungPrompt {
    fun prompt(): Int {
        println("Wie viele Stunden wurden im Leistungszeitraum erbracht?")
        return readLine()!!.toInt()
    }
}
