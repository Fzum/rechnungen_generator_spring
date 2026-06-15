package at.sellemond.software.rechnung_generator_spring.harvest

/** Failure fetching hours from Harvest; [code] maps to a machine-readable error + exit code. */
class HarvestException(val code: String, override val message: String) : RuntimeException(message)
