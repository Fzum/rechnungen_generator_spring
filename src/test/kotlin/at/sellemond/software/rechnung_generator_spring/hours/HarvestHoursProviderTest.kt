package at.sellemond.software.rechnung_generator_spring.hours

import at.sellemond.software.rechnung_generator_spring.harvest.HarvestException
import at.sellemond.software.rechnung_generator_spring.harvest.HarvestProperties
import at.sellemond.software.rechnung_generator_spring.harvest.HarvestTimeReportClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Properties

class HarvestHoursProviderTest {

    private fun customer(projectId: String?): Properties = Properties().apply {
        if (projectId != null) setProperty("harvest-project-id", projectId)
    }

    private fun clientReturning(expected: BigDecimal) =
        object : HarvestTimeReportClient(HarvestProperties("acc", "tok", "url")) {
            override fun billableHours(projectId: Long, from: LocalDate, to: LocalDate): BigDecimal {
                assertThat(projectId).isEqualTo(123L)
                assertThat(from).isEqualTo(LocalDate.of(2026, 5, 1))
                assertThat(to).isEqualTo(LocalDate.of(2026, 5, 31))
                return expected
            }
        }

    @Test
    fun `fetches billable hours for the mapped project and month`() {
        val hours = HarvestHoursProvider(clientReturning(BigDecimal("142.50")))
            .fetch(2026, 5, customer("123"))
        assertThat(hours).isEqualByComparingTo("142.50")
    }

    @Test
    fun `fails when customer has no harvest-project-id`() {
        assertThatThrownBy {
            HarvestHoursProvider(clientReturning(BigDecimal.ZERO)).fetch(2026, 5, customer(null))
        }.isInstanceOfSatisfying(HarvestException::class.java) {
            assertThat(it.code).isEqualTo("UNMAPPED_HARVEST_PROJECT")
        }
    }

    @Test
    fun `extracts billable hours from a multi-project report`() {
        val json = """{"results":[{"project_id":111,"billable_hours":10.0},""" +
            """{"project_id":123,"total_hours":200.0,"billable_hours":142.5}]}"""
        assertThat(HarvestTimeReportClient.extractBillableHours(json, 123L)).isEqualByComparingTo("142.5")
    }

    @Test
    fun `extraction fails when the project is absent`() {
        val json = """{"results":[{"project_id":111,"billable_hours":10.0}]}"""
        assertThatThrownBy { HarvestTimeReportClient.extractBillableHours(json, 999L) }
            .isInstanceOf(HarvestException::class.java)
    }
}
