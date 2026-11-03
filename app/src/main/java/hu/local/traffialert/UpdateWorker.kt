package hu.local.traffialert

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class UpdateWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        return try {
            val list = Scraper.download()
            val old = CameraStore.count(ctx)
            // védelem: ha az oldal szerkezete változott, ne írjuk felül a jó listát
            if (list.size < MIN_COUNT || (old > 0 && list.size < old * 0.7)) {
                CameraStore.setStatus(ctx, "Gyanús letöltés (${list.size} sor), a régi lista marad")
                Result.failure()
            } else {
                val changed = CameraStore.saveIfChanged(ctx, list)
                CameraStore.setStatus(
                    ctx,
                    if (changed) "Lista frissítve: ${list.size} mérő" else "Nincs változás (${list.size} mérő)"
                )
                Result.success()
            }
        } catch (e: Exception) {
            CameraStore.setStatus(ctx, "Letöltési hiba: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val MIN_COUNT = 150
        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Naponta egyszer fut; ha már be van ütemezve, nem nyúl hozzá. */
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(online).build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork("cam-update-daily", ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun runNow(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<UpdateWorker>().setConstraints(online).build()
            WorkManager.getInstance(ctx)
                .enqueueUniqueWork("cam-update-now", ExistingWorkPolicy.REPLACE, req)
        }
    }
}
