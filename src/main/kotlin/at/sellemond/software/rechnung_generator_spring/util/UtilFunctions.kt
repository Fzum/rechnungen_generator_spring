package at.sellemond.software.rechnung_generator_spring.util

import java.math.BigDecimal
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun toEuroFormat(localDateTime: LocalDateTime): String = DateTimeFormatter
    .ofPattern("dd.MM.yyyy")
    .format(localDateTime)

fun calculateNettobetrag(stundenleistung: BigDecimal, stundensatz: Double): BigDecimal =
    stundenleistung.multiply(BigDecimal.valueOf(stundensatz))

fun sanitizeUrl(s: String) = URLEncoder.encode(
    s.replace("ue", "ü")
        .replace("ae", "ä")
        .replace("oe", "ö")
        .replace("Ue", "Ü")
        .replace("Ae", "Ä")
        .replace("Oe", "Ö")
        .replace("ss", "ß"),
    "UTF-8"
).replace("+", "%20")

fun replacePlaceholder(input: String, placeholder: String, replacement: String) =
    input.replace("{{$placeholder}}", replacement)

fun removeLastPathComponent(path: String): String {
    val lastSlashIndex = path.lastIndexOf("/")
    return if (lastSlashIndex >= 0) path.substring(0, lastSlashIndex) else path
}