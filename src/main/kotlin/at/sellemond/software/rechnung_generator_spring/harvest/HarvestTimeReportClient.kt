package at.sellemond.software.rechnung_generator_spring.harvest

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Calls Harvest's v2 project time-report and returns the billable hours for a single project in a
 * date range. Parses only the one field it needs (`billable_hours` for the matching `project_id`),
 * so no JSON dependency is required.
 */
@Component
class HarvestTimeReportClient(private val properties: HarvestProperties) {

    private val http: HttpClient = HttpClient.newHttpClient()

    fun billableHours(projectId: Long, from: LocalDate, to: LocalDate): BigDecimal {
        val uri = URI.create(
            "${properties.baseUrl}/reports/time/projects?from=${from.format(DATE)}&to=${to.format(DATE)}"
        )
        val request = HttpRequest.newBuilder(uri)
            .header("Authorization", "Bearer ${properties.accessToken}")
            .header("Harvest-Account-Id", properties.accountId)
            .header("User-Agent", "rechnung-generator (sellemondfabian@gmail.com)")
            .GET()
            .build()

        val response = try {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw HarvestException("HARVEST_FETCH_FAILED", "Harvest request failed: ${e.message}")
        }
        if (response.statusCode() !in 200..299) {
            throw HarvestException("HARVEST_FETCH_FAILED", "Harvest API returned status ${response.statusCode()}")
        }
        return extractBillableHours(response.body(), projectId)
    }

    companion object {
        private val DATE = DateTimeFormatter.ofPattern("yyyyMMdd")

        /** Finds the result row for [projectId] and reads its `billable_hours`. */
        fun extractBillableHours(json: String, projectId: Long): BigDecimal {
            val row = json.split("{")
                .firstOrNull { Regex("\"project_id\"\\s*:\\s*$projectId\\b").containsMatchIn(it) }
                ?: throw HarvestException("HARVEST_FETCH_FAILED", "No project row for harvest-project-id $projectId")
            val match = Regex("\"billable_hours\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").find(row)
                ?: throw HarvestException("HARVEST_FETCH_FAILED", "billable_hours missing for project $projectId")
            return match.groupValues[1].toBigDecimal()
        }
    }
}
