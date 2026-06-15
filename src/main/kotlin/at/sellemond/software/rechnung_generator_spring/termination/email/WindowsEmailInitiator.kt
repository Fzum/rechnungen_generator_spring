package at.sellemond.software.rechnung_generator_spring.termination.email

import model.Unternehmen
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class WindowsEmailInitiator : EmailInitiator {
    override fun initiateEmail(kunde: Unternehmen, zeitraum: String, attachments: List<Path>) {
        TODO("Not yet implemented")
    }
}
