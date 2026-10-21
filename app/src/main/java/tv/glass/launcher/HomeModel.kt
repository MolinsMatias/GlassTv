package tv.glass.launcher

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Activity-owned model: all work is cancelled when the Activity is destroyed. */
class HomeModel(context: Context, private val scope: CoroutineScope) {
    private val context = context.applicationContext
    val preferences = Preferences(context)
    val apps = AppsRepository(context)
    private val weatherRepository = WeatherRepository()
    private val appState = MutableStateFlow<List<TvApp>>(emptyList())
    val installed: StateFlow<List<TvApp>> = appState
    private val weatherState = MutableStateFlow<WeatherState>(WeatherState.Hidden)
    val weather: StateFlow<WeatherState> = weatherState
    private var weatherJob: Job? = null
    var appsError: String? = null; private set
    suspend fun refreshApps() {
        runCatching { apps.installed() }.onSuccess {
            appState.value = it; appsError = null
            if (preferences.favorites() == null && it.isNotEmpty()) preferences.saveFavorites(it.take(7).map(TvApp::id))
        }.onFailure { appsError = "No se pudieron leer las aplicaciones" }
    }
    fun refreshWeather() {
        weatherJob?.cancel()
        weatherJob = scope.launch {
            val city = preferences.appearance.value.city
            weatherState.value = if (city.isBlank()) WeatherState.Hidden else WeatherState.Loading
            weatherState.value = weatherRepository.load(city, online())
        }
    }
    fun online(): Boolean = runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }.getOrDefault(false)
}
