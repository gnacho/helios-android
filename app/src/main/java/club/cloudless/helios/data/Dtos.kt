package club.cloudless.helios.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shape of `GET /api/solar/live` as the Go backend marshals [solar.LiveData],
 * with the `connected` flag merged into the root object by the handler.
 *
 * Only the fields the widget uses are declared, and unknown keys are ignored,
 * so adding fields on the server does not break the app.
 */
@Serializable
data class LiveDto(
    val connected: Boolean = false,
    val production: Double = 0.0,
    val consumption: Double? = null,
    val soc: Double = 0.0,
    @SerialName("batteryPower") val batteryPower: Double = 0.0,
    @SerialName("batteryStatus") val batteryStatus: String = "",
    val grid: Double? = null,
    @SerialName("inverterOnline") val inverterOnline: Boolean = false,
    val ts: String = "",
)

/**
 * Shape of `GET /api/extensions`. `chargerActive` is what the dashboard uses to
 * decide whether to show the charger card at all.
 */
@Serializable
data class ExtensionsDto(
    val enabled: Boolean = false,
    val chargerActive: Boolean = false,
    val bydActive: Boolean = false,
)

/** What the widget renders. Built from one HTTP round trip plus the charger call. */
@Serializable
data class WidgetSnapshot(
    val soc: Double,
    val productionKw: Double,
    val consumptionKw: Double?,
    val batteryPower: Double,
    val chargerActive: Boolean?,
    val connected: Boolean,
    val fetchedAtMillis: Long,
)
