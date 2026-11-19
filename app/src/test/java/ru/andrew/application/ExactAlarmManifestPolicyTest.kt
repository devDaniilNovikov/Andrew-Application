package ru.andrew.application

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import ru.andrew.application.notifications.BootReceiver

/**
 * Напоминания должны приходить точно в срок без ручной настройки:
 * на Android 13+ — через USE_EXACT_ALARM (выдаётся при установке), на Android 12–12L — через SCHEDULE_EXACT_ALARM.
 * Уже поставленные будильники перепланируются после перезагрузки, обновления приложения и выдачи разрешения.
 */
class ExactAlarmManifestPolicyTest {

    @Test
    fun useExactAlarmIsDeclaredForEveryApiLevelItExists() {
        manifestsToCheck().forEach { manifestFile ->
            val permission = manifestFile.parseXml().permission(USE_EXACT_ALARM)

            assertNotNull(
                "${manifestFile.path} must declare $USE_EXACT_ALARM so reminders are exact on Android 13+ without user action.",
                permission
            )
            assertEquals(
                "${manifestFile.path} must not limit $USE_EXACT_ALARM with maxSdkVersion.",
                "",
                permission?.androidAttribute("maxSdkVersion")
            )
        }
    }

    @Test
    fun scheduleExactAlarmIsRequestedOnlyUpToApi32() {
        manifestsToCheck().forEach { manifestFile ->
            val permission = manifestFile.parseXml().permission(SCHEDULE_EXACT_ALARM)

            assertNotNull(
                "${manifestFile.path} must keep $SCHEDULE_EXACT_ALARM for Android 12–12L, where $USE_EXACT_ALARM does not exist.",
                permission
            )
            assertEquals(
                "${manifestFile.path} must limit $SCHEDULE_EXACT_ALARM to API 32: on Android 13+ $USE_EXACT_ALARM is used instead.",
                "32",
                permission?.androidAttribute("maxSdkVersion")
            )
        }
    }

    @Test
    fun bootReceiverListensForEveryRescheduleAction() {
        assertTrue(
            "BootReceiver must reschedule reminders after boot, app update and exact alarm permission grant.",
            BootReceiver.RESCHEDULE_ACTIONS.containsAll(
                setOf(
                    "android.intent.action.BOOT_COMPLETED",
                    "android.intent.action.MY_PACKAGE_REPLACED",
                    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
                )
            )
        )

        manifestsToCheck().forEach { manifestFile ->
            val receiver = manifestFile.parseXml()
                .elementsByTagName("receiver")
                .firstOrNull { it.androidAttribute("name").endsWith(".notifications.BootReceiver") }

            assertNotNull("${manifestFile.path} must declare BootReceiver.", receiver)

            val actions = receiver!!.elementsByTagName("action")
                .map { it.androidAttribute("name") }
                .toSet()

            assertEquals(
                "${manifestFile.path}: BootReceiver intent-filter must match BootReceiver.RESCHEDULE_ACTIONS.",
                BootReceiver.RESCHEDULE_ACTIONS,
                actions
            )
        }
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

    private fun Document.permission(name: String): Element? =
        elementsByTagName("uses-permission").firstOrNull { it.androidAttribute("name") == name }

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
        private const val USE_EXACT_ALARM = "android.permission.USE_EXACT_ALARM"
        private const val SCHEDULE_EXACT_ALARM = "android.permission.SCHEDULE_EXACT_ALARM"
    }
}
