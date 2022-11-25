package at.sellemond.software.rechnung_generator_spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RechnungGeneratorSpringApplication

fun main(args: Array<String>) {
	runApplication<RechnungGeneratorSpringApplication>(*args)
}
