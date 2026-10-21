package tv.glass.launcher

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Requires an API 28 television emulator or a real device; never substitutes for Mi Box testing. */
@RunWith(AndroidJUnit4::class)
class RemoteNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    @Before fun openLauncher() {
        instrumentation.targetContext.startActivity(Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.desc("Todas las aplicaciones")), 10000))
        device.waitForIdle()
    }
    @Test fun dpadOpensControlAndBackRestoresDock() {
        device.pressDPadUp(); device.pressDPadCenter()
        assertTrue(device.wait(Until.hasObject(By.textStartsWith("Centro de control    ")), 3000))
        device.pressDPadDown(); device.pressDPadDown(); device.pressBack()
        assertFalse(device.hasObject(By.textStartsWith("Centro de control    ")))
        assertTrue(device.hasObject(By.desc("Todas las aplicaciones")))
    }
    @Test fun allAppsAreNavigable() {
        // Dock may contain preinstalled TV apps: traverse to its terminal Apps shortcut with D-pad.
        repeat(20) { device.pressDPadRight() }
        assertTrue("El foco debe detenerse en Apps al terminar el dock", device.hasObject(By.desc("Todas las aplicaciones").focused(true)))
        device.pressDPadCenter()
        assertTrue(device.wait(Until.hasObject(By.textStartsWith("‹  Todas las aplicaciones")), 3000))
        device.pressDPadDown(); device.pressDPadRight(); device.pressDPadLeft()
        device.pressBack()
        assertTrue(device.hasObject(By.desc("Todas las aplicaciones")))
    }
}
