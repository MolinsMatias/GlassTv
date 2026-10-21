package tv.glass.launcher

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

data class Appearance(val dark: Boolean = true, val twentyFourHour: Boolean = true,
    val background: Int = 0, val reduceMotion: Boolean = false, val city: String = "")

/** Ordered values, including an explicitly empty list, survive process and device restarts. */
class Preferences(context: Context) {
    private val disk = context.getSharedPreferences("glass.preferences.v1", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(read())
    val appearance: StateFlow<Appearance> = mutable
    private fun read() = Appearance(disk.getBoolean("dark", true), disk.getBoolean("24h", true),
        disk.getInt("background", 0).coerceIn(0, 2), disk.getBoolean("motion", false), disk.getString("city", "").orEmpty())
    fun update(value: Appearance) {
        disk.edit().putBoolean("dark", value.dark).putBoolean("24h", value.twentyFourHour)
            .putInt("background", value.background).putBoolean("motion", value.reduceMotion)
            .putString("city", value.city.trim().take(100)).apply()
        mutable.value = read()
    }
    fun favorites(): List<String>? {
        val raw = disk.getString("favorites", null) ?: return null
        return runCatching { val a = JSONArray(raw); (0 until a.length()).map { a.getString(it) }.distinct() }.getOrDefault(emptyList())
    }
    fun saveFavorites(ids: List<String>) { disk.edit().putString("favorites", JSONArray(ids.distinct()).toString()).apply() }
}

object FavoriteOrder {
    fun toggle(ids: List<String>, id: String) = if (id in ids) ids - id else ids + id
    fun move(ids: List<String>, id: String, delta: Int): List<String> {
        val from = ids.indexOf(id)
        if (from < 0) return ids
        val to = (from + delta).coerceIn(0, ids.lastIndex)
        return ids.toMutableList().apply { removeAt(from); add(to, id) }
    }
}

enum class Surface { HOME, APPS, CONTROL }
class NavigationState {
    var surface = Surface.HOME; private set
    fun open(next: Surface) { surface = next }
    fun back(): Boolean = if (surface != Surface.HOME) { surface = Surface.HOME; true } else false
}
