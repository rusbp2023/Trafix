package hu.local.traffialert

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object Scraper {
    const val PAGE_URL = "https://www.autopalyamatrica.hu/fix-traffipax-lista-veda-terkep"

    fun download(): List<Camera> {
        val doc = Jsoup.connect(PAGE_URL)
            .userAgent("Mozilla/5.0 (Android) TraffiAlert/1.0")
            .timeout(20_000)
            .get()
        return parse(doc)
    }

    /**
     * Megyénként egy táblázat: az első sor a megye neve ("Pest megye"),
     * utána fejléc, majd az adatsorok:
     * település | útszám | km-szelvény/utca | szélesség | hosszúság
     * A büntetési táblázat első sorában nincs "megye", ezért kimarad.
     */
    fun parse(doc: Document): List<Camera> {
        val out = LinkedHashSet<Camera>()
        for (table in doc.select("table")) {
            val rows = table.select("tr")
            if (rows.isEmpty()) continue
            val header = rows[0].text().trim()
            val county = if (header.contains("megye", ignoreCase = true)) header else continue
            for (row in rows.drop(1)) {
                val c = row.select("td").map { it.text().trim() }
                if (c.size < 5) continue
                // az oldalon van pont helyett vesszős koordináta is (pl. 20,381692)
                val lat = c[3].replace(',', '.').toDoubleOrNull() ?: continue
                val lon = c[4].replace(',', '.').toDoubleOrNull() ?: continue
                if (lat !in 45.7..48.7 || lon !in 16.0..23.0) continue // nyilvánvalóan hibás sor
                out.add(Camera(county, c[0], c[1], c[2], lat, lon))
            }
        }
        return out.toList()
    }
}
