package tv.glass.launcher

import android.app.Application

/** Crash-loop recovery is a local safe screen, never a system-package or default-home mutation. */
class GlassApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val prefs = getSharedPreferences("recovery", MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val count = if (now - prefs.getLong("last", 0) < 300_000) prefs.getInt("crashes", 0) + 1 else 1
            prefs.edit().putLong("last", now).putInt("crashes", count).commit()
            previous?.uncaughtException(thread, throwable)
        }
    }
}
