package club.cloudless.helios.data

/** Turns the API payloads into the values the widgets show. */
object SnapshotMapper {

    fun from(
        live: LiveDto,
        extensions: ExtensionsDto?,
        chargerState: ChargerStateDto?,
        nowMillis: Long,
    ): WidgetSnapshot {
        val active = chargerState?.active == true && chargerState.chargers.isNotEmpty()
        return WidgetSnapshot(
            soc = live.soc,
            productionKw = live.production,
            consumptionKw = live.consumption,
            batteryPower = live.batteryPower,
            connected = live.connected,
            fetchedAtMillis = nowMillis,
            chargersActive = active,
            chargers = if (active) chargerState.chargers else emptyList(),
        )
    }
}
