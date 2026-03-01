package at.sellemond.software.rechnung_generator_spring

import java.io.File
import java.io.InputStream

object ExternalConfigLoader {

    fun listExternalOptions(folder: String): List<String> {
        val dir = File(folder)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles { f -> f.isFile && f.extension == "properties" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }

    fun loadExternalProperties(folder: String, name: String): InputStream? {
        val file = File("$folder/$name.properties")
        return if (file.exists() && file.isFile) file.inputStream() else null
    }
}
