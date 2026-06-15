package at.sellemond.software.rechnung_generator_spring.termination.email

import model.Unternehmen
import java.nio.file.Path

interface EmailInitiator {
    fun initiateEmail(kunde: Unternehmen, zeitraum: String, attachments: List<Path>)
}
