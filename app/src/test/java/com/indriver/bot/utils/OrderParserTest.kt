package com.indriver.bot.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderParserTest {

    @Test
    fun testParseMoroccanDirham() {
        // Moroccan Dirham in various representations
        assertEquals(35.0, OrderParser.parsePrice("35 DH")!!, 0.001)
        assertEquals(35.0, OrderParser.parsePrice("35DH")!!, 0.001)
        assertEquals(45.5, OrderParser.parsePrice("45.50 DH")!!, 0.001)
        assertEquals(45.5, OrderParser.parsePrice("45,50 DH")!!, 0.001)
        assertEquals(60.0, OrderParser.parsePrice("60 MAD")!!, 0.001)
        assertEquals(25.0, OrderParser.parsePrice("25 د.م")!!, 0.001)
        assertEquals(25.0, OrderParser.parsePrice("25 د.م.")!!, 0.001)
        assertEquals(50.0, OrderParser.parsePrice("50 درهم")!!, 0.001)
        assertEquals(70.0, OrderParser.parsePrice("درهم 70")!!, 0.001)
        assertEquals(85.0, OrderParser.parsePrice("DH 85")!!, 0.001)

        // Moroccan Dirham with Arabic digits
        assertEquals(35.0, OrderParser.parsePrice("٣٥ د.م")!!, 0.001)
        assertEquals(35.0, OrderParser.parsePrice("٣٥ درهم")!!, 0.001)
        assertEquals(35.5, OrderParser.parsePrice("٣٥٫٥ د.م")!!, 0.001)
    }

    @Test
    fun testParseArabicIndicDigits() {
        val distDot = OrderParser.parseDistance("١٢.٥ كم")
        assertEquals(12.5, distDot!!, 0.001)

        val distComma = OrderParser.parseDistance("١٢,٥ كم")
        assertEquals(12.5, distComma!!, 0.001)

        val distArabicSep = OrderParser.parseDistance("١٢٫٥ كم")
        assertEquals(12.5, distArabicSep!!, 0.001)

        val price1 = OrderParser.parsePrice("٥٠ د.م")
        assertEquals(50.0, price1!!, 0.001)

        val price2 = OrderParser.parsePrice("٥٠٫٥ د.م")
        assertEquals(50.5, price2!!, 0.001)
    }

    @Test
    fun testParseDistanceFormats() {
        val dist1 = OrderParser.parseDistance("12,5 km")
        assertEquals(12.5, dist1!!, 0.001)

        val dist2 = OrderParser.parseDistance("12.5 كم")
        assertEquals(12.5, dist2!!, 0.001)

        val dist3 = OrderParser.parseDistance("12.5km")
        assertEquals(12.5, dist3!!, 0.001)

        val dist4 = OrderParser.parseDistance("12 كم")
        assertEquals(12.0, dist4!!, 0.001)

        val distMeters1 = OrderParser.parseDistance("800 m")
        assertEquals(0.8, distMeters1!!, 0.001)

        val distMeters2 = OrderParser.parseDistance("500 متر")
        assertEquals(0.5, distMeters2!!, 0.001)
    }

    @Test
    fun testMissingValues() {
        assertNull(OrderParser.parseDistance(""))
        assertNull(OrderParser.parsePrice(""))
        assertNull(OrderParser.parseDistance("شارع الحسن الثاني، الدار البيضاء"))
        assertNull(OrderParser.parsePrice("شارع الحسن الثاني، الدار البيضاء"))

        val order1 = OrderParser.parse("12.5 كم")
        assertNull(order1.price)
        assertEquals(12.5, order1.distanceKm!!, 0.001)
        assertFalse(order1.isValid)

        val order2 = OrderParser.parse("50 د.م")
        assertEquals(50.0, order2.price!!, 0.001)
        assertNull(order2.distanceKm)
        assertFalse(order2.isValid)
    }

    @Test
    fun testParseMoroccanOrderNodeList() {
        val nodes = listOf(
            "طلب رحلة جديد",
            "الانطلاق: شارع محمد الخامس",
            "الوجهة: المعاريف",
            "4.5 كم",
            "35.00 DH",
            "قبول"
        )
        val parsed = OrderParser.parse(nodes)
        assertNotNull(parsed.price)
        assertNotNull(parsed.distanceKm)
        assertEquals(35.0, parsed.price!!, 0.001)
        assertEquals(4.5, parsed.distanceKm!!, 0.001)
        assertTrue(parsed.isValid)
    }
}
