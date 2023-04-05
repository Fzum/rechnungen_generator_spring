package at.sellemond.software.rechnung_generator_spring.util

import java.math.BigDecimal
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun toEuroFormat(localDateTime: LocalDateTime): String = DateTimeFormatter
    .ofPattern("dd.MM.yyyy")
    .format(localDateTime)

fun calculateNettobetrag(stundenleistung: BigDecimal, stundensatz: Int): BigDecimal {
    return stundenleistung.multiply(BigDecimal(stundensatz))
}

fun sanitizeUrl(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
