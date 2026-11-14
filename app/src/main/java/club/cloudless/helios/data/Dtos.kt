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

/** One charger from `GET /api/charger/state` (Helios 1.2.11+). */
@Serializable
data class ChargerDto(
    val key: String = "",
    val name: String = "",
    val charging: Boolean = false,
    val connected: Boolean = false,
    val state: String? = null,
    @SerialName("powerKw") val powerKw: Double = 0.0,
    @SerialName("powerKnown") val powerKnown: Boolean = false,
    @SerialName("deviceOffline") val deviceOffline: Boolean = false,
    @SerialName("sessionKwh") val sessionKwh: Double? = null,
    @SerialName("totalKwh") val totalKwh: Double? = null,
    @SerialName("switchOn") val switchOn: Boolean? = null,
)

/** Shape of `GET /api/charger/state`. Empty list means no charger configured. */
@Serializable
data class ChargerStateDto(
    val active: Boolean = false,
    val chargers: List<ChargerDto> = emptyList(),
)

/** What the widgets render. Built from one refresh, stored as is. */
@Serializable
data class WidgetSnapshot(
    val soc: Double,
    val productionKw: Double,
    val consumptionKw: Double?,
    val batteryPower: Double,
    val connected: Boolean,
    val fetchedAtMillis: Long,
    val chargersActive: Boolean = false,
    val chargers: List<ChargerDto> = emptyList(),
) {
    /** A home screen widget is not a dashboard: the first charger is enough. */
    val mainCharger: ChargerDto? get() = chargers.firstOrNull()
}
