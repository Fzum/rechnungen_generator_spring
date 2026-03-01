package at.sellemond.software.rechnung_generator_spring

import LeistungsaufstellungTable
import RechnungenFolderCreator
import UnternehmenPropertiesParser
import at.sellemond.software.rechnung_generator_spring.model.AbrechnungProperties
import at.sellemond.software.rechnung_generator_spring.prompt.LeistungszeitraumPromptResult
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenPromptResult
import at.sellemond.software.rechnung_generator_spring.util.RechnungUidGenerator
import at.sellemond.software.rechnung_generator_spring.util.alignToBottom
import at.sellemond.software.rechnung_generator_spring.util.calculateNettobetrag
import at.sellemond.software.rechnung_generator_spring.util.formatCurrency
import at.sellemond.software.rechnung_generator_spring.util.toEuroFormat
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import model.Unternehmen
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

@Component
class PdfCreator(private val abrechnungProperties: AbrechnungProperties) {
    fun createRechnung(
        unternehmenPromptResult: UnternehmenPromptResult,
        leistungszeitraumPromptResult: LeistungszeitraumPromptResult,
        stundenleistungPromptResult: BigDecimal
    ): String {
        val kunde = UnternehmenPropertiesParser.parse(unternehmenPromptResult.kundenProperties)
        RechnungenFolderCreator.createFolderIfNotPresent(kunde.shortName)

        val pdfFileName = getPdfFileName(kunde, leistungszeitraumPromptResult)
        val writer = PdfWriter(pdfFileName)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        val unternehmen = UnternehmenPropertiesParser.parse(unternehmenPromptResult.unternehmenProperties)

        val nettobetrag = calculateNettobetrag(stundenleistungPromptResult, abrechnungProperties.stundensatz)
        val (ustBetragParagraph, ustBetrag) = getUstBetrag(abrechnungProperties, nettobetrag)

        document
            .add(Paragraph(unternehmen.toString()))
            .add(Paragraph("An die:\n$kunde"))
            .add(getAusstellungsdatumParagraph())
            .add(getLeistungszeitraumParagraph(kunde, leistungszeitraumPromptResult))
            .add(Paragraph("Leistungszeitraum ${leistungszeitraumPromptResult.zeitraum}"))
            .add(Paragraph("Ihre (Kunden) UID Nr.: ${kunde.uid}"))
            .add(getTable(stundenleistungPromptResult, abrechnungProperties))
            .add(ustBetragParagraph)
            .add(getGesamtbetragWithUstParagraph(ustBetrag, nettobetrag))
            .add(getZahlungsbedingungenParagraph(abrechnungProperties))
            .add(getZahlungsaufforderungParagraph(abrechnungProperties))
            .add(getImpressumParagraph(unternehmen).alignToBottom(pdf, document))

        document.close()

        return pdfFileName
    }

    private fun getPdfFileName(kunde: Unternehmen, leistungszeitraumPromptResult: LeistungszeitraumPromptResult) =
        "${RechnungenFolderCreator.destination}/${kunde.shortName}/rechnung_${kunde.shortName}" +
                "_${leistungszeitraumPromptResult.month}_${leistungszeitraumPromptResult.year}.pdf"

    private fun getLeistungszeitraumParagraph(
        kunde: Unternehmen,
        leistungszeitraumPromptResult: LeistungszeitraumPromptResult
    ) = Paragraph(
        RechnungUidGenerator.getUid(
            kunde.shortName,
            leistungszeitraumPromptResult.year,
            leistungszeitraumPromptResult.month
        )
    )

    private fun getAusstellungsdatumParagraph(): Paragraph =
        Paragraph("Wien, ${toEuroFormat(LocalDateTime.now())}").setTextAlignment(TextAlignment.RIGHT)

    private fun getTable(stundenLeistung: BigDecimal, abrechnungProperties: AbrechnungProperties) =
        LeistungsaufstellungTable()
            .addMenge(stundenLeistung)
            .addEinheit(abrechnungProperties.einheit)
            .addStundensatz(abrechnungProperties.stundensatz)
            .addUst(abrechnungProperties.ust)
            .addTaetigkeit(abrechnungProperties.taetigkeit)
            .addBetrag(calculateNettobetrag(stundenLeistung, abrechnungProperties.stundensatz))

    private fun getUstBetrag(
        abrechnungProperties: AbrechnungProperties,
        nettobetrag: BigDecimal
    ): Pair<Paragraph, BigDecimal> {
        val ustValue = BigDecimal(abrechnungProperties.ust)
        val ustBetrag = nettobetrag.times((ustValue.divide(BigDecimal(100))))
        return Pair(
            Paragraph("${abrechnungProperties.ust}% Ust: ${formatCurrency(ustBetrag)}")
                .setTextAlignment(TextAlignment.RIGHT)
                .setUnderline(), ustBetrag
        )
    }

    private fun getGesamtbetragWithUstParagraph(ustBetrag: BigDecimal, nettobetrag: BigDecimal): Paragraph =
        Paragraph("Gesamtbetrag inkl. Ust: ${formatCurrency(ustBetrag.add(nettobetrag))}")
            .setTextAlignment(TextAlignment.RIGHT)
            .setUnderline()

    private fun getZahlungsbedingungenParagraph(abrechnungProperties: AbrechnungProperties): Paragraph =
        Paragraph("Zahlungsbedingungen: ${abrechnungProperties.zahlungsbedingungen}")

    private fun getZahlungsaufforderungParagraph(abrechnungProperties: AbrechnungProperties): Paragraph = Paragraph(
        "Ich bedanke mich für Ihren Auftrag und ersuche um Überweisung des Betrags auf das Konto der " +
                "Musterbank, IBAN: ${abrechnungProperties.iban}."
    )

    private fun getImpressumParagraph(unternehmen: Unternehmen): Paragraph =
        Paragraph("UID NR.: ${unternehmen.uid}\nSitz in ${unternehmen.adresse}")
            .setUnderline()
            .setTextAlignment(TextAlignment.CENTER)

}
