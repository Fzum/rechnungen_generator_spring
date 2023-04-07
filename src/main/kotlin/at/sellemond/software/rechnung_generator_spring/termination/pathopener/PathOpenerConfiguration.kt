package at.sellemond.software.rechnung_generator_spring.termination.pathopener

import at.sellemond.software.rechnung_generator_spring.termination.OperatingSystem.*
import at.sellemond.software.rechnung_generator_spring.termination.getOperatingSystem
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PathOpenerConfiguration {

    @Bean
    fun pathOpener() = when (getOperatingSystem()) {
        MAC -> MacPathOpener()
        WINDOWS -> WindowsPathOpener()
        OTHER -> throw IllegalStateException("OS not supported")
    }
}