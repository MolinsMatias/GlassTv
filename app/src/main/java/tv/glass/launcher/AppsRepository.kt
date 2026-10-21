package tv.glass.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

data class TvApp(val id: String, val label: String)
class AppsRepository(private val context: Context) {
    private val pm = context.packageManager
    private val icons = LruCache<String, Drawable>(48)
    suspend fun installed(): List<TvApp> = withContext(Dispatchers.IO) {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        pm.queryIntentActivities(query, 0).filter { it.activityInfo.packageName != context.packageName && it.activityInfo.exported && it.activityInfo.enabled }
            .map { TvApp(ComponentName(it.activityInfo.packageName, it.activityInfo.name).flattenToString(), it.loadLabel(pm).toString()) }
            .distinctBy { it.id }.sortedWith { a, b -> Collator.getInstance(Locale("es")).compare(a.label, b.label) }
    }
    fun icon(app: TvApp): Drawable {
        icons.get(app.id)?.let { return it }
        val icon = runCatching { pm.getActivityIcon(ComponentName.unflattenFromString(app.id)!!) }.getOrElse { pm.defaultActivityIcon }
        icons.put(app.id, icon)
        return icon
    }
    fun launch(id: String): Boolean = runCatching {
        val component = ComponentName.unflattenFromString(id) ?: return false
        val info = pm.getActivityInfo(component, 0)
        if (!info.exported || !info.enabled) return false
        context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        true
    }.getOrDefault(false)
    fun originalHomes(): List<TvApp> = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
        .filter { it.activityInfo.packageName != context.packageName && it.activityInfo.exported }
        .map { TvApp(ComponentName(it.activityInfo.packageName, it.activityInfo.name).flattenToString(), it.loadLabel(pm).toString()) }.distinctBy { it.id }
}
