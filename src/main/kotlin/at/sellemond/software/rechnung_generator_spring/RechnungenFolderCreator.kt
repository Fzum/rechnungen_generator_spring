import java.io.File

class RechnungenFolderCreator {
    companion object {
        const val  destination = "rechnungen"
        val getFileDestination: (String) -> String = { "$destination/$it" }
        fun createFolderIfNotPresent(kunde: String): Boolean = File(getFileDestination(kunde)).mkdirs()
    }
}