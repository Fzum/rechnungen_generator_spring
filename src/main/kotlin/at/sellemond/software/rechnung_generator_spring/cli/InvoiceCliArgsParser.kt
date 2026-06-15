package at.sellemond.software.rechnung_generator_spring.cli

import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenPropertiesLoader
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.YearMonth

/**
 * Parses and validates the non-interactive command-line options into [InvoiceCliArgs].
 * Throws [CliValidationException] (with a machine-readable code) on any invalid/missing input.
 */
@Component
class InvoiceCliArgsParser {

    fun parse(args: ApplicationArguments): InvoiceCliArgs {
        val issuer = required(args, "issuer")
        if (issuer !in UnternehmenPropertiesLoader.listOptions("unternehmen")) {
            throw CliValidationException("UNKNOWN_ISSUER", "Unknown issuer '$issuer'")
        }

        val customer = required(args, "customer")
        if (customer !in UnternehmenPropertiesLoader.listOptions("kunden")) {
            throw CliValidationException("UNKNOWN_CUSTOMER", "Unknown customer '$customer'")
        }

        val (year, month) = parsePeriod(required(args, "period"))

        val hoursSource = parseHoursSource(opt(args, "hours-source") ?: "manual")
        val hours = opt(args, "hours")?.let { parseHours(it) }
        if (hoursSource == HoursSource.MANUAL && hours == null) {
            throw CliValidationException("MISSING_HOURS", "--hours is required when --hours-source=manual")
        }

        val mail = bool(args, "mail", default = true)
        val open = bool(args, "open", default = true)

        val reportPdfPath = opt(args, "report-pdf")?.let { Path.of(it) }
        if (mail) {
            if (reportPdfPath == null) {
                throw CliValidationException("MISSING_REPORT_PDF", "--report-pdf is required when --mail=true")
            }
            if (!Files.isReadable(reportPdfPath)) {
                throw CliValidationException("MISSING_REPORT_PDF", "report PDF not readable: $reportPdfPath")
            }
        }

        val format = parseFormat(opt(args, "format") ?: "text")

        return InvoiceCliArgs(issuer, customer, year, month, hoursSource, hours, reportPdfPath, mail, open, format)
    }

    /** Best-effort format detection for emitting an error before/while full parsing fails. */
    fun peekFormat(args: ApplicationArguments): OutputFormat =
        runCatching { parseFormat(opt(args, "format") ?: "text") }.getOrDefault(OutputFormat.TEXT)

    private fun opt(args: ApplicationArguments, name: String): String? =
        if (args.containsOption(name)) args.getOptionValues(name).firstOrNull() else null

    private fun required(args: ApplicationArguments, name: String): String =
        opt(args, name) ?: throw CliValidationException("INVALID_ARGUMENTS", "--$name is required")

    private fun bool(args: ApplicationArguments, name: String, default: Boolean): Boolean {
        if (!args.containsOption(name)) return default
        val value = args.getOptionValues(name).firstOrNull() ?: return true
        return value.equals("true", ignoreCase = true)
    }

    private fun parsePeriod(raw: String): Pair<Int, Int> {
        val parts = raw.split("-")
        if (parts.size != 2) throw CliValidationException("INVALID_PERIOD", "period must be YYYY-MM, got '$raw'")
        val year = parts[0].toIntOrNull()
        val month = parts[1].toIntOrNull()
        if (year == null || month == null || month !in 1..12) {
            throw CliValidationException("INVALID_PERIOD", "invalid period '$raw'")
        }
        if (YearMonth.of(year, month).isAfter(YearMonth.now())) {
            throw CliValidationException("INVALID_PERIOD", "period '$raw' is in the future")
        }
        return year to month
    }

    private fun parseHours(raw: String): BigDecimal =
        raw.toBigDecimalOrNull() ?: throw CliValidationException("INVALID_ARGUMENTS", "invalid --hours '$raw'")

    private fun parseHoursSource(raw: String): HoursSource = when (raw.lowercase()) {
        "manual" -> HoursSource.MANUAL
        "harvest" -> HoursSource.HARVEST
        else -> throw CliValidationException("INVALID_ARGUMENTS", "invalid --hours-source '$raw'")
    }

    private fun parseFormat(raw: String): OutputFormat = when (raw.lowercase()) {
        "text" -> OutputFormat.TEXT
        "json" -> OutputFormat.JSON
        else -> throw CliValidationException("INVALID_ARGUMENTS", "invalid --format '$raw'")
    }
}
