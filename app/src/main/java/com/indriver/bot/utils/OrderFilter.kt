package com.indriver.bot.utils

enum class FilterDecision {
    ACCEPT,
    REJECT_PRICE_TOO_LOW,
    REJECT_DISTANCE_TOO_HIGH,
    SKIP_UNPARSEABLE,
    SKIP_AUTO_BID_DISABLED
}

/**
 * Pure filter evaluator comparing an extracted order against user thresholds.
 * Independent of Android framework for fast, robust unit testing.
 */
object OrderFilter {

    fun evaluate(
        order: ParsedOrder,
        minPrice: Double,
        maxDistance: Double,
        isAutoBidEnabled: Boolean = true
    ): FilterDecision {
        if (!isAutoBidEnabled) {
            return FilterDecision.SKIP_AUTO_BID_DISABLED
        }

        val price = order.price ?: return FilterDecision.SKIP_UNPARSEABLE
        val distance = order.distanceKm ?: return FilterDecision.SKIP_UNPARSEABLE

        if (price < minPrice) {
            return FilterDecision.REJECT_PRICE_TOO_LOW
        }
        if (distance > maxDistance) {
            return FilterDecision.REJECT_DISTANCE_TOO_HIGH
        }

        return FilterDecision.ACCEPT
    }
}

