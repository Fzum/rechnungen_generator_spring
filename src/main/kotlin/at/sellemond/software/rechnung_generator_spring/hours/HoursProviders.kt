package at.sellemond.software.rechnung_generator_spring.hours

import at.sellemond.software.rechnung_generator_spring.cli.HoursSource
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Selects the [HoursProvider] for a run. An explicit `--hours` value always wins (manual override);
 * otherwise the source decides. The MANUAL-without-hours case is prevented by the arg parser.
 */
@Component
class HoursProviders(private val harvestHoursProvider: HarvestHoursProvider) {

    fun resolve(source: HoursSource, explicitHours: BigDecimal?): HoursProvider = when {
        explicitHours != null -> ManualHoursProvider(explicitHours)
        source == HoursSource.HARVEST -> harvestHoursProvider
        else -> error("hours must be provided for the manual source")
    }
}
