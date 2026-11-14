package club.cloudless.helios.data

/** Turns the API payloads into the values the widget shows. */
object SnapshotMapper {

    fun from(live: LiveDto, extensions: ExtensionsDto?, nowMillis: Long): WidgetSnapshot =
        WidgetSnapshot(
            soc = live.soc,
            productionKw = live.production,
            consumptionKw = live.consumption,
            batteryPower = live.batteryPower,
            // `chargerActive` is already the server's verdict (extensions on, car
            // charger on, at least one charger configured). When the extension is
            // missing or off there is no charger to report, so null.
            chargerActive = extensions?.takeIf { it.enabled }?.chargerActive,
            connected = live.connected,
            fetchedAtMillis = nowMillis,
        )
}
