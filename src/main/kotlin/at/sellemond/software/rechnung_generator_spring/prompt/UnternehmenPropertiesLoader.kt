package at.sellemond.software.rechnung_generator_spring.prompt

import at.sellemond.software.rechnung_generator_spring.ExternalConfigLoader
import java.util.Properties

/**
 * Loads kunde/unternehmen [Properties] by short-name, preferring an external directory next to the
 * JAR and falling back to the bundled defaults. Shared by the interactive prompt and the
 * non-interactive CLI so both honour the same external-override semantics.
 */
object UnternehmenPropertiesLoader {

    fun listOptions(folder: String): List<String> {
        val external = ExternalConfigLoader.listExternalOptions(folder)
        if (external.isNotEmpty()) return external
        return if (folder == "kunden") listOf("test-kunde") else listOf("test-unternehmen")
    }

    fun load(folder: String, name: String): Properties {
        val stream = ExternalConfigLoader.loadExternalProperties(folder, name)
            ?: UnternehmenPropertiesLoader::class.java.getResourceAsStream("/$folder/$name.properties")
            ?: throw IllegalArgumentException("No properties found for $folder/$name")
        return Properties().apply { stream.use { load(it) } }
    }
}
