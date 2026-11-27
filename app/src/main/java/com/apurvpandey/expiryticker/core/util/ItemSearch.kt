package com.apurvpandey.expiryticker.core.util

import java.text.Normalizer

/**
 * Forgiving text matching for the dashboard search, so people can type the way they speak:
 * capitalisation, accents on Latin letters and word order do not matter, and every word typed
 * has to be found somewhere in the item's text.
 */
object ItemSearch {

    /**
     * Lower-cases, composes characters and removes accents from Latin letters ("café" matches
     * "cafe"). Marks on other scripts are kept: in Devanagari or Arabic they are vowels and
     * carry meaning, and dropping them would make unrelated words look the same.
     */
    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val out = StringBuilder(decomposed.length)
        var afterLatinLetter = false
        for (ch in decomposed) {
            if (Character.getType(ch) == Character.NON_SPACING_MARK.toInt()) {
                if (afterLatinLetter) continue
            } else {
                afterLatinLetter = Character.UnicodeScript.of(ch.code) == Character.UnicodeScript.LATIN
            }
            out.append(ch)
        }
        return Normalizer.normalize(out, Normalizer.Form.NFC).lowercase()
    }

    fun tokens(query: String): List<String> =
        normalize(query).split(Regex("\\s+")).filter { it.isNotEmpty() }

    /** True when [query] is blank, or when each of its words appears in at least one of [fields]. */
    fun matches(query: String, fields: List<String>): Boolean {
        val words = tokens(query)
        if (words.isEmpty()) return true
        val haystack = fields.joinToString(" ") { normalize(it) }
        return words.all { it in haystack }
    }
}
