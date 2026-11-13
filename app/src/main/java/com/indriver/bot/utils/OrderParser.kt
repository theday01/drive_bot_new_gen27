package com.indriver.bot.utils

/**
 * Pure Kotlin parser for extracting price and distance from order text.
 * Contains no Android framework dependencies so it can be thoroughly unit tested on JVM.
 */
data class ParsedOrder(
    val price: Double?,
    val distanceKm: Double?
) {
    val isValid: Boolean get() = price != null && distanceKm != null
}

object OrderParser {

    /**
     * Converts Eastern Arabic (Persian/Urdu) and Arabic-Indic digits to Latin digits (0-9).
     * Normalizes Arabic decimal separator (\u066B) to '.'.
     */
    fun normalizeDigits(input: CharSequence): String {
        val sb = StringBuilder(input.length)
        for (i in 0 until input.length) {
            val ch = input[i]
            when (ch) {
                in '\u0660'..'\u0669' -> sb.append((ch - '\u0660')) // Arabic-Indic digits (٠-٩)
                in '\u06F0'..'\u06F9' -> sb.append((ch - '\u06F0')) // Eastern Arabic digits (۰-۹)
                '\u066B' -> sb.append('.') // Arabic decimal separator (٫)
                '\u066C' -> sb.append(',') // Arabic thousands separator (٬)
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Parses price and distance from a list of strings extracted from accessibility nodes.
     */
    fun parse(nodeTexts: List<String>): ParsedOrder {
        var foundPrice: Double? = null
        var foundDistance: Double? = null

        // Pass 1: Try to extract distance from nodes
        for (raw in nodeTexts) {
            if (foundDistance == null) {
                val dist = parseDistance(raw)
                if (dist != null) {
                    foundDistance = dist
                }
            }
        }

        // Pass 2: Try to extract price from nodes
        for (raw in nodeTexts) {
            if (foundPrice == null) {
                val price = parsePrice(raw)
                if (price != null) {
                    foundPrice = price
                }
            }
        }

        return ParsedOrder(price = foundPrice, distanceKm = foundDistance)
    }

    /**
     * Convenience method to parse both price and distance from a single combined text string.
     */
    fun parse(singleText: String): ParsedOrder {
        return ParsedOrder(
            price = parsePrice(singleText),
            distanceKm = parseDistance(singleText)
        )
    }

    /**
     * Extracts distance in kilometers from text.
     * Supports:
     * - "12,5 km", "12.5 كم", "12.5km", "12 كم", "12,5 км", "١٢٫٥ كم", "١٢.٥ كم"
     * - "800 m", "800 م", "800 متر", "800م" -> converted to 0.8 km
     */
    fun parseDistance(text: String): Double? {
        if (text.isBlank()) return null
        val normalized = normalizeDigits(text.trim())

        // 1. Check for kilometers (number before or after unit)
        val kmRegex = Regex(
            """(?:(?:^|[^\w\d])(\d+(?:[.,]\d+)?)\s*(?:km|كم|км|كيلومتر|كيلو|ك\.م)(?:$|[^\w\d])|(?:^|[^\w\d])(?:km|كم|км|كيلومتر|كيلو|ك\.م)\s*(\d+(?:[.,]\d+)?)(?:$|[^\w\d]))""",
            RegexOption.IGNORE_CASE
        )
        val kmMatch = kmRegex.find(normalized)
        if (kmMatch != null) {
            val numStr = kmMatch.groups[1]?.value ?: kmMatch.groups[2]?.value
            if (numStr != null) {
                return cleanNumber(numStr)
            }
        }

        // 2. Check for meters (e.g., 500 m, 500 متر) -> convert to km
        val mRegex = Regex(
            """(?:^|[^\w\d])(\d+(?:[.,]\d+)?)\s*(?:m|متر)(?:$|[^\w\d])""",
            RegexOption.IGNORE_CASE
        )
        val mMatch = mRegex.find(normalized)
        if (mMatch != null) {
            val numStr = mMatch.groups[1]?.value
            if (numStr != null) {
                val meters = cleanNumber(numStr) ?: return null
                return meters / 1000.0
            }
        }

        return null
    }

    /**
     * Extracts price value from text containing currency symbols or codes.
     * Full support for Moroccan Dirham (MAD, DH, د.م, درهم) and other currencies.
     */
    fun parsePrice(text: String): Double? {
        if (text.isBlank()) return null
        val normalized = normalizeDigits(text.trim())

        val currencyTokens = "MAD|DH|DHS|د\\.م\\.|د\\.م|درهم|درهما|درهماً|EGP|SAR|AED|USD|EUR|DZD|DA|LE|L\\.E|ج\\.م|ر\\.س|د\\.إ|د\\.ك|دج|جنيه|ريال|دينار|руб|\\$|€|£|¥|₽|₹"

        val currencyPattern = Regex(
            """(?x)
            (?:
                (?:^|[^\w\d])
                (\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:[.,]\d{1,2})?)
                \s*
                (?:$currencyTokens)
                (?:$|[^\w\d])
            |
                (?:^|[^\w\d])
                (?:$currencyTokens)
                \s*
                (\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:[.,]\d{1,2})?)
                (?:$|[^\w\d])
            )
            """,
            RegexOption.IGNORE_CASE
        )

        val match = currencyPattern.find(normalized)
        if (match != null) {
            val numStr = match.groups[1]?.value ?: match.groups[2]?.value
            if (numStr != null) {
                return cleanNumber(numStr)
            }
        }

        return null
    }

    private fun cleanNumber(raw: String): Double? {
        val trimmed = raw.trim()
        val cleaned = if (trimmed.contains(",") && trimmed.contains(".")) {
            // "1,250.50" -> remove thousands comma
            trimmed.replace(",", "")
        } else if (trimmed.contains(",")) {
            val parts = trimmed.split(",")
            if (parts.size == 2 && parts[1].length == 3 && parts[0].length in 1..3) {
                // "1,000" or "50,000" -> thousands separator
                trimmed.replace(",", "")
            } else {
                // "12,5" -> decimal separator
                trimmed.replace(',', '.')
            }
        } else {
            raw
        }
        return cleaned.toDoubleOrNull()
    }
}
