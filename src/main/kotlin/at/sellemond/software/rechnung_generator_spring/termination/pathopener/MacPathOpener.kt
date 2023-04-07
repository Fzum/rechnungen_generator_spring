package at.sellemond.software.rechnung_generator_spring.termination.pathopener

import at.sellemond.software.rechnung_generator_spring.util.removeLastPathComponent

class MacPathOpener : PathOpener {
    override fun openRechnungFile(pdfFileName: String) {
        Runtime.getRuntime().exec("open $pdfFileName")
    }

    override fun openRechnungFolder(pdfFileName: String) {
        Runtime.getRuntime().exec("open ${removeLastPathComponent(pdfFileName)}")
    }
}