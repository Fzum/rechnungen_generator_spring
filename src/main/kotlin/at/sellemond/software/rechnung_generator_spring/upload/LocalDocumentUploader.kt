package at.sellemond.software.rechnung_generator_spring.upload

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * [DocumentUploader] that copies the generated PDF to a local directory.
 *
 * Configuration (`application.yaml`):
 * ```yaml
 * upload:
 *   target: local
 *   local:
 *     destination-path: /path/to/backup/folder
 * ```
 */
class LocalDocumentUploader(private val properties: UploadProperties.LocalProperties) : DocumentUploader {
    private val log = LoggerFactory.getLogger(LocalDocumentUploader::class.java)

    override fun upload(pdfPath: String) {
        require(properties.destinationPath.isNotBlank()) {
            "upload.local.destination-path must be set when upload.target=local"
        }

        val source = Paths.get(pdfPath)
        val destinationDir = File(properties.destinationPath).also { it.mkdirs() }
        val destination = Paths.get(destinationDir.absolutePath, source.fileName.toString())

        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
        log.info("PDF uploaded (copied) to local path: {}", destination)
    }
}
