package hu.local.traffialert

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlin.math.abs

class AlertService : Service() {

    private lateinit var client: FusedLocationProviderClient
    private var cams: List<Camera> = emptyList()
    private var camsVersion = -1L
    private val stage = HashMap<Camera, Int>() // 1 = 5 km-es jelzés elhangzott, 2 = 1 km-es is

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { handle(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_STATUS, "Figyelés", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Traffipax riasztás", NotificationManager.IMPORTANCE_HIGH)
        )
        val n = NotificationCompat.Builder(this, CH_STATUS)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Traffipax figyelés fut")
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf(); return
        }
        client = LocationServices.getFusedLocationProviderClient(this)
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1000L).build()
        client.requestLocationUpdates(req, callback, Looper.getMainLooper())
    }

    override fun onDestroy() {
        if (::client.isInitialized) client.removeLocationUpdates(callback)
        super.onDestroy()
    }

    private fun handle(loc: Location) {
        val v = CameraStore.version(this)
        if (v != camsVersion) { cams = CameraStore.load(this); camsVersion = v }

        // irányt csak mozgás közben ad a GPS
        if (!loc.hasSpeed() || !loc.hasBearing() || loc.speed < MIN_SPEED) return

        val res = FloatArray(3)
        var best: Camera? = null
        var bestDist = Float.MAX_VALUE

        for (c in cams) {
            Location.distanceBetween(loc.latitude, loc.longitude, c.lat, c.lon, res)
            val d = res[0]
            if (d > RESET_M) { stage.remove(c); continue }
            if (d > FAR_M) continue

            // res[1]: induló irány a mérő felé (-180..180); csak az előttünk lévőt jelezzük
            val diff = abs(((res[1] - loc.bearing + 540f) % 360f) - 180f)
            if (diff > MAX_ANGLE) continue

            val s = stage[c] ?: 0
            var fire = false
            if (d <= NEAR_M && s < 2) { stage[c] = 2; fire = true }
            else if (d <= FAR_M && s < 1 && loc.speed >= MIN_SPEED_FAR) { stage[c] = 1; fire = true }

            if (fire && d < bestDist) { best = c; bestDist = d }
        }
        best?.let { alert(it, bestDist) }
    }

    private fun alert(c: Camera, d: Float) {
        val dist = if (d >= 1000f) "%.1f km".format(d / 1000f) else "${(d / 50f).toInt() * 50} m"
        val n = NotificationCompat.Builder(this, CH_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Traffipax $dist-re")
            .setContentText(c.label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000)
            .build()
        getSystemService(NotificationManager::class.java).notify(2, n)
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
    }

    companion object {
        const val CH_STATUS = "status"
        const val CH_ALERT = "alert"

        const val FAR_M = 5000f          // első jelzés
        const val NEAR_M = 1000f         // második jelzés
        const val RESET_M = 6000f        // ennél messzebb a mérő "elfelejtődik"
        const val MAX_ANGLE = 45f        // ennyivel térhet el a haladási iránytól
        const val MIN_SPEED = 4f         // m/s (~15 km/h) alatt nincs jelzés
        const val MIN_SPEED_FAR = 14f    // m/s (~50 km/h) alatt az 5 km-es jelzés kimarad
    }
}
