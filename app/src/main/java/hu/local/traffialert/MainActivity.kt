package hu.local.traffialert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var info: TextView

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (hasLocation()) startMonitoring()
            else info.text = "Helyadat-engedély nélkül nem tud működni."
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        info = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, pad) }
        root.addView(info)
        fun button(text: String, onClick: () -> Unit) {
            root.addView(Button(this).apply {
                this.text = text
                setOnClickListener { onClick() }
            }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        button("Figyelés indítása") { onStartClicked() }
        button("Figyelés leállítása") {
            stopService(Intent(this, AlertService::class.java))
            refresh()
        }
        button("Lista frissítése most") {
            UpdateWorker.runNow(this)
            info.text = "Frissítés folyamatban..."
            info.postDelayed({ refresh() }, 6000)
        }
        setContentView(root)

        UpdateWorker.schedule(this)                       // napi ellenőrzés
        if (CameraStore.count(this) == 0) UpdateWorker.runNow(this) // első indulás
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        fun fmt(t: Long) =
            if (t == 0L) "még nem volt"
            else SimpleDateFormat("yyyy.MM.dd HH:mm", Locale("hu")).format(Date(t))
        info.text = "Mérők száma: ${CameraStore.count(this)}\n" +
            "Utolsó ellenőrzés: ${fmt(CameraStore.checkedAt(this))}\n" +
            "Utolsó változás: ${fmt(CameraStore.version(this))}\n" +
            "Állapot: ${CameraStore.status(this)}"
    }

    private fun hasLocation() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun onStartClicked() {
        val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startMonitoring() else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startMonitoring() {
        ContextCompat.startForegroundService(this, Intent(this, AlertService::class.java))
        info.text = "A figyelés elindult. Az értesítési sávban látszik, hogy fut."
    }
}
