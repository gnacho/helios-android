package club.cloudless.helios

import club.cloudless.helios.data.ExtensionsDto
import club.cloudless.helios.data.HeliosApi
import club.cloudless.helios.data.LiveDto
import club.cloudless.helios.data.SnapshotMapper
import club.cloudless.helios.widget.WidgetText
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveDtoTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    @Test
    fun `parses a full payload and ignores unknown keys`() {
        val raw = """
            {
              "connected": true,
              "production": 3.214,
              "consumption": 1.2,
              "respaldoKw": 0.4,
              "noRespaldadaKw": 0.0,
              "batteryPower": -0.86,
              "batteryStatus": "Descargando",
              "soc": 82.4,
              "grid": -0.15,
              "solis": 2.1,
              "fox": 1.1,
              "inverters": [{"key": "solis", "power": 2.1}],
              "at": 10,
              "alerts": [{"severity": "info", "text": "todo bien"}],
              "sun": {"sunrise": "08:00", "sunset": "18:00"},
              "weather": "sunny",
              "weatherTemp": 12.5,
              "station": "Mandor",
              "inverterOnline": true,
              "ts": "2026-11-14T10:35:00+01:00",
              "brandNewField": {"nested": [1, 2, 3]}
            }
        """.trimIndent()

        val live = json.decodeFromString(LiveDto.serializer(), raw)

        assertEquals(3.214, live.production, 1e-9)
        assertEquals(82.4, live.soc, 1e-9)
        assertEquals(-0.86, live.batteryPower, 1e-9)
        assertEquals(true, live.connected)
        assertEquals(1.2, live.consumption!!, 1e-9)
    }

    @Test
    fun `null consumption and grid survive the parse`() {
        val raw = """{"connected": false, "production": 0, "soc": 0, "consumption": null, "grid": null}"""
        val live = json.decodeFromString(LiveDto.serializer(), raw)
        assertNull(live.consumption)
        assertNull(live.grid)
    }

    @Test
    fun `missing fields fall back to defaults instead of throwing`() {
        val live = json.decodeFromString(LiveDto.serializer(), "{}")
        assertEquals(0.0, live.production, 1e-9)
        assertEquals(0.0, live.soc, 1e-9)
        assertNull(live.consumption)
    }
}

class WidgetTextTest {

    @Test
    fun `kw uses a decimal comma and two decimals`() {
        assertEquals("3,21 kW", WidgetText.kw(3.214))
        assertEquals("0,00 kW", WidgetText.kw(0.0))
        assertEquals("sin dato", WidgetText.kw(null))
    }

    @Test
    fun `soc is rounded without decimals`() {
        assertEquals("82 %", WidgetText.soc(82.4))
        assertEquals("83 %", WidgetText.soc(82.6))
    }

    @Test
    fun `battery label follows the same 0,05 kW threshold as the web UI`() {
        assertEquals("Cargando", WidgetText.batteryLabel(0.051))
        assertEquals("Descargando", WidgetText.batteryLabel(-0.051))
        assertEquals("En reposo", WidgetText.batteryLabel(0.05))
        assertEquals("En reposo", WidgetText.batteryLabel(0.0))
    }

    @Test
    fun `freshness degrades from now to minutes to hours`() {
        val now = 1_000_000_000_000L
        assertEquals("sin datos", WidgetText.freshness(0L, now))
        assertEquals("ahora mismo", WidgetText.freshness(now - 20_000, now))
        assertEquals("hace 5 min", WidgetText.freshness(now - 5 * 60_000, now))
        assertEquals("hace 2 h", WidgetText.freshness(now - 2 * 3_600_000, now))
    }
}

class SnapshotMapperTest {

    @Test
    fun `charger is reported only when the extension is enabled`() {
        val live = LiveDto(soc = 50.0, production = 1.0, batteryPower = 0.2, connected = true)

        val enabledAndOn = SnapshotMapper.from(live, ExtensionsDto(enabled = true, chargerActive = true), 1L)
        assertEquals(true, enabledAndOn.chargerActive)

        val enabledAndOff = SnapshotMapper.from(live, ExtensionsDto(enabled = true, chargerActive = false), 1L)
        assertEquals(false, enabledAndOff.chargerActive)

        val disabled = SnapshotMapper.from(live, ExtensionsDto(enabled = false, chargerActive = true), 1L)
        assertNull(disabled.chargerActive)

        val noExtensions = SnapshotMapper.from(live, null, 1L)
        assertNull(noExtensions.chargerActive)
    }
}

class BaseUrlTest {

    @Test
    fun `normalizes what a person types`() {
        assertEquals("https://helios.cloudless.club", HeliosApi.normalizeBaseUrl("helios.cloudless.club"))
        assertEquals("https://helios.cloudless.club", HeliosApi.normalizeBaseUrl("  https://helios.cloudless.club/  "))
        assertEquals("http://192.168.1.226", HeliosApi.normalizeBaseUrl("192.168.1.226"))
        assertEquals("", HeliosApi.normalizeBaseUrl("   "))
    }
}
