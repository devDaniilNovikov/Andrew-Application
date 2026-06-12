package ru.andrew.application

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class OfflineManifestPolicyTest {
    @Test
    fun manifestDoesNotRequestInternetPermission() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertFalse(
            "MVP must stay offline-only and must not request INTERNET permission.",
            manifest.contains("android.permission.INTERNET")
        )
    }
}

