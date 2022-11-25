package at.sellemond.software.rechnung_generator_spring.prompt

import UnternehmenPropertiesParser
import at.sellemond.software.rechnung_generator_spring.prompt.UnternehmenResourceFolderOption.*
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
        println("Für welchen Kunden soll die Rechnung generiert werden?")
        val kundenProperties = getPropertiesFromPrompt(KUNDEN)

        println("Für welches Unternehmen soll die Rechnung generiert werden?")
        val unternehmenProperties = getPropertiesFromPrompt(UNTERNEHMEN)

        return UnternehmenPromptResult(kundenProperties, unternehmenProperties)
    }

    private fun getPropertiesFromPrompt(folderOption: UnternehmenResourceFolderOption): Properties {
        val options = getPromptOptions(folderOption)
        if (options.size == 1) {
            val properties = Properties()
            properties.load(getPropertiesFromPromptSelection(options, 0, folderOption))
            println("${options[0]} wird automatisch ausgewählt (keine andere Auswahl verfügbar).")

            return properties
        }

        options.forEachIndexed { index, s -> println("${index}: $s") }

        val selection = readLine()!!.toInt()
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
        val file = "${resourceFolder.folder}/${selectionList[selectionIndex]}.properties"
        return UnternehmenPropertiesParser::class.java.getResourceAsStream(file)
    }

    // can be extended if more kunden arise
    // 🧨 has to be named like the corresponding properties file in /resources/kunden
    private fun getPromptOptions(folderOption: UnternehmenResourceFolderOption) =
        if (folderOption == KUNDEN) listOf("softwareXperts")
        else listOf("sellemond")
}