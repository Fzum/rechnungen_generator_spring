package at.sellemond.software.rechnung_generator_spring.termination

import at.sellemond.software.rechnung_generator_spring.termination.OperatingSystem.*

enum class OperatingSystem {
    WINDOWS, MAC, OTHER
}

fun getOperatingSystem(): OperatingSystem {
    val osName = System.getProperty("os.name").lowercase()

    return when {
        osName.contains("mac") -> MAC
        osName.contains("windows") -> WINDOWS
        else -> OTHER
    }
}