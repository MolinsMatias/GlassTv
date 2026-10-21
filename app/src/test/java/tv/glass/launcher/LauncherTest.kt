package tv.glass.launcher

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LauncherTest {
    private val context get() = RuntimeEnvironment.getApplication()
    @Before fun reset() {
        context.getSharedPreferences("glass.preferences.v1", 0).edit().clear().commit()
        context.getSharedPreferences("recovery", 0).edit().clear().commit()
    }
    @Test fun favoritesHaveStableOrderAndDoNotDuplicate() {
        val prefs = Preferences(context)
        assertNull(prefs.favorites())
        prefs.saveFavorites(listOf("app/c", "app/a", "app/c"))
        assertEquals(listOf("app/c", "app/a"), Preferences(context).favorites())
        prefs.saveFavorites(emptyList())
        assertEquals(emptyList<String>(), Preferences(context).favorites())
    }
    @Test fun favoritesCanBeAddedRemovedAndMovedAcrossRestarts() {
        var ids = FavoriteOrder.toggle(listOf("A", "B"), "C")
        ids = FavoriteOrder.move(ids, "C", -1)
        assertEquals(listOf("A", "C", "B"), ids)
        ids = FavoriteOrder.toggle(ids, "A")
        Preferences(context).saveFavorites(ids)
        assertEquals(listOf("C", "B"), Preferences(context).favorites())
        assertEquals(ids, FavoriteOrder.move(ids, "unknown", 1))
        assertEquals(ids, FavoriteOrder.move(ids, "C", -1))
    }
    @Test fun appearanceAndCityPersist() {
        val expected = Appearance(false, false, 1, true, "Mendoza")
        Preferences(context).update(expected)
        assertEquals(expected, Preferences(context).appearance.value)
    }
    @Test fun corruptPreferencesFallBackSafely() {
        context.getSharedPreferences("glass.preferences.v1", 0).edit().putString("favorites", "not-json").putInt("background", 99).commit()
        assertEquals(emptyList<String>(), Preferences(context).favorites())
        assertEquals(2, Preferences(context).appearance.value.background)
    }
    @Test fun controlCenterIsCollapsedAndBackClosesEverySurface() {
        val navigation = NavigationState()
        assertEquals(Surface.HOME, navigation.surface)
        assertFalse(navigation.back())
        for (surface in listOf(Surface.CONTROL, Surface.APPS)) {
            navigation.open(surface); assertTrue(navigation.back()); assertEquals(Surface.HOME, navigation.surface)
        }
    }
    @Test fun offlineWeatherAndDisabledWeatherNeverFabricateData() = runBlocking {
        val repository = WeatherRepository()
        assertEquals(WeatherState.Hidden, repository.load("", false))
        assertEquals(WeatherState.Offline, repository.load("Madrid", false))
        assertEquals("Condición no disponible", weatherDescription(999))
    }
    @Test fun missingApplicationReturnsFailure() {
        val apps = AppsRepository(context)
        assertFalse(apps.launch("invalid"))
        assertFalse(apps.launch("does.not.exist/.Missing"))
    }
    @Test fun televisionAppsAreListedOnceAndCanBeOpened() = runBlocking {
        val pm = shadowOf(context.packageManager)
        val info = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "example.television"; name = "example.television.Player"
                exported = true; enabled = true
                applicationInfo = ApplicationInfo().apply { packageName = "example.television"; enabled = true }
            }
            nonLocalizedLabel = "TV Player"
        }
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        pm.addResolveInfoForIntent(query, listOf(info, info))
        pm.addOrUpdateActivity(info.activityInfo)
        val apps = AppsRepository(context)
        val listed = apps.installed().filter { it.label == "TV Player" }
        assertEquals(1, listed.size)
        assertTrue(apps.launch(listed.single().id))
        assertEquals(ComponentName("example.television", "example.television.Player"), shadowOf(context).nextStartedActivity.component)
    }
    @Test fun activityCanPauseResumeAndRecreateWithoutPermissions() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertEquals(Surface.HOME, activity.get().navigation.surface)
        activity.pause().stop().start().resume().visible()
        activity.recreate()
        assertEquals(Surface.HOME, activity.get().navigation.surface)
        activity.pause().stop().destroy()
    }
}
