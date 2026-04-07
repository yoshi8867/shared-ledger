package com.yoshi0311.sharedledger.service.autofill.parsers

import com.yoshi0311.sharedledger.service.autofill.BaseKoreanFinanceParser
import com.yoshi0311.sharedledger.service.autofill.ParsedTransaction

/**
 * 토스 파서
 *
 * 예시 알림:
 *  - "이마트에서 10,000원 결제했어요"
 *  - "토스뱅크" (title) + "이마트 10,000원 출금"
 */
class TossParser : BaseKoreanFinanceParser() {

    override val packageNames = listOf(
        "viva.republica.toss",       // 토스
        "com.tossbank.depositaccount" // 토스뱅크
    )

    override fun parse(title: String?, body: String): ParsedTransaction {
        val text = "${title ?: ""} $body"
        val amount = extractAmount(text)
        val type = detectType(text)
        // "XXX에서 N원 결제" 패턴
        val merchantFromPattern = MERCHANT_REGEX.find(body)?.groupValues?.getOrNull(1)?.trim()
        val description = merchantFromPattern?.takeIf { it.length >= 2 }
            ?: extractDescription(body)
        return ParsedTransaction(amount, type, null, description)
    }

    companion object {
        private val MERCHANT_REGEX = Regex("(.+?)에서\\s*[\\d,]+원")
    }
}
