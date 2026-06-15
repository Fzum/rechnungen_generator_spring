package at.sellemond.software.rechnung_generator_spring.hours

import java.math.BigDecimal
import java.util.Properties

/** Source of billed hours for a run. Implementations: manual value or Harvest fetch. */
interface HoursProvider {
    fun fetch(year: Int, month: Int, customer: Properties): BigDecimal
}
