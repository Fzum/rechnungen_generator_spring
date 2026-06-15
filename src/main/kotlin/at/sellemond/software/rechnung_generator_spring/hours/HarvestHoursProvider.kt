package at.sellemond.software.rechnung_generator_spring.hours

import at.sellemond.software.rechnung_generator_spring.harvest.HarvestException
import at.sellemond.software.rechnung_generator_spring.harvest.HarvestTimeReportClient
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.Properties

/** Fetches billable hours for the customer's configured Harvest project over the invoice month. */
@Component
class HarvestHoursProvider(private val client: HarvestTimeReportClient) : HoursProvider {

    override fun fetch(year: Int, month: Int, customer: Properties): BigDecimal {
        val projectId = customer.getProperty("harvest-project-id")?.trim()?.toLongOrNull()
            ?: throw HarvestException(
                "UNMAPPED_HARVEST_PROJECT",
                "customer has no harvest-project-id; cannot fetch hours from Harvest"
            )
        val from = LocalDate.of(year, month, 1)
        val to = from.with(lastDayOfMonth())
        return client.billableHours(projectId, from, to)
    }
}
