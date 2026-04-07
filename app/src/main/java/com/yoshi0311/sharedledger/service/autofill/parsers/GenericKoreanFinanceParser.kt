package com.yoshi0311.sharedledger.service.autofill.parsers

import com.yoshi0311.sharedledger.service.autofill.BaseKoreanFinanceParser
import com.yoshi0311.sharedledger.service.autofill.ParsedTransaction

/**
 * 등록되지 않은 금융 앱 / SMS 공통 폴백 파서.
 * "XXX원" 패턴이 있으면 금액을 추출하고, 타입/상호명을 최선으로 추정한다.
 */
class GenericKoreanFinanceParser : BaseKoreanFinanceParser() {

    override val packageNames: List<String> = emptyList() // Registry에서 직접 사용 안 함

    override fun parse(title: String?, body: String): ParsedTransaction {
        val text = "${title ?: ""} $body"
        val amount = extractAmount(text)
        val type = detectType(text)
        val description = extractDescription(body)
        return ParsedTransaction(amount, type, null, description)
    }
}
