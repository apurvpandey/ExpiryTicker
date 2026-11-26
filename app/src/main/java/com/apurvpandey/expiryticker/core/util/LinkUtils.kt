package com.apurvpandey.expiryticker.core.util

/** Cleans user-entered links and phone numbers before they are handed to other apps. */
object LinkUtils {
    private val webAddress = Regex("""^(https?://)?([A-Za-z0-9-]+\.)+[A-Za-z]{2,}(:\d+)?(/\S*)?$""", RegexOption.IGNORE_CASE)

    /** Returns an http(s) address, adding https:// when missing, or null if it is not a web address. */
    fun normalizeUrl(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty() || !webAddress.matches(text)) return null
        return if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) text
        else "https://$text"
    }

    /** Keeps digits and a leading plus; null when there are fewer than 3 digits. */
    fun normalizePhone(raw: String): String? {
        val trimmed = raw.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length < 3 || digits.length > 15) return null
        return (if (trimmed.startsWith("+")) "+" else "") + digits
    }

    fun isValidUrl(raw: String): Boolean = raw.isBlank() || normalizeUrl(raw) != null
    fun isValidPhone(raw: String): Boolean = raw.isBlank() || normalizePhone(raw) != null
}
