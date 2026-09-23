package com.wakemyway.app

import android.content.pm.ApplicationInfo
import android.content.res.XmlResourceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.nio.file.Files
import java.nio.file.Path
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupPolicyContractTest {
    @Test
    fun `platform backup is constrained by explicit deny by default rules`() {
        val context = RuntimeEnvironment.getApplication()

        assertTrue(
            "allowBackup stays enabled only so the explicit XML policy can govern supported transports",
            context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0,
        )
        assertManifestWiring()

        assertLegacyRules(context.resources.getXml(R.xml.backup_rules))
        assertModernRules(context.resources.getXml(R.xml.data_extraction_rules))
    }

    private fun assertLegacyRules(parser: XmlResourceParser) {
        parser.use {
            val snapshot = parseRules(it)
            assertEquals("full-backup-content", snapshot.root)
            assertTrue(snapshot.includes.isEmpty())
            assertEquals(EXCLUDED_DOMAINS, snapshot.excludesBySection.getValue(ROOT_SECTION))
        }
    }

    private fun assertModernRules(parser: XmlResourceParser) {
        parser.use {
            val snapshot = parseRules(it)
            assertEquals("data-extraction-rules", snapshot.root)
            assertTrue(snapshot.includes.isEmpty())
            assertEquals(EXCLUDED_DOMAINS, snapshot.excludesBySection.getValue("cloud-backup"))
            assertEquals(EXCLUDED_DOMAINS, snapshot.excludesBySection.getValue("device-transfer"))
        }
    }

    private fun assertManifestWiring() {
        val manifest = readSourceManifest()
        assertTrue(
            "Android 12+ extraction rules must stay wired from the application manifest",
            manifest.contains("""android:dataExtractionRules="@xml/data_extraction_rules""""),
        )
        assertTrue(
            "Android 11-and-lower full-backup rules must stay wired from the application manifest",
            manifest.contains("""android:fullBackupContent="@xml/backup_rules""""),
        )
    }

    private fun readSourceManifest(): String {
        val start = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        val manifestPath = generateSequence(start) { current -> current.parent }
            .flatMap { directory ->
                sequenceOf(
                    directory.resolve("app/src/main/AndroidManifest.xml"),
                    directory.resolve("apps/android/app/src/main/AndroidManifest.xml"),
                )
            }
            .firstOrNull(Files::isRegularFile)

        return Files.readString(
            requireNotNull(manifestPath) { "Could not locate app/src/main/AndroidManifest.xml" },
        )
    }

    private fun parseRules(parser: XmlResourceParser): RuleSnapshot {
        var root: String? = null
        var section = ROOT_SECTION
        val includes = mutableListOf<String>()
        val excludes = mutableMapOf<String, MutableSet<String>>()

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name
                    if (root == null) root = tag
                    if (tag == "cloud-backup" || tag == "device-transfer") {
                        section = tag
                    }
                    if (tag == "include") {
                        includes += "${parser.getAttributeValue(null, "domain")}:${parser.getAttributeValue(null, "path")}"
                    }
                    if (tag == "exclude") {
                        val domain = requireNotNull(parser.getAttributeValue(null, "domain"))
                        val path = requireNotNull(parser.getAttributeValue(null, "path"))
                        assertEquals("Every excluded domain must be denied in full", ".", path)
                        excludes.getOrPut(section) { mutableSetOf() } += domain
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "cloud-backup" || parser.name == "device-transfer") {
                        section = ROOT_SECTION
                    }
                }
            }
            parser.next()
        }

        return RuleSnapshot(
            root = requireNotNull(root),
            includes = includes,
            excludesBySection = excludes,
        )
    }

    private data class RuleSnapshot(
        val root: String,
        val includes: List<String>,
        val excludesBySection: Map<String, Set<String>>,
    )

    private companion object {
        const val ROOT_SECTION = "root"
        val EXCLUDED_DOMAINS = setOf(
            "root",
            "file",
            "database",
            "sharedpref",
            "external",
            "device_root",
            "device_file",
            "device_database",
            "device_sharedpref",
        )
    }
}
