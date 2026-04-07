package com.yoshi0311.sharedledger.service.autofill

import java.util.Date

data class ParsedTransaction(
    val amount: Long?,
    val type: String?,          // "income" | "expense" | null → default expense
    val date: Date?,
    val description: String?
)

interface NotificationParser {
    val packageNames: List<String>
    fun parse(title: String?, body: String): ParsedTransaction
}

/** 모든 한국 금융 파서에서 공통으로 사용하는 유틸리티 */
abstract class BaseKoreanFinanceParser : NotificationParser {

    protected fun extractAmount(text: String): Long? =
        AMOUNT_REGEX.find(text)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toLongOrNull()

    protected fun detectType(text: String): String {
        val hasIncome = INCOME_KEYWORDS.any { text.contains(it) }
        return if (hasIncome) "income" else "expense"
    }

    /**
     * 금액·키워드·괄호 등을 제거하고 남은 문자를 상호명/내용으로 추출.
     * 2글자 미만이면 null 반환.
     */
    protected fun extractDescription(body: String): String? {
        var text = body
        text = text.replace(AMOUNT_REGEX, "")
        REMOVE_WORDS.forEach { text = text.replace(it, " ") }
        text = text.replace(Regex("[\\[\\]()\\.\\d\\-_]"), " ")
        text = text.replace(Regex("\\s+"), " ").trim()
        return text.takeIf { it.length >= 2 }
    }

    companion object {
        val AMOUNT_REGEX = Regex("(\\d[\\d,]*)원")
        private val INCOME_KEYWORDS = listOf("입금", "환급", "취소", "캐시백", "충전", "수신", "환불", "적립")
        private val REMOVE_WORDS = listOf(
            "결제", "승인", "완료", "했습니다", "했어요", "잔액", "출금", "입금", "이체",
            "사용", "체크카드", "신용카드", "카드", "은행", "승인번호", "에서", "으로",
            "에게", "합니다", "하였습니다", "번호", "KB", "Pay", "pay"
        )
    }
}
