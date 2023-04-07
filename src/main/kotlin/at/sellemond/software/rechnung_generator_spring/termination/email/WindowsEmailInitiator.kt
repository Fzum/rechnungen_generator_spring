package at.sellemond.software.rechnung_generator_spring.termination.email

import model.Unternehmen
import org.springframework.stereotype.Component

@Component
class WindowsEmailInitiator : EmailInitiator {
    override fun initiateEmail(kunde: Unternehmen, zeitraum: String) {
        TODO("Not yet implemented")
    }
}