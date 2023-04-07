package at.sellemond.software.rechnung_generator_spring.termination.pathopener

interface PathOpener {
    fun openRechnungFile(pdfFileName: String);
    fun openRechnungFolder(pdfFileName: String)
}

