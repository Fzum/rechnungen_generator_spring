package at.sellemond.software.rechnung_generator_spring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class ExternalConfigLoaderTest {

    @Test
    internal fun `listExternalOptions returns empty list when directory does not exist`() {
        val result = ExternalConfigLoader.listExternalOptions("/nonexistent/directory/kunden")
        assertThat(result).isEmpty()
    }

    @Test
    internal fun `listExternalOptions returns sorted list of properties file names`(@TempDir tempDir: Path) {
        File(tempDir.toFile(), "acme.properties").writeText("name=Acme Corp")
        File(tempDir.toFile(), "beta.properties").writeText("name=Beta GmbH")
        File(tempDir.toFile(), "notes.txt").writeText("ignore me")

        val result = ExternalConfigLoader.listExternalOptions(tempDir.toString())

        assertThat(result).containsExactly("acme", "beta")
    }

    @Test
    internal fun `loadExternalProperties returns null when file does not exist`(@TempDir tempDir: Path) {
        val result = ExternalConfigLoader.loadExternalProperties(tempDir.toString(), "missing")
        assertThat(result).isNull()
    }

    @Test
    internal fun `loadExternalProperties returns input stream for existing file`(@TempDir tempDir: Path) {
        File(tempDir.toFile(), "kunde.properties").writeText("name=Test GmbH")

        val result = ExternalConfigLoader.loadExternalProperties(tempDir.toString(), "kunde")

        assertThat(result).isNotNull()
        result!!.use { stream ->
            val content = stream.bufferedReader().readText()
            assertThat(content).contains("Test GmbH")
        }
    }
}
