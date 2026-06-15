package at.sellemond.software.rechnung_generator_spring.cli

import UnternehmenPropertiesParser
import at.sellemond.software.rechnung_generator_spring.PdfCreator
import at.sellemond.software.rechnung_generator_spring.harvest.HarvestException
import at.sellemond.software.rechnung_generator_spring.hours.HoursProviders
import at.sellemond.software.rechnung_generator_spring.model.AbrechnungProperties
import at.sellemond.software.rechnung_generator_spring.prompt.LeistungszeitraumFactory
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenPromptResult
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenPropertiesLoader
import at.sellemond.software.rechnung_generator_spring.termination.email.EmailInitiator
import at.sellemond.software.rechnung_generator_spring.termination.pathopener.PathOpener
import at.sellemond.software.rechnung_generator_spring.util.RechnungUidGenerator
import at.sellemond.software.rechnung_generator_spring.util.calculateNettobetrag
import at.sellemond.software.rechnung_generator_spring.util.calculateUstBetrag
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import java.io.File
import java.math.RoundingMode.HALF_UP
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Drives a complete invoice generation from command-line arguments (no prompts) and emits a single
 * machine-readable result. Reuses the same [PdfCreator]/[EmailInitiator]/[PathOpener] the
 * interactive runner uses, so the produced PDF, totals, and side effects are identical.
 */
@Component
class NonInteractiveInvoiceRunner(
    private val parser: InvoiceCliArgsParser,
    private val jsonWriter: GenerationResultJsonWriter,
    private val pdfCreator: PdfCreator,
    private val emailInitiator: EmailInitiator,
    private val pathOpener: PathOpener,
    private val abrechnungProperties: AbrechnungProperties,
    private val hoursProviders: HoursProviders,
    private val applicationArguments: ApplicationArguments,
) {

    fun run() {
        val format = parser.peekFormat(applicationArguments)
        val args = try {
            parser.parse(applicationArguments)
        } catch (e: CliValidationException) {
            emitError(format, e.code, e.message)
            exitProcess(exitCodeFor(e.code))
        }

        try {
            emitResult(args.format, generate(args))
        } catch (e: HarvestException) {
            emitError(args.format, e.code, e.message)
            exitProcess(exitCodeFor(e.code))
        } catch (e: CliValidationException) {
            emitError(args.format, e.code, e.message)
            exitProcess(exitCodeFor(e.code))
        } catch (e: Exception) {
            emitError(args.format, "UNEXPECTED_ERROR", e.message ?: e.toString())
            exitProcess(1)
        }
    }

    private fun generate(args: InvoiceCliArgs): GenerationResult {
        val customerProps = UnternehmenPropertiesLoader.load("kunden", args.customer)
        val issuerProps = UnternehmenPropertiesLoader.load("unternehmen", args.issuer)
        val period = LeistungszeitraumFactory.create(args.year, args.month)
        val hours = hoursProviders.resolve(args.hoursSource, args.hours).fetch(args.year, args.month, customerProps)

        val pdfFileName = pdfCreator.createRechnung(UnternehmenPromptResult(customerProps, issuerProps), period, hours)
        val pdfAbsPath = File(pdfFileName).absolutePath

        val kunde = UnternehmenPropertiesParser.parse(customerProps)
        val unternehmen = UnternehmenPropertiesParser.parse(issuerProps)

        val net = calculateNettobetrag(hours, abrechnungProperties.stundensatz)
        val vatRaw = calculateUstBetrag(net, abrechnungProperties.ust)
        val vat = vatRaw.setScale(2, HALF_UP)
        val gross = net.add(vatRaw).setScale(2, HALF_UP)

        val warnings = mutableListOf<String>()
        val attachments = mutableListOf<String>()

        if (args.mail) {
            val files: List<Path> = listOfNotNull(Path.of(pdfAbsPath), args.reportPdfPath?.toAbsolutePath())
            files.forEach { attachments.add(it.toString()) }
            runCatching { emailInitiator.initiateEmail(kunde, period.zeitraum, files) }
                .onFailure { warnings.add("Mail draft failed: ${it.message}") }
        }

        if (args.open) {
            runCatching {
                pathOpener.openRechnungFile(pdfFileName)
                pathOpener.openRechnungFolder(pdfFileName)
            }.onFailure { warnings.add("Finder open failed: ${it.message}") }
        }

        return GenerationResult(
            invoiceNumber = RechnungUidGenerator.getUid(kunde.shortName, args.year, args.month),
            pdfPath = pdfAbsPath,
            customer = kunde.shortName,
            issuer = unternehmen.shortName,
            period = args.period,
            hours = hours,
            net = net,
            vat = vat,
            gross = gross,
            attachments = attachments,
            warnings = warnings,
        )
    }

    private fun emitResult(format: OutputFormat, result: GenerationResult) = when (format) {
        OutputFormat.JSON -> jsonWriter.writeResult(result)
        OutputFormat.TEXT -> println(
            "Invoice ${result.invoiceNumber} -> ${result.pdfPath} " +
                "(net ${result.net}, USt ${result.vat}, gross ${result.gross})"
        )
    }

    private fun emitError(format: OutputFormat, code: String, message: String) = when (format) {
        OutputFormat.JSON -> jsonWriter.writeError(GenerationError(code, message))
        OutputFormat.TEXT -> System.err.println("ERROR [$code]: $message")
    }

    private fun exitCodeFor(code: String): Int = when (code) {
        "HARVEST_FETCH_FAILED" -> 3
        else -> 2
    }
}
