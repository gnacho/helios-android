package club.cloudless.helios.widget

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatting and state derivation, kept free of Android APIs so it can be
 * tested on the JVM (the widget and the settings screen share it).
 */
object WidgetText {

    private val locale = Locale("es", "ES")

    fun kw(value: Double?): String {
        if (value == null) return "sin dato"
        return String.format(locale, "%.2f kW", value)
    }

    fun soc(value: Double): String = String.format(locale, "%.0f %%", value)

    /** Sign of the battery power, the same threshold the web UI uses (0.05 kW). */
    fun batteryLabel(power: Double): String = when {
        power > 0.05 -> "Cargando"
        power < -0.05 -> "Descargando"
        else -> "En reposo"
    }

    fun freshness(fetchedAtMillis: Long, nowMillis: Long): String {
        if (fetchedAtMillis <= 0L) return "sin datos"
        val diff = (nowMillis - fetchedAtMillis).coerceAtLeast(0L)
        val minutes = diff / 60_000
        return when {
            minutes < 1 -> "ahora mismo"
            minutes < 60 -> "hace $minutes min"
            minutes < 60 * 24 -> "hace ${minutes / 60} h"
            else -> SimpleDateFormat("d MMM HH:mm", locale).format(Date(fetchedAtMillis))
        }
    }

    fun clock(millis: Long): String =
        SimpleDateFormat("HH:mm", locale).format(Date(millis))

    /** One line for the main widget: what the car charger is doing right now. */
    fun chargerLine(charging: Boolean, connected: Boolean, powerKw: Double, powerKnown: Boolean, offline: Boolean): String =
        when {
            offline -> "sin conexión"
            charging && powerKnown -> "cargando · " + kw(powerKw)
            charging -> "cargando"
            connected -> "conectado"
            else -> "en reposo"
        }

    /** Session energy, only when the server actually has the number. */
    fun kwh(value: Double?): String =
        if (value == null) "sin dato" else String.format(locale, "%.2f kWh", value)
}
