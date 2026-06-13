package ru.andrew.application

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class OfflineManifestPolicyTest {
    @Test
    fun manifestsDoNotRequestNetworkOrFutureFeaturePermissions() {
        manifestsToCheck().forEach { manifestFile ->
            val manifest = manifestFile.parseXml()
            val requestedPermissions = permissionTags
                .flatMap { tag -> manifest.elementsByTagName(tag) }
                .map { it.androidAttribute("name") }
                .toSet()

            deniedPermissions.forEach { permission ->
                assertFalse(
                    "${manifestFile.path} must not request $permission during the offline MVP stage.",
                    permission in requestedPermissions
                )
            }
        }
    }

    @Test
    fun manifestsDoNotDeclareNetworkConfiguration() {
        manifestsToCheck().forEach { manifestFile ->
            val application = manifestFile.parseXml().applicationElement()

            assertFalse(
                "${manifestFile.path} must not enable cleartext network traffic in the offline MVP.",
                application.hasAttributeNS(ANDROID_NAMESPACE, "usesCleartextTraffic")
            )
            assertFalse(
                "${manifestFile.path} must not configure a network security config in the offline MVP.",
                application.hasAttributeNS(ANDROID_NAMESPACE, "networkSecurityConfig")
            )
        }
    }

    @Test
    fun mainActivityIsExportedLauncherActivity() {
        manifestsToCheck().forEach { manifestFile ->
            val launcherActivity = manifestFile.parseXml()
                .elementsByTagName("activity")
                .firstOrNull { activity ->
                    activity.androidAttribute("name").endsWith(".MainActivity") &&
                        activity.elementsByTagName("intent-filter")
                            .any { intentFilter -> intentFilter.isLauncherIntentFilter() }
                }

            assertNotNull(
                "${manifestFile.path} must declare MainActivity as the launcher activity.",
                launcherActivity
            )
            assertTrue(
                "${manifestFile.path} must export MainActivity because it has a launcher intent-filter.",
                launcherActivity?.androidAttribute("exported") == "true"
            )
        }
    }

    private fun Element.isLauncherIntentFilter(): Boolean {
        val actionNames = elementsByTagName("action").map { it.androidAttribute("name") }
        val categoryNames = elementsByTagName("category").map { it.androidAttribute("name") }

        return "android.intent.action.MAIN" in actionNames &&
            "android.intent.category.LAUNCHER" in categoryNames
    }

    private fun manifestsToCheck(): List<File> {
        val manifests = listOf(
            "src/main/AndroidManifest.xml",
            "build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
            "build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml",
            "build/intermediates/packaged_manifests/debug/processDebugManifestForPackage/AndroidManifest.xml"
        )
            .map(::File)
            .filter(File::exists)

        assertTrue(
            "At least the source AndroidManifest.xml must exist.",
            manifests.isNotEmpty()
        )

        return manifests
    }

    private fun File.parseXml(): Document =
        DocumentBuilderFactory.newInstance()
            .apply {
                isNamespaceAware = true
                setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
                )
            }
            .newDocumentBuilder()
            .parse(this)

    private fun Document.applicationElement(): Element =
        elementsByTagName("application").first()

    private fun Document.elementsByTagName(name: String): List<Element> =
        documentElement.getElementsByTagName(name)
            .let { nodes -> List(nodes.length) { index -> nodes.item(index) as Element } }

    private fun Element.elementsByTagName(name: String): List<Element> =
        getElementsByTagName(name)
            .let { nodes -> List(nodes.length) { index -> nodes.item(index) as Element } }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(ANDROID_NAMESPACE, name)

    private companion object {
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

        private val permissionTags = listOf(
            "uses-permission",
            "uses-permission-sdk-23"
        )

        private val deniedPermissions = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.CHANGE_NETWORK_STATE",
            "android.permission.CHANGE_WIFI_STATE",
            "android.permission.NEARBY_WIFI_DEVICES",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CONTACTS"
        )
    }
}
