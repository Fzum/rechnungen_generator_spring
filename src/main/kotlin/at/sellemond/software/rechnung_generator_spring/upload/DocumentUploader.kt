package at.sellemond.software.rechnung_generator_spring.upload

/**
 * Plugin interface for uploading generated PDF invoices to a remote or local destination.
 *
 * Implementations are selected at startup via the `upload.target` configuration property.
 * Supported targets: `none`, `local`, `ftp`, `google-drive`.
 */
interface DocumentUploader {
    fun upload(pdfPath: String)
}
