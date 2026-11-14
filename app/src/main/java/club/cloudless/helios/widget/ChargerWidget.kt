package club.cloudless.helios.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import club.cloudless.helios.config.ConfigStore
import club.cloudless.helios.data.ChargerDto
import club.cloudless.helios.ui.MainActivity

/**
 * One charger, big. Reads the same stored snapshot as the solar widget, so both
 * stay consistent and a single refresh feeds them.
 */
class ChargerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cfg = ConfigStore.create(context)
        val snapshot = cfg.snapshot()
        val error = cfg.error()
        val charger = snapshot?.mainCharger
        val openIntent = Intent(context, MainActivity::class.java)

        provideContent {
            val colors = chargerColors()
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(colors.surface)
                    .padding(12.dp)
                    .clickable(actionStartActivity(openIntent)),
            ) {
                when {
                    error.isNotEmpty() -> Message(error, colors)
                    snapshot == null -> Message("Sin datos todavía. Abre la app.", colors)
                    !snapshot.chargersActive || charger == null -> Message("No hay cargador configurado", colors)
                    else -> Body(charger, snapshot.fetchedAtMillis, colors)
                }
            }
        }
    }

    @Composable
    private fun Message(message: String, colors: ColorProviders) {
        Text(
            text = "Coche",
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onSurface),
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(text = message, style = TextStyle(fontSize = 12.sp, color = colors.onSurfaceVariant))
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = "Toca para refrescar",
            style = TextStyle(fontSize = 11.sp, color = colors.primary),
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
        )
    }

    @Composable
    private fun Body(charger: ChargerDto, fetchedAtMillis: Long, colors: ColorProviders) {
        Text(
            text = charger.name.ifBlank { "Coche" },
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onSurface),
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = WidgetText.chargerLine(
                charger.charging,
                charger.connected,
                charger.powerKw,
                charger.powerKnown,
                charger.deviceOffline,
            ),
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (charger.charging) colors.primary else colors.onSurfaceVariant,
            ),
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = "Sesión " + WidgetText.kwh(charger.sessionKwh),
            style = TextStyle(fontSize = 12.sp, color = colors.onSurfaceVariant),
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = "Actualizado " + WidgetText.freshness(fetchedAtMillis, System.currentTimeMillis()),
            style = TextStyle(fontSize = 11.sp, color = colors.onSurfaceVariant),
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
        )
    }
}

class ChargerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChargerWidget()
}

private fun chargerColors(): ColorProviders = androidx.glance.material3.ColorProviders(
    light = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFFB26A00),
        surface = Color(0xFFFFFBF5),
    ),
    dark = androidx.compose.material3.darkColorScheme(
        primary = Color(0xFFF0A020),
        surface = Color(0xFF1C1B1F),
    ),
)
