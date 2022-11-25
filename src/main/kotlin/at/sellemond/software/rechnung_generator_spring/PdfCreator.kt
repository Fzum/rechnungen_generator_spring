import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import at.sellemond.software.rechnung_generator_spring.model.AbrechnungProperties
import model.Unternehmen
import at.sellemond.software.rechnung_generator_spring.prompt.LeistungszeitraumPromptResult
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenPromptResult
import at.sellemond.software.rechnung_generator_spring.util.RechnungUidGenerator
import at.sellemond.software.rechnung_generator_spring.util.calculateNettobetrag
import at.sellemond.software.rechnung_generator_spring.util.toEuroFormat
import java.time.LocalDateTime

class PdfCreator {
    companion object {
        fun createRechnung(
            unternehmenPromptResult: UnternehmenPromptResult,
            leistungszeitraumPromptResult: LeistungszeitraumPromptResult,
            stundenleistungPromptResult: Int
        ) {
            val kunde = UnternehmenPropertiesParser.parse(unternehmenPromptResult.kundenProperties)
            RechnungenFolderCreator.createFolderIfNotPresent(kunde.name)

            val writer = PdfWriter(getPdfFileName(kunde, leistungszeitraumPromptResult))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            val unternehmen = UnternehmenPropertiesParser.parse(unternehmenPromptResult.unternehmenProperties)
            val abrechnungProperties = AbrechnungProperties()

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
                .add(getZahlungsaufforderungParagraph(abrechnungProperties))
                .add(getImpressumParagraph(unternehmen))

            document.close()
        }

        private fun getPdfFileName(kunde: Unternehmen, leistungszeitraumPromptResult: LeistungszeitraumPromptResult) =
            "${RechnungenFolderCreator.destination}/${kunde.name}/sellemond_${kunde.name}" +
                    "_${leistungszeitraumPromptResult.month}_${leistungszeitraumPromptResult.year}.pdf"

        private fun getLeistungszeitraumParagraph(
            kunde: Unternehmen,
            leistungszeitraumPromptResult: LeistungszeitraumPromptResult
        ) = Paragraph(
            RechnungUidGenerator.getUid(
                kunde.name,
                leistungszeitraumPromptResult.year,
                leistungszeitraumPromptResult.month
            )
        )

        private fun getAusstellungsdatumParagraph(): Paragraph =
            Paragraph("Wien, ${toEuroFormat(LocalDateTime.now())}").setTextAlignment(TextAlignment.RIGHT)

        private fun getTable(stundenLeistung: Int, abrechnungProperties: AbrechnungProperties) = LeistungsaufstellungTable()
            .addMenge(stundenLeistung)
            .addEinheit(abrechnungProperties.einheit)
            .addStundensatz(abrechnungProperties.stundensatz)
            .addUst(abrechnungProperties.ust)
            .addTaetigkeit(abrechnungProperties.taetigkeit)
            .addBetrag(calculateNettobetrag(stundenLeistung, abrechnungProperties.stundensatz))

        private fun getUstBetrag(
            abrechnungProperties: AbrechnungProperties,
            nettobetrag: Int
        ): Pair<Paragraph, Double> {
            val ustBetrag = nettobetrag * (abrechnungProperties.ust.toDouble() / 100)
            return Pair(
                Paragraph("${abrechnungProperties.ust}% Ust: $ustBetrag")
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setUnderline(), ustBetrag
            )
        }

        private fun getGesamtbetragWithUstParagraph(ustBetrag: Double, nettobetrag: Int): Paragraph =
            Paragraph("Gesamtbetrag inkl. Ust: ${ustBetrag + nettobetrag}")
                .setTextAlignment(TextAlignment.RIGHT)
                .setUnderline()

        private fun getZahlungsaufforderungParagraph(abrechnungProperties: AbrechnungProperties): Paragraph = Paragraph(
            "Ich bedanke mich für Ihren Auftrag und ersuche um Überweisung des Betrags auf das Konto der " +
                    "Raiffeisen-Landesbank Tirol, IBAN: ${abrechnungProperties.iban}."
        )

        private fun getImpressumParagraph(unternehmen: Unternehmen): Paragraph? =
            Paragraph("Sitz in ${unternehmen.adresse} \n UID NR.: ${unternehmen.uid}")
                .setUnderline()
                .setTextAlignment(TextAlignment.CENTER)
    }
}
