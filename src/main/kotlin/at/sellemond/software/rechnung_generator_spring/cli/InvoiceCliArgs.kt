package at.sellemond.software.rechnung_generator_spring.cli

import java.math.BigDecimal
import java.nio.file.Path

/** Parsed and validated non-interactive invocation. */
data class InvoiceCliArgs(
    val issuer: String,
    val customer: String,
    val year: Int,
    val month: Int,
    val hoursSource: HoursSource,
    val hours: BigDecimal?,
    val reportPdfPath: Path?,
    val mail: Boolean,
    val open: Boolean,
    val format: OutputFormat,
) {
    val period: String get() = "%04d-%02d".format(year, month)
}
