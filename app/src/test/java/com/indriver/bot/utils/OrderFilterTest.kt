package com.indriver.bot.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderFilterTest {

    @Test
    fun testAcceptMatchingOrder() {
        val order = ParsedOrder(price = 50.0, distanceKm = 8.0)
        val decision = OrderFilter.evaluate(
            order = order,
            minPrice = 30.0,
            maxDistance = 10.0,
            isAutoBidEnabled = true
        )
        assertEquals(FilterDecision.ACCEPT, decision)
    }

    @Test
    fun testAcceptBoundaryValues() {
        val order = ParsedOrder(price = 30.0, distanceKm = 10.0)
        val decision = OrderFilter.evaluate(
            order = order,
            minPrice = 30.0,
            maxDistance = 10.0,
            isAutoBidEnabled = true
        )
        assertEquals(FilterDecision.ACCEPT, decision)
    }

    @Test
    fun testRejectPriceTooLow() {
        val order = ParsedOrder(price = 25.0, distanceKm = 5.0)
        val decision = OrderFilter.evaluate(
            order = order,
            minPrice = 30.0,
            maxDistance = 10.0,
            isAutoBidEnabled = true
        )
        assertEquals(FilterDecision.REJECT_PRICE_TOO_LOW, decision)
    }

    @Test
    fun testRejectDistanceTooHigh() {
        val order = ParsedOrder(price = 50.0, distanceKm = 15.0)
        val decision = OrderFilter.evaluate(
            order = order,
            minPrice = 30.0,
            maxDistance = 10.0,
            isAutoBidEnabled = true
        )
        assertEquals(FilterDecision.REJECT_DISTANCE_TOO_HIGH, decision)
    }

    @Test
    fun testSkipUnparseablePriceOrDistance() {
        val missingPrice = ParsedOrder(price = null, distanceKm = 5.0)
        assertEquals(
            FilterDecision.SKIP_UNPARSEABLE,
            OrderFilter.evaluate(missingPrice, minPrice = 30.0, maxDistance = 10.0)
        )

        val missingDistance = ParsedOrder(price = 50.0, distanceKm = null)
        assertEquals(
            FilterDecision.SKIP_UNPARSEABLE,
            OrderFilter.evaluate(missingDistance, minPrice = 30.0, maxDistance = 10.0)
        )

        val missingBoth = ParsedOrder(price = null, distanceKm = null)
        assertEquals(
            FilterDecision.SKIP_UNPARSEABLE,
            OrderFilter.evaluate(missingBoth, minPrice = 30.0, maxDistance = 10.0)
        )
    }

    @Test
    fun testSkipWhenAutoBidDisabled() {
        val order = ParsedOrder(price = 100.0, distanceKm = 2.0)
        val decision = OrderFilter.evaluate(
            order = order,
            minPrice = 30.0,
            maxDistance = 10.0,
            isAutoBidEnabled = false
        )
        assertEquals(FilterDecision.SKIP_AUTO_BID_DISABLED, decision)
    }
}

