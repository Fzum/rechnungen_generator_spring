package at.sellemond.software.rechnung_generator_spring.upload

import org.slf4j.LoggerFactory

/**
 * [DocumentUploader] scaffold for uploading the generated PDF to Google Drive.
 *
 * Configuration (`application.yaml`):
 * ```yaml
 * upload:
 *   target: google-drive
 *   google-drive:
 *     credentials-file: /path/to/service-account.json
 *     folder-id: your-google-drive-folder-id
 * ```
 *
 * **To complete this implementation**, add the Google Drive SDK dependency to `pom.xml`:
 * ```xml
 * <dependency>
 *   <groupId>com.google.apis</groupId>
 *   <artifactId>google-api-services-drive</artifactId>
 *   <version>v3-rev20230822-2.0.0</version>
 * </dependency>
 * <dependency>
 *   <groupId>com.google.auth</groupId>
 *   <artifactId>google-auth-library-oauth2-http</artifactId>
 *   <!-- Use the latest stable version from https://mvnrepository.com/artifact/com.google.auth/google-auth-library-oauth2-http -->
 *   <version>LATEST</version>
 * </dependency>
 * ```
 * Then replace the body of [upload] with the appropriate Drive API calls using
 * a service-account credential loaded from [UploadProperties.GoogleDriveProperties.credentialsFile].
 */
class GoogleDriveDocumentUploader(private val properties: UploadProperties.GoogleDriveProperties) : DocumentUploader {
    private val log = LoggerFactory.getLogger(GoogleDriveDocumentUploader::class.java)

    override fun upload(pdfPath: String) {
        require(properties.credentialsFile.isNotBlank()) {
            "upload.google-drive.credentials-file must be set when upload.target=google-drive"
        }
        require(properties.folderId.isNotBlank()) {
            "upload.google-drive.folder-id must be set when upload.target=google-drive"
        }

        // TODO: Integrate Google Drive Java SDK here.
        // Example flow:
        //   1. Load credentials from properties.credentialsFile (service-account JSON)
        //   2. Build a Drive service client
        //   3. Create a File metadata object with the target folder ID
        //   4. Upload the PDF using drive.files().create(metadata, mediaContent).execute()
        log.warn(
            "Google Drive upload is not yet implemented. PDF '{}' was NOT uploaded to folder '{}'.",
            pdfPath, properties.folderId
        )
        throw UnsupportedOperationException(
            "Google Drive upload is not yet implemented. See GoogleDriveDocumentUploader for instructions."
        )
    }
}
