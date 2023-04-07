package at.sellemond.software.rechnung_generator_spring.email

import model.Unternehmen

interface EmailInitiator {
    fun initiateEmail(kunde: Unternehmen, zeitraum: String)
}