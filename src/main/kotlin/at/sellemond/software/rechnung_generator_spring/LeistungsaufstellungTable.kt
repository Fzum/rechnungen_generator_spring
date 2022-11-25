import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table

class LeistungsaufstellungTable : Table(6) {
    init {
        useAllAvailableWidth()
        val headerCells = listOf("Menge", "Einheit", "Tätigkeit", "Stundensatz", "Ust%", "Nettobetrag")
        headerCells
            .map { Paragraph(it) }
            .forEach {
                addHeaderCell(it)
                addCell(Cell())
            }
    }

    fun addMenge(menge: Int): LeistungsaufstellungTable {
        addParagraphToCol(0, menge.toString())
        return this
    }

    fun addEinheit(einheit: String): LeistungsaufstellungTable {
        addParagraphToCol(1, einheit)
        return this
    }

    fun addTaetigkeit(taetigkeit: String): LeistungsaufstellungTable {
        addParagraphToCol(2, taetigkeit)
        return this
    }

    fun addStundensatz(stundensatz: Int): LeistungsaufstellungTable {
        addParagraphToCol(3, stundensatz.toString())
        return this
    }

    fun addUst(ust: Int): LeistungsaufstellungTable {
        addParagraphToCol(4, ust.toString())
        return this
    }

    fun addBetrag(betrag: Int): LeistungsaufstellungTable {
        addParagraphToCol(5, betrag.toString())
        return this
    }

    private fun addParagraphToCol(col: Int, content: String) = getCell(0, col).add(Paragraph(content))
}