package club.cloudless.helios.widget

import android.content.Context
import android.content.Intent
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import club.cloudless.helios.config.ConfigStore
import club.cloudless.helios.data.WidgetSnapshot
import club.cloudless.helios.ui.MainActivity

class HeliosWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cfg = ConfigStore.create(context)
        val snapshot = cfg.snapshot()
        val error = cfg.error()
        val openIntent = Intent(context, MainActivity::class.java)
        val colors = widgetColors()

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(colors.surface)
                    .padding(12.dp)
                    .clickable(actionStartActivity(openIntent)),
            ) {
                when {
                    error.isNotEmpty() -> Message(error, colors)
                    snapshot == null -> Message("Sin datos todavía. Abre la app para configurar.", colors)
                    else -> Snapshot(snapshot, colors)
                }
            }
        }
    }

    @Composable
    private fun Message(message: String, colors: ColorProviders) {
        Text(
            text = "Helios",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.onSurface),
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(text = message, style = TextStyle(fontSize = 12.sp, color = colors.onSurfaceVariant))
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = "Toca para refrescar",
            style = TextStyle(fontSize = 11.sp, color = colors.primary),
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
        )
    }

    @Composable
    private fun Snapshot(snapshot: WidgetSnapshot, colors: ColorProviders) {
        Row {
            Text(
                text = WidgetText.soc(snapshot.soc),
                style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.primary),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = WidgetText.batteryLabel(snapshot.batteryPower),
                style = TextStyle(fontSize = 12.sp, color = colors.onSurfaceVariant),
            )
        }
        Spacer(GlanceModifier.height(10.dp))
        Metric("Producción", WidgetText.kw(snapshot.productionKw), colors)
        Spacer(GlanceModifier.height(4.dp))
        Metric("Consumo", WidgetText.kw(snapshot.consumptionKw), colors)
        snapshot.mainCharger?.let { chg ->
            Spacer(GlanceModifier.height(4.dp))
            Metric(
                chg.name.ifBlank { "Coche" },
                WidgetText.chargerLine(chg.charging, chg.connected, chg.powerKw, chg.powerKnown, chg.deviceOffline),
                colors,
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = "Actualizado " + WidgetText.freshness(snapshot.fetchedAtMillis, System.currentTimeMillis()),
            style = TextStyle(fontSize = 11.sp, color = colors.onSurfaceVariant),
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
        )
    }

    @Composable
    private fun Metric(label: String, value: String, colors: ColorProviders) {
        Row {
            Text(text = label, style = TextStyle(fontSize = 12.sp, color = colors.onSurfaceVariant))
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = value,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onSurface),
            )
        }
    }
}

class HeliosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HeliosWidget()
}

/** Amber accent on a warm surface, matching the web app's brand. */
private fun widgetColors(): ColorProviders = androidx.glance.material3.ColorProviders(
    light = lightColorScheme(
        primary = Color(0xFFB26A00),
        surface = Color(0xFFFFFBF5),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFF0A020),
        surface = Color(0xFF1C1B1F),
    ),
)
