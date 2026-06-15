package at.sellemond.software.rechnung_generator_spring.harvest

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Harvest credentials. Defaults read the canonical environment variables directly so they bind even
 * without relaxed-binding config; values may still be overridden via `harvest.*` in application.yaml.
 * Never hardcode or commit real credentials (Constitution I).
 */
@ConfigurationProperties(prefix = "harvest")
data class HarvestProperties(
    val accountId: String = System.getenv("HARVEST_ACCOUNT_ID") ?: "",
    val accessToken: String = System.getenv("HARVEST_ACCESS_TOKEN") ?: "",
    val baseUrl: String = "https://api.harvestapp.com/v2",
)
