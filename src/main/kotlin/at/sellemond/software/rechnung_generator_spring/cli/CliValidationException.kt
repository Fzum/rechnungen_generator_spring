package at.sellemond.software.rechnung_generator_spring.cli

/** Raised for any input/validation failure; [code] maps to a machine-readable error + exit code. */
class CliValidationException(val code: String, override val message: String) : RuntimeException(message)
