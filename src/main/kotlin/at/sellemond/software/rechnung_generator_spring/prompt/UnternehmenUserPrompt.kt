package at.sellemond.software.rechnung_generator_spring.prompt

import UnternehmenPropertiesParser
import at.sellemond.software.rechnung_generator_spring.ExternalConfigLoader
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenResourceFolderOption.KUNDEN
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenResourceFolderOption.UNTERNEHMEN
import exception.SelectionOutOfBoundException
import java.io.InputStream
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
        val options = getPromptOptions(folderOption)
        if (options.size == 1) {
            val properties = Properties()
            properties.load(getPropertiesFromPromptSelection(options, 0, folderOption))

            return properties
        }

        println("Für welchen ${folderOption.name} soll die Rechnung generiert werden?")

        options.forEachIndexed { index, s -> println("${index}: $s") }

        val selection = readln().toInt()
        if (selection < 0 || selection > options.size - 1) {
            throw SelectionOutOfBoundException()
        }

        val properties = Properties()
        properties.load(getPropertiesFromPromptSelection(options, selection, folderOption))

        return properties
    }

    private fun getPropertiesFromPromptSelection(
        selectionList: List<String>,
        selectionIndex: Int,
        resourceFolder: UnternehmenResourceFolderOption
    ): InputStream? {
        val name = selectionList[selectionIndex]
        // Try external file first (directory next to the JAR)
        val externalStream = ExternalConfigLoader.loadExternalProperties(resourceFolder.folder, name)
        if (externalStream != null) return externalStream
        // Fall back to bundled resource
        val file = "${resourceFolder.folder}/$name.properties"
        return UnternehmenPropertiesParser::class.java.getResourceAsStream(file)
    }

    // Returns options from external directories if present, otherwise falls back to bundled defaults.
    private fun getPromptOptions(folderOption: UnternehmenResourceFolderOption): List<String> {
        val externalOptions = ExternalConfigLoader.listExternalOptions(folderOption.folder)
        if (externalOptions.isNotEmpty()) return externalOptions
        // Fallback to bundled defaults
        return if (folderOption == KUNDEN) listOf("test-kunde")
        else listOf("test-unternehmen")
    }
}
