package at.sellemond.software.rechnung_generator_spring.cli

import java.math.BigDecimal

/** Machine-readable outcome of a non-interactive run (see contracts/generation-result.schema.json). */
data class GenerationResult(
    val invoiceNumber: String,
    val pdfPath: String,
    val customer: String,
    val issuer: String,
    val period: String,
    val hours: BigDecimal,
    val net: BigDecimal,
    val vat: BigDecimal,
    val gross: BigDecimal,
    val attachments: List<String>,
    val warnings: List<String>,
)

data class GenerationError(val code: String, val message: String)
