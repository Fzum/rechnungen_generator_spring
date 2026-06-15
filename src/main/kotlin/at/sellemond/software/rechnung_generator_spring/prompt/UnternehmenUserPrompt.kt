package at.sellemond.software.rechnung_generator_spring.prompt

import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenResourceFolderOption.KUNDEN
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenResourceFolderOption.UNTERNEHMEN
import exception.SelectionOutOfBoundException
import java.util.*

data class UnternehmenPromptResult(val kundenProperties: Properties, val unternehmenProperties: Properties)

enum class UnternehmenResourceFolderOption(val folder: String) {
    KUNDEN("kunden"),
    UNTERNEHMEN("unternehmen"),
}

class UnternehmenUserPrompt {
    fun promptUser(): UnternehmenPromptResult {
        val kundenProperties = getPropertiesFromPrompt(KUNDEN)
        val unternehmenProperties = getPropertiesFromPrompt(UNTERNEHMEN)

        return UnternehmenPromptResult(kundenProperties, unternehmenProperties)
    }

    private fun getPropertiesFromPrompt(folderOption: UnternehmenResourceFolderOption): Properties {
        val options = UnternehmenPropertiesLoader.listOptions(folderOption.folder)
        if (options.size == 1) {
            return UnternehmenPropertiesLoader.load(folderOption.folder, options.first())
        }

        println("Für welchen ${folderOption.name} soll die Rechnung generiert werden?")

        options.forEachIndexed { index, s -> println("${index}: $s") }

        val selection = readln().toInt()
        if (selection < 0 || selection > options.size - 1) {
            throw SelectionOutOfBoundException()
        }

        return UnternehmenPropertiesLoader.load(folderOption.folder, options[selection])
    }
}
