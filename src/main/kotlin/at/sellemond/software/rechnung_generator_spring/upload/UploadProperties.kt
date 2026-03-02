package at.sellemond.software.rechnung_generator_spring.upload

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the document upload plugin system.
 *
 * Set `upload.target` to one of the supported values to activate the corresponding uploader:
 * - `none`         – no upload (default)
 * - `local`        – copy the PDF to a local directory
 * - `ftp`          – upload via FTP
 * - `google-drive` – upload to Google Drive
 *
 * Example `application.yaml`:
 * ```yaml
 * upload:
 *   target: ftp
 *   ftp:
 *     host: ftp.example.com
 *     port: 21
 *     username: myuser
 *     password: secret
 *     remote-path: /invoices/
 * ```
 */
@ConfigurationProperties(prefix = "upload")
data class UploadProperties(
    val target: String = "none",
    val ftp: FtpProperties = FtpProperties(),
    val googleDrive: GoogleDriveProperties = GoogleDriveProperties(),
    val local: LocalProperties = LocalProperties()
) {
    data class FtpProperties(
        val host: String = "",
        val port: Int = 21,
        val username: String = "",
        val password: String = "",
        val remotePath: String = "/"
    )

    data class GoogleDriveProperties(
        val credentialsFile: String = "",
        val folderId: String = ""
    )

    data class LocalProperties(
        val destinationPath: String = ""
    )
}
