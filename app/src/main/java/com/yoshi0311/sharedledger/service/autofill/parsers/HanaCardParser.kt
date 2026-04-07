package com.yoshi0311.sharedledger.service.autofill.parsers

import com.yoshi0311.sharedledger.service.autofill.BaseKoreanFinanceParser
import com.yoshi0311.sharedledger.service.autofill.ParsedTransaction

/**
 * 하나카드 / 하나은행 파서
 *
 * 예시 알림:
 *  - "[하나카드] 1234카드 이마트 10,000원 결제 승인"
 *  - "하나카드 이마트 10,000원 승인"
 */
class HanaCardParser : BaseKoreanFinanceParser() {

    override val packageNames = listOf(
        "com.hanacard.mobilecard",    // 하나카드
        "com.hanafinancial.hanacard", // 하나카드 구버전
        "com.hanamembers.hanacard",   // 하나멤버스
        "com.hanabank.ebk.channel.android.hananbank" // 하나은행
    )

    override fun parse(title: String?, body: String): ParsedTransaction {
        val text = "${title ?: ""} $body"
        val amount = extractAmount(text)
        val type = detectType(text)
        // 카드번호(4자리) 패턴 제거
        val cleaned = body.replace(Regex("\\d{4}카드"), "")
        val description = extractDescription(cleaned)
        return ParsedTransaction(amount, type, null, description)
    }
}
