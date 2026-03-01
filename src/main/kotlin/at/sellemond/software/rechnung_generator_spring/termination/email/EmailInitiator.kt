package at.sellemond.software.rechnung_generator_spring.termination.email

import model.Unternehmen

interface EmailInitiator {
    fun initiateEmail(kunde: Unternehmen, zeitraum: String)
}