package com.yoshi0311.sharedledger.util

import java.util.Calendar

data class AppYearMonth(val year: Int, val month: Int) {

    fun format(): String = "%04d-%02d".format(year, month)

    fun displayLabel(): String = "${year}년 ${month}월"

    fun next(): AppYearMonth = if (month == 12) AppYearMonth(year + 1, 1)
    else AppYearMonth(year, month + 1)

    fun prev(): AppYearMonth = if (month == 1) AppYearMonth(year - 1, 12)
    else AppYearMonth(year, month - 1)

    companion object {
        fun now(): AppYearMonth {
            val cal = Calendar.getInstance()
            return AppYearMonth(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1
            )
        }
    }
}
