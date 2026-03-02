package at.sellemond.software.rechnung_generator_spring.upload

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class NoOpDocumentUploaderTest {

    @Test
    fun `upload does not throw and does nothing`(@TempDir tempDir: Path) {
        val pdfFile = File(tempDir.toFile(), "rechnung.pdf").also { it.writeText("data") }
        // should complete without exception and without modifying anything
        NoOpDocumentUploader().upload(pdfFile.absolutePath)
    }

    @Test
    fun `upload does not throw for non-existent path`() {
        NoOpDocumentUploader().upload("/nonexistent/path/rechnung.pdf")
    }
}
