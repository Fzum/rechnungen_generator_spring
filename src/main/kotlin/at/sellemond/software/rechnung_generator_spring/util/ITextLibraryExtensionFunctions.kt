package at.sellemond.software.rechnung_generator_spring.util

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.BlockElement
import com.itextpdf.layout.element.IBlockElement

fun BlockElement<*>.alignToBottom(pdf: PdfDocument, doc: Document): IBlockElement {
    this.setFixedPosition(
        doc.leftMargin,
        doc.bottomMargin,
        pdf.defaultPageSize.width - doc.leftMargin - doc.rightMargin
    )
    return this
}