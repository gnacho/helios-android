package club.cloudless.helios.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import club.cloudless.helios.config.ConfigStore
import club.cloudless.helios.data.ApiResult
import club.cloudless.helios.data.HeliosApi
import club.cloudless.helios.data.SnapshotMapper
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Fetches the live snapshot and stores it for the widget.
 *
 * The widget never does network calls while rendering: Glance has to produce a
 * RemoteViews tree quickly, and a failing HTTP call there would show up as a
 * blank widget. The worker writes either a snapshot or the last error.
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cfg = ConfigStore.create(applicationContext)
        val widgets = HeliosWidget()

        if (!cfg.isConfigured()) {
            cfg.saveError("Falta configurar el servidor y el token")
            widgets.updateAll(applicationContext)
            return Result.success()
        }

        val api = HeliosApi(cfg.baseUrl, cfg.token)

        when (val live = api.live()) {
            is ApiResult.Error -> {
                cfg.saveError(live.message)
                widgets.updateAll(applicationContext)
                // A wrong token will not fix itself, a network error might.
                return if (live.unauthorized) Result.success() else Result.retry()
            }

            is ApiResult.Ok -> {
                val extensions = (api.extensions() as? ApiResult.Ok)?.value
                // Un servidor anterior a 1.2.11 responde 404 aquí: el widget de
                // solar sigue valiendo y el del coche muestra su estado vacío.
                val chargerState = (api.chargerState() as? ApiResult.Ok)?.value
                val snapshot = SnapshotMapper.from(
                    live.value,
                    extensions,
                    chargerState,
                    System.currentTimeMillis(),
                )
                cfg.saveSnapshot(snapshot)
                cfg.clearError()
                widgets.updateAll(applicationContext)
                ChargerWidget().updateAll(applicationContext)
                return Result.success()
            }
        }
    }

    companion object {
        private const val PERIODIC = "helios-widget-refresh"

        /** Android will not run periodic work more often than every 15 minutes. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun refreshNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build())
        }
    }
}

internal val snapshotJson = Json { ignoreUnknownKeys = true }
