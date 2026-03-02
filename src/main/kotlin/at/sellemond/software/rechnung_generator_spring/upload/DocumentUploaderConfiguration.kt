package at.sellemond.software.rechnung_generator_spring.upload

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Factory configuration that selects and creates the appropriate [DocumentUploader] bean
 * based on the `upload.target` property.
 *
 * Supported targets:
 * - `none`         → [NoOpDocumentUploader] (default)
 * - `local`        → [LocalDocumentUploader]
 * - `ftp`          → [FtpDocumentUploader]
 * - `google-drive` → [GoogleDriveDocumentUploader]
 */
@Configuration
class DocumentUploaderConfiguration {
    private val log = LoggerFactory.getLogger(DocumentUploaderConfiguration::class.java)

    @Bean
    fun documentUploader(uploadProperties: UploadProperties): DocumentUploader {
        val target = uploadProperties.target.lowercase().trim()
        return when (target) {
            "local" -> {
                log.info("Document upload target: local ({})", uploadProperties.local.destinationPath)
                LocalDocumentUploader(uploadProperties.local)
            }
            "ftp" -> {
                log.info("Document upload target: FTP ({}:{})", uploadProperties.ftp.host, uploadProperties.ftp.port)
                FtpDocumentUploader(uploadProperties.ftp)
            }
            "google-drive" -> {
                log.info("Document upload target: Google Drive (folder={})", uploadProperties.googleDrive.folderId)
                GoogleDriveDocumentUploader(uploadProperties.googleDrive)
            }
            "none" -> {
                log.info("Document upload is disabled (target=none)")
                NoOpDocumentUploader()
            }
            else -> {
                log.warn("Unknown upload target '{}'. Falling back to no-op uploader.", target)
                NoOpDocumentUploader()
            }
        }
    }
}
