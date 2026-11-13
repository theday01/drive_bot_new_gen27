package com.indriver.bot.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderParserTest {

    @Test
    fun testParseArabicIndicDigits() {
        // Arabic digits with decimal dot
        val distDot = OrderParser.parseDistance("١٢.٥ كم")
        assertEquals(12.5, distDot!!, 0.001)

        // Arabic digits with decimal comma
        val distComma = OrderParser.parseDistance("١٢,٥ كم")
        assertEquals(12.5, distComma!!, 0.001)

        // Arabic digits with Arabic decimal separator (٫)
        val distArabicSep = OrderParser.parseDistance("١٢٫٥ كم")
        assertEquals(12.5, distArabicSep!!, 0.001)

        // Arabic price
        val price1 = OrderParser.parsePrice("٥٠ ج.م")
        assertEquals(50.0, price1!!, 0.001)

        val price2 = OrderParser.parsePrice("٥٠٫٥ ج.م")
        assertEquals(50.5, price2!!, 0.001)

        val price3 = OrderParser.parsePrice("٥٠.٥ ر.س")
        assertEquals(50.5, price3!!, 0.001)
    }

    @Test
    fun testParseDistanceFormats() {
        // "12,5 km"
        val dist1 = OrderParser.parseDistance("12,5 km")
        assertEquals(12.5, dist1!!, 0.001)

        // "12.5 كم"
        val dist2 = OrderParser.parseDistance("12.5 كم")
        assertEquals(12.5, dist2!!, 0.001)

        // "12.5km" without space
        val dist3 = OrderParser.parseDistance("12.5km")
        assertEquals(12.5, dist3!!, 0.001)

        // Integer distance "12 كم"
        val dist4 = OrderParser.parseDistance("12 كم")
        assertEquals(12.0, dist4!!, 0.001)

        // Russian Cyrillic "12,5 км"
        val dist5 = OrderParser.parseDistance("12,5 км")
        assertEquals(12.5, dist5!!, 0.001)

        // Distance in meters: "800 m" -> 0.8 km
        val distMeters1 = OrderParser.parseDistance("800 m")
        assertEquals(0.8, distMeters1!!, 0.001)

        val distMeters2 = OrderParser.parseDistance("500 متر")
        assertEquals(0.5, distMeters2!!, 0.001)
    }

    @Test
    fun testParsePriceFormats() {
        // Egyptian Pound
        assertEquals(50.0, OrderParser.parsePrice("50 ج.م")!!, 0.001)
        assertEquals(50.0, OrderParser.parsePrice("50ج.م")!!, 0.001)
        assertEquals(75.5, OrderParser.parsePrice("75.50 EGP")!!, 0.001)
        assertEquals(60.0, OrderParser.parsePrice("60 LE")!!, 0.001)

        // Saudi Riyal
        assertEquals(30.0, OrderParser.parsePrice("30 ر.س")!!, 0.001)
        assertEquals(45.5, OrderParser.parsePrice("45.5 SAR")!!, 0.001)

        // Dollars / Euros
        assertEquals(25.0, OrderParser.parsePrice("$25")!!, 0.001)
        assertEquals(25.5, OrderParser.parsePrice("$25.50")!!, 0.001)
        assertEquals(40.5, OrderParser.parsePrice("40,50 €")!!, 0.001)

        // Thousands separator
        assertEquals(1500.0, OrderParser.parsePrice("1,500 EGP")!!, 0.001)
    }

    @Test
    fun testMissingValues() {
        // Blank or irrelevant text
        assertNull(OrderParser.parseDistance(""))
        assertNull(OrderParser.parsePrice(""))
        assertNull(OrderParser.parseDistance("Street 15, Downtown"))
        assertNull(OrderParser.parsePrice("Street 15, Downtown"))

        // Only distance present
        val order1 = OrderParser.parse("12.5 km")
        assertNull(order1.price)
        assertEquals(12.5, order1.distanceKm!!, 0.001)
        assertFalse(order1.isValid)

        // Only price present
        val order2 = OrderParser.parse("50 ج.م")
        assertEquals(50.0, order2.price!!, 0.001)
        assertNull(order2.distanceKm)
        assertFalse(order2.isValid)
    }

    @Test
    fun testParseNodeList() {
        val nodes = listOf(
            "طلب جديد",
            "نقطة الركوب: شارع التحرير",
            "الوجهة: المعادي",
            "12.5 كم",
            "65.00 ج.م",
            "قبول"
        )
        val parsed = OrderParser.parse(nodes)
        assertNotNull(parsed.price)
        assertNotNull(parsed.distanceKm)
        assertEquals(65.0, parsed.price!!, 0.001)
        assertEquals(12.5, parsed.distanceKm!!, 0.001)
        assertTrue(parsed.isValid)
    }
}

