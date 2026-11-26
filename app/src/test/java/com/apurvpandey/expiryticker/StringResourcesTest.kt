package com.apurvpandey.expiryticker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards translations against drifting from the English source: a missing key falls back
 * silently, but a different set of format placeholders can crash at runtime.
 */
class StringResourcesTest {
    private val resDir = File("src/main/res")

    private data class Entry(val name: String, val texts: List<String>)

    private fun load(file: File): Map<String, Entry> {
        val xml = file.readText()
        val result = linkedMapOf<String, Entry>()
        Regex("""<string\s+name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).forEach { result[it.groupValues[1]] = Entry(it.groupValues[1], listOf(it.groupValues[2])) }
        Regex("""<plurals\s+name="([^"]+)"[^>]*>(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { m ->
            val items = Regex("""<item[^>]*>(.*?)</item>""", RegexOption.DOT_MATCHES_ALL).findAll(m.groupValues[2]).map { it.groupValues[1] }.toList()
            result[m.groupValues[1]] = Entry(m.groupValues[1], items)
        }
        return result
    }

    private val english = load(File(resDir, "values/strings.xml"))
    private val translations = resDir.listFiles { f -> f.isDirectory && f.name.startsWith("values-") }
        .orEmpty()
        .map { it.name to File(it, "strings.xml") }
        .filter { it.second.exists() }
        .associate { it.first to load(it.second) }

    /** Argument positions used by a message, e.g. {1, 2} for "%1$s ... %2$d". */
    private fun placeholders(text: String): Set<String> =
        Regex("""%(\d+\$)?[sd]""").findAll(text).map { it.value.filter { c -> c != '%' } }.toSet()

    @Test fun englishSourceIsNotEmpty() = assertTrue(english.size > 200)

    @Test fun atLeastOneTranslationIsPresent() = assertTrue(translations.isNotEmpty())

    @Test fun everyTranslationHasExactlyTheEnglishKeys() {
        translations.forEach { (locale, strings) ->
            assertEquals("$locale is missing keys", emptySet<String>(), english.keys - strings.keys)
            assertEquals("$locale has keys English lacks", emptySet<String>(), strings.keys - english.keys)
        }
    }

    @Test fun translationsUseTheSameFormatPlaceholders() {
        translations.forEach { (locale, strings) ->
            english.forEach { (name, source) ->
                val translated = strings[name] ?: return@forEach
                // Plurals may phrase "one" without a number, so compare the union across forms.
                val expected = source.texts.flatMap { placeholders(it) }.toSet()
                val actual = translated.texts.flatMap { placeholders(it) }.toSet()
                assertTrue("$locale/$name: unexpected placeholders ${actual - expected}", expected.containsAll(actual))
                assertTrue("$locale/$name: lost placeholders ${expected - actual}", actual.containsAll(expected))
            }
        }
    }

    @Test fun noTranslationIsBlank() {
        translations.forEach { (locale, strings) ->
            strings.values.forEach { e -> e.texts.forEach { assertTrue("$locale/${e.name} is blank", it.isNotBlank()) } }
        }
    }
}
