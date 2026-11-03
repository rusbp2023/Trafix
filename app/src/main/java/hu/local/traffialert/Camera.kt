package hu.local.traffialert

data class Camera(
    val county: String,
    val town: String,
    val road: String,
    val place: String,
    val lat: Double,
    val lon: Double
) {
    /** Rövid szöveg az értesítéshez, pl. "Budapest – Váci út". */
    val label: String
        get() = listOf(town, listOf(road, place).filter { it.isNotBlank() }.joinToString(" "))
            .filter { it.isNotBlank() }
            .joinToString(" – ")
}
