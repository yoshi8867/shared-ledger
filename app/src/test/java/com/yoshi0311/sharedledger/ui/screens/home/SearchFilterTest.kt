package com.yoshi0311.sharedledger.ui.screens.home

import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SearchFilterTest {

    private fun tx(
        type: String,
        amount: Long,
        desc: String,
        date: Date = Date(),
        categoryId: Long? = null
    ) = TransactionEntity(
        ledgerId = 1, type = type, amount = amount, date = date, time = "00:00",
        description = desc, categoryId = categoryId
    )

    @Test
    fun `type checkboxes act as OR`() {
        val income = tx("income", 1000, "월급")
        val expense = tx("expense", 1000, "커피")
        assertTrue(SearchFilter(income = true, expense = true).matches(income))
        assertTrue(SearchFilter(income = true, expense = true).matches(expense))
        assertTrue(SearchFilter(income = true, expense = false).matches(income))
        assertFalse(SearchFilter(income = true, expense = false).matches(expense))
        assertFalse(SearchFilter(income = false, expense = false).matches(income))
    }

    @Test
    fun `query matches description case-insensitively`() {
        val t = tx("income", 1000, "이번달 월급")
        assertTrue(SearchFilter(query = "월급").matches(t))
        assertTrue(SearchFilter(query = "  월급 ").matches(t))
        assertFalse(SearchFilter(query = "보너스").matches(t))
    }

    @Test
    fun `amount range is inclusive and open-ended`() {
        val t = tx("expense", 9000, "밥")
        assertTrue(SearchFilter(minAmount = 9000).matches(t))      // 이상
        assertFalse(SearchFilter(minAmount = 9001).matches(t))
        assertTrue(SearchFilter(maxAmount = 9000).matches(t))      // 이하
        assertFalse(SearchFilter(maxAmount = 8999).matches(t))
        assertTrue(SearchFilter(minAmount = 8000, maxAmount = 10000).matches(t))
    }

    @Test
    fun `date range is inclusive`() {
        val t = tx("expense", 1000, "밥", date = Date(1_000_000L))
        assertTrue(SearchFilter(startDate = 1_000_000L, endDate = 1_000_000L).matches(t))
        assertFalse(SearchFilter(startDate = 1_000_001L).matches(t))
        assertFalse(SearchFilter(endDate = 999_999L).matches(t))
    }

    @Test
    fun `category set acts as OR and empty means all`() {
        val a = tx("expense", 1000, "밥", categoryId = 5)
        val b = tx("expense", 1000, "차", categoryId = 7)
        assertTrue(SearchFilter().matches(a))                          // 비어있으면 전체
        assertTrue(SearchFilter(categoryIds = setOf(5, 7)).matches(a)) // OR
        assertTrue(SearchFilter(categoryIds = setOf(5, 7)).matches(b))
        assertFalse(SearchFilter(categoryIds = setOf(7)).matches(a))
    }

    @Test
    fun `conditions combine with AND`() {
        val t = tx("income", 220000, "3월 월급")
        // 검색어 + 수입/지출 모두 + 금액 200000~250000
        assertTrue(SearchFilter(query = "월급", minAmount = 200000, maxAmount = 250000).matches(t))
        assertFalse(SearchFilter(query = "월급", minAmount = 230000).matches(t)) // 금액 미달
    }
}
