package at.sellemond.software.rechnung_generator_spring.upload

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.nio.file.Paths

/**
 * [DocumentUploader] that uploads the generated PDF to an FTP server.
 *
 * Configuration (`application.yaml`):
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
 *
 * Requires the `commons-net` library (included via `pom.xml`).
 */
class FtpDocumentUploader(private val properties: UploadProperties.FtpProperties) : DocumentUploader {
    private val log = LoggerFactory.getLogger(FtpDocumentUploader::class.java)

    override fun upload(pdfPath: String) {
        require(properties.host.isNotBlank()) { "upload.ftp.host must be set when upload.target=ftp" }

        val client = FTPClient()
        try {
            client.connect(properties.host, properties.port)
            client.login(properties.username, properties.password)
            client.setFileType(FTP.BINARY_FILE_TYPE)
            client.enterLocalPassiveMode()

            val fileName = Paths.get(pdfPath).fileName.toString()
            val remotePath = properties.remotePath.trimEnd('/') + "/$fileName"

            FileInputStream(pdfPath).use { input ->
                val success = client.storeFile(remotePath, input)
                if (!success) {
                    throw IllegalStateException(
                        "FTP upload failed for '$pdfPath' → '$remotePath'. Reply: ${client.replyString}"
                    )
                }
            }
            log.info("PDF uploaded via FTP to {}:{}{}", properties.host, properties.port, remotePath)
        } finally {
            if (client.isConnected) {
                client.logout()
                client.disconnect()
            }
        }
    }
}
