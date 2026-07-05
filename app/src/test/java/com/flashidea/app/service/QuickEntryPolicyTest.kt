package com.flashidea.app.service

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class QuickEntryPolicyTest {

    @Test
    fun `quick entry does not expose overlay capsule path`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val settingsScreen = File(
            "src/main/java/com/flashidea/app/ui/settings/SettingsScreen.kt"
        ).readText()

        assertFalse(manifest.contains("SYSTEM_ALERT_WINDOW"))
        assertFalse(manifest.contains("FOREGROUND_SERVICE"))
        assertFalse(manifest.contains("SidebarBubbleService"))
        assertFalse(settingsScreen.contains("侧边快捷胶囊"))
        assertFalse(settingsScreen.contains("SidebarBubbleService"))
        assertFalse(File("src/main/java/com/flashidea/app/service/SidebarBubbleService.kt").exists())
        assertFalse(File("src/main/res/layout/bubble_view.xml").exists())
    }
}
