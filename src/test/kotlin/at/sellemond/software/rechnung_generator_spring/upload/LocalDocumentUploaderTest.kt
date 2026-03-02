package at.sellemond.software.rechnung_generator_spring.upload

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class LocalDocumentUploaderTest {

    @Test
    fun `upload copies pdf file to destination directory`(@TempDir sourceDir: Path, @TempDir destDir: Path) {
        val pdfFile = File(sourceDir.toFile(), "rechnung_test_01_2024.pdf").also { it.writeText("PDF content") }
        val properties = UploadProperties.LocalProperties(destinationPath = destDir.toString())

        LocalDocumentUploader(properties).upload(pdfFile.absolutePath)

        val uploadedFile = File(destDir.toFile(), pdfFile.name)
        assertThat(uploadedFile).exists()
        assertThat(uploadedFile).hasContent("PDF content")
    }

    @Test
    fun `upload creates destination directory if it does not exist`(@TempDir baseDir: Path, @TempDir sourceDir: Path) {
        val pdfFile = File(sourceDir.toFile(), "rechnung.pdf").also { it.writeText("data") }
        val newDestDir = File(baseDir.toFile(), "new/nested/dir")
        val properties = UploadProperties.LocalProperties(destinationPath = newDestDir.absolutePath)

        LocalDocumentUploader(properties).upload(pdfFile.absolutePath)

        assertThat(File(newDestDir, pdfFile.name)).exists()
    }

    @Test
    fun `upload replaces existing file in destination`(@TempDir sourceDir: Path, @TempDir destDir: Path) {
        val pdfFile = File(sourceDir.toFile(), "rechnung.pdf").also { it.writeText("new content") }
        File(destDir.toFile(), "rechnung.pdf").writeText("old content")
        val properties = UploadProperties.LocalProperties(destinationPath = destDir.toString())

        LocalDocumentUploader(properties).upload(pdfFile.absolutePath)

        assertThat(File(destDir.toFile(), "rechnung.pdf")).hasContent("new content")
    }

    @Test
    fun `upload throws when destination path is blank`(@TempDir sourceDir: Path) {
        val pdfFile = File(sourceDir.toFile(), "rechnung.pdf").also { it.writeText("data") }
        val properties = UploadProperties.LocalProperties(destinationPath = "")

        assertThatThrownBy { LocalDocumentUploader(properties).upload(pdfFile.absolutePath) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("upload.local.destination-path")
    }
}
