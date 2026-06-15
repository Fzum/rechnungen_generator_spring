package at.sellemond.software.rechnung_generator_spring.cli

import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Serializes a [GenerationResult] / [GenerationError] to a single JSON object on stdout. Hand-rolled
 * (Jackson is not on the classpath and the object is small/flat) with explicit string escaping and
 * `BigDecimal` scale preservation.
 */
@Component
class GenerationResultJsonWriter {

    fun writeResult(result: GenerationResult) = println(toJson(result))

    fun writeError(error: GenerationError) = println(toJson(error))

    fun toJson(r: GenerationResult): String =
        "{" +
            "\"invoiceNumber\":${str(r.invoiceNumber)}," +
            "\"pdfPath\":${str(r.pdfPath)}," +
            "\"customer\":${str(r.customer)}," +
            "\"issuer\":${str(r.issuer)}," +
            "\"period\":${str(r.period)}," +
            "\"hours\":${num(r.hours)}," +
            "\"net\":${num(r.net)}," +
            "\"vat\":${num(r.vat)}," +
            "\"gross\":${num(r.gross)}," +
            "\"attachments\":${arr(r.attachments)}," +
            "\"warnings\":${arr(r.warnings)}" +
            "}"

    fun toJson(e: GenerationError): String =
        "{\"error\":{\"code\":${str(e.code)},\"message\":${str(e.message)}}}"

    private fun num(b: BigDecimal): String = b.toPlainString()

    private fun arr(items: List<String>): String = items.joinToString(",", "[", "]") { str(it) }

    private fun str(s: String): String = buildString {
        append('"')
        for (c in s) when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
        append('"')
    }
}
