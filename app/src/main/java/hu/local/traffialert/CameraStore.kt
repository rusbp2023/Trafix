package hu.local.traffialert

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object CameraStore {
    private const val FILE = "cameras.json"
    private fun prefs(c: Context) = c.getSharedPreferences("cams", Context.MODE_PRIVATE)

    fun version(c: Context): Long = prefs(c).getLong("updatedAt", 0L)
    fun checkedAt(c: Context): Long = prefs(c).getLong("checkedAt", 0L)
    fun count(c: Context): Int = prefs(c).getInt("count", 0)
    fun status(c: Context): String = prefs(c).getString("status", "még nem futott") ?: ""
    fun setStatus(c: Context, s: String) = prefs(c).edit().putString("status", s).apply()

    fun load(c: Context): List<Camera> {
        val f = File(c.filesDir, FILE)
        if (!f.exists()) return emptyList()
        return try {
            val a = JSONArray(f.readText())
            List(a.length()) {
                val o = a.getJSONObject(it)
                Camera(
                    o.getString("county"), o.getString("town"), o.getString("road"),
                    o.getString("place"), o.getDouble("lat"), o.getDouble("lon")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** true, ha a lista tényleg különbözött a mentettől. */
    @Synchronized
    fun saveIfChanged(c: Context, list: List<Camera>): Boolean {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("county", it.county); put("town", it.town); put("road", it.road)
                put("place", it.place); put("lat", it.lat); put("lon", it.lon)
            })
        }
        val json = arr.toString()
        val now = System.currentTimeMillis()
        prefs(c).edit().putLong("checkedAt", now).apply()

        val f = File(c.filesDir, FILE)
        if (f.exists() && f.readText() == json) return false

        val tmp = File(c.filesDir, "$FILE.tmp")
        tmp.writeText(json)
        tmp.renameTo(f)
        prefs(c).edit().putLong("updatedAt", now).putInt("count", list.size).apply()
        return true
    }
}
