package tv.glass.launcher

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

sealed class WeatherState {
    data object Hidden : WeatherState()
    data object Loading : WeatherState()
    data class Ready(val place: String, val temperature: Double, val code: Int, val observed: String) : WeatherState()
    data class Error(val message: String) : WeatherState()
    data object Offline : WeatherState()
}

class WeatherRepository {
    private var cachedCity = ""
    private var cachedAt = 0L
    private var cached: WeatherState? = null
    suspend fun load(city: String, online: Boolean): WeatherState = withContext(Dispatchers.IO) {
        if (city.isBlank()) return@withContext WeatherState.Hidden
        if (!online) return@withContext WeatherState.Offline
        if (city == cachedCity && System.currentTimeMillis() - cachedAt < 900_000) cached?.let { return@withContext it }
        try {
            val places = request("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(city, "UTF-8")}&count=5&language=es&format=json").optJSONArray("results")
                ?: return@withContext WeatherState.Error("Ciudad no encontrada; probá un nombre más específico")
            if (places.length() == 0) return@withContext WeatherState.Error("Ciudad no encontrada")
            // Ask for a more specific name in the UI when multiple places share it; show the actual selected location.
            val place = places.getJSONObject(0)
            val current = request("https://api.open-meteo.com/v1/forecast?latitude=${place.getDouble("latitude")}&longitude=${place.getDouble("longitude")}&current=temperature_2m,weather_code&timezone=auto").getJSONObject("current")
            WeatherState.Ready(listOf(place.getString("name"), place.optString("admin1"), place.optString("country")).filter { it.isNotBlank() }.distinct().joinToString(", "),
                current.getDouble("temperature_2m"), current.getInt("weather_code"), current.getString("time")).also {
                cachedCity = city; cached = it; cachedAt = System.currentTimeMillis()
            }
        } catch (e: CancellationException) { throw e }
        catch (e: IOException) { WeatherState.Error("No se pudo consultar el clima. Revisá la conexión") }
        catch (e: Exception) { WeatherState.Error("Respuesta meteorológica no disponible") }
    }
    private fun request(address: String): JSONObject {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 7000; connection.readTimeout = 7000
        return try {
            if (connection.responseCode != 200) throw IOException("HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader().use { it.readText().take(262144) }
            JSONObject(body)
        } finally { connection.disconnect() }
    }
}

fun weatherDescription(code: Int): String = when (code) {
    0 -> "Despejado"; 1, 2 -> "Parcialmente nublado"; 3 -> "Nublado"; 45, 48 -> "Niebla"
    in 51..57 -> "Llovizna"; in 61..67, in 80..82 -> "Lluvia"; in 71..77, 85, 86 -> "Nieve"
    in 95..99 -> "Tormenta"; else -> "Condición no disponible"
}
