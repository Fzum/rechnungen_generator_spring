package at.sellemond.software.rechnung_generator_spring.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun toEuroFormat(localDateTime: LocalDateTime): String = DateTimeFormatter
    .ofPattern("dd.MM.yyyy")
    .format(localDateTime)

fun calculateNettobetrag(stundenleistung: Int, stundensatz: Int): Int = stundenleistung * stundensatz