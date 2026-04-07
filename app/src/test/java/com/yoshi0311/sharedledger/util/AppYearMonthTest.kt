package com.yoshi0311.sharedledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AppYearMonthTest {

    @Test
    fun `format returns YYYY-MM with zero-padded month`() {
        assertEquals("2026-04", AppYearMonth(2026, 4).format())
        assertEquals("2026-12", AppYearMonth(2026, 12).format())
        assertEquals("2026-01", AppYearMonth(2026, 1).format())
    }

    @Test
    fun `displayLabel returns year-month in Korean`() {
        assertEquals("2026년 4월", AppYearMonth(2026, 4).displayLabel())
        assertEquals("2025년 12월", AppYearMonth(2025, 12).displayLabel())
    }

    @Test
    fun `next increments month normally`() {
        assertEquals(AppYearMonth(2026, 5), AppYearMonth(2026, 4).next())
        assertEquals(AppYearMonth(2026, 2), AppYearMonth(2026, 1).next())
    }

    @Test
    fun `next wraps December to January of next year`() {
        assertEquals(AppYearMonth(2027, 1), AppYearMonth(2026, 12).next())
    }

    @Test
    fun `prev decrements month normally`() {
        assertEquals(AppYearMonth(2026, 3), AppYearMonth(2026, 4).prev())
        assertEquals(AppYearMonth(2026, 11), AppYearMonth(2026, 12).prev())
    }

    @Test
    fun `prev wraps January to December of previous year`() {
        assertEquals(AppYearMonth(2025, 12), AppYearMonth(2026, 1).prev())
    }

    @Test
    fun `next then prev returns original`() {
        val original = AppYearMonth(2026, 6)
        assertEquals(original, original.next().prev())
    }

    @Test
    fun `prev then next returns original`() {
        val original = AppYearMonth(2026, 6)
        assertEquals(original, original.prev().next())
    }

    @Test
    fun `chaining 12 next calls advances exactly one year`() {
        var ym = AppYearMonth(2026, 1)
        repeat(12) { ym = ym.next() }
        assertEquals(AppYearMonth(2027, 1), ym)
    }

    @Test
    fun `two different months are not equal`() {
        assertNotEquals(AppYearMonth(2026, 4), AppYearMonth(2026, 5))
    }
}
