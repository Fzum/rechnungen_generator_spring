package at.sellemond.software.rechnung_generator_spring.upload

import org.slf4j.LoggerFactory

/**
 * Default no-operation [DocumentUploader] used when `upload.target` is `none` or unrecognised.
 * PDF files are not uploaded anywhere; the application simply continues without uploading.
 */
class NoOpDocumentUploader : DocumentUploader {
    private val log = LoggerFactory.getLogger(NoOpDocumentUploader::class.java)

    override fun upload(pdfPath: String) {
        log.debug("Document upload is disabled (target=none). Skipping upload for: {}", pdfPath)
    }
}
