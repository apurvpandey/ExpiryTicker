package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.ItemSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemSearchTest {
    private fun match(query: String, vararg fields: String) = ItemSearch.matches(query, fields.toList())

    @Test fun blankQueryMatchesEverything() {
        assertTrue(match(""))
        assertTrue(match("   ", "anything"))
    }

    @Test fun ignoresCase() = assertTrue(match("CAR", "car insurance"))

    @Test fun everyWordMustMatchInAnyOrder() {
        assertTrue(match("insurance car", "Car insurance"))
        assertTrue(match("car ins", "Car insurance"))
        assertFalse(match("car passport", "Car insurance"))
    }

    @Test fun wordsCanComeFromDifferentFields() {
        // title + category + notes together
        assertTrue(match("netflix subscription family", "Netflix", "Subscription", "family plan"))
    }

    @Test fun accentsOnLatinLettersAreIgnored() {
        assertTrue(match("cafe", "Café membership"))
        assertTrue(match("café", "cafe membership"))
        assertEquals("resume", ItemSearch.normalize("Résumé"))
    }

    @Test fun hindiCategoryNameMatches() {
        assertTrue(match("बीमा", "Car insurance", "Insurance", "बीमा"))
        assertTrue(match("बीमा कार", "कार", "बीमा"))
    }

    @Test fun devanagariVowelSignsAreNotStripped() {
        // "बीमा" (insurance) must not match "बम" (bomb): the vowel signs are meaningful.
        assertFalse(match("बीमा", "बम"))
        assertEquals("बीमा", ItemSearch.normalize("बीमा"))
    }

    @Test fun composedAndDecomposedAccentsAreEqual() {
        val composed = "café"
        val decomposed = "café"
        assertEquals(ItemSearch.normalize(composed), ItemSearch.normalize(decomposed))
    }

    @Test fun noMatchWhenWordIsMissing() = assertFalse(match("passport", "Car insurance", "Insurance"))

    @Test fun extraWhitespaceInTheQueryIsHarmless() = assertTrue(match("  car    insurance  ", "Car insurance"))
}
