package com.yoshi0311.sharedledger.service.autofill.parsers

import com.yoshi0311.sharedledger.service.autofill.BaseKoreanFinanceParser
import com.yoshi0311.sharedledger.service.autofill.ParsedTransaction

/**
 * 신한카드 / 신한SOL 파서
 *
 * 예시 알림:
 *  - "[신한카드] 이마트 10,000원 승인"
 *  - "[신한카드] 1234 이마트 10,000원 일시불 승인"
 *  - "신한SOL 이마트 10,000원 출금"
 */
class ShinhanCardParser : BaseKoreanFinanceParser() {

    override val packageNames = listOf(
        "com.shcard.shinhancard",     // 신한카드
        "com.shinhan.smartcaremng",   // 신한SOL
        "com.shinhan.sbanking"        // 신한은행
    )

    override fun parse(title: String?, body: String): ParsedTransaction {
        val text = "${title ?: ""} $body"
        val amount = extractAmount(text)
        val type = detectType(text)
        val description = extractDescription(body)
        return ParsedTransaction(amount, type, null, description)
    }
}
