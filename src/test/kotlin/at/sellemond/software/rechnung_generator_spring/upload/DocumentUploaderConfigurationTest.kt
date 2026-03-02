package at.sellemond.software.rechnung_generator_spring.upload

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class DocumentUploaderConfigurationTest {

    private val configuration = DocumentUploaderConfiguration()

    @Test
    fun `given target none then NoOpDocumentUploader is created`() {
        val properties = UploadProperties(target = "none")
        val uploader = configuration.documentUploader(properties)
        assertThat(uploader).isInstanceOf(NoOpDocumentUploader::class.java)
    }

    @Test
    fun `given target empty string then NoOpDocumentUploader is created`() {
        val properties = UploadProperties(target = "")
        val uploader = configuration.documentUploader(properties)
        assertThat(uploader).isInstanceOf(NoOpDocumentUploader::class.java)
    }

    @Test
    fun `given unknown target then NoOpDocumentUploader is created`() {
        val properties = UploadProperties(target = "sftp")
        val uploader = configuration.documentUploader(properties)
        assertThat(uploader).isInstanceOf(NoOpDocumentUploader::class.java)
    }

    @Test
    fun `given target local then LocalDocumentUploader is created`() {
        val properties = UploadProperties(
            target = "local",
            local = UploadProperties.LocalProperties(destinationPath = "/tmp/invoices")
        )
        val uploader = configuration.documentUploader(properties)
        assertThat(uploader).isInstanceOf(LocalDocumentUploader::class.java)
    }

    @Test
    fun `given target ftp then FtpDocumentUploader is created`() {
        val properties = UploadProperties(
            target = "ftp",
            ftp = UploadProperties.FtpProperties(host = "ftp.example.com")
        )
        val uploader = configuration.documentUploader(properties)
        assertThat(uploader).isInstanceOf(FtpDocumentUploader::class.java)
    }

    @Test
    fun `given target google-drive then GoogleDriveDocumentUploader is created`() {
        val properties = UploadProperties(
            target = "google-drive",
            googleDrive = UploadProperties.GoogleDriveProperties(
                credentialsFile = "/path/to/creds.json",
                folderId = "abc123"
            )
        )
        val uploader = configuration.documentUploader(properties)
        assertThat(uploader).isInstanceOf(GoogleDriveDocumentUploader::class.java)
    }

    @Test
    fun `given target with mixed case then correct uploader is created`() {
        val properties = UploadProperties(target = "LOCAL")
        val uploader = configuration.documentUploader(properties)
        assertThat(uploader).isInstanceOf(LocalDocumentUploader::class.java)
    }
}
