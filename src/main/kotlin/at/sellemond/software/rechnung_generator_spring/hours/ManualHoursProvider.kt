package at.sellemond.software.rechnung_generator_spring.hours

import java.math.BigDecimal
import java.util.Properties

/** Returns the hours supplied directly on the command line. */
class ManualHoursProvider(private val hours: BigDecimal) : HoursProvider {
    override fun fetch(year: Int, month: Int, customer: Properties): BigDecimal = hours
}
