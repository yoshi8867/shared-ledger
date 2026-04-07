package com.yoshi0311.sharedledger.service.autofill.parsers

import com.yoshi0311.sharedledger.service.autofill.BaseKoreanFinanceParser
import com.yoshi0311.sharedledger.service.autofill.ParsedTransaction

/**
 * KB국민은행 / KB스타뱅킹 / KB Pay 파서
 *
 * 예시 알림:
 *  - "[국민은행] 체크카드(1234) 이마트 10,000원 승인 잔액 200,000원"
 *  - "KB Pay 결제 이마트 10,000원 완료"
 *  - "[KB국민카드] 이마트 10,000원 일시불 승인"
 */
class KBBankParser : BaseKoreanFinanceParser() {

    override val packageNames = listOf(
        "com.kbstar.kbbank",        // KB스타뱅킹
        "com.kbcard.kbfinancegroup", // KB국민카드
        "com.kbcard.cxh.apk"        // KB카드 구버전
    )

    override fun parse(title: String?, body: String): ParsedTransaction {
        val text = "${title ?: ""} $body"
        val amount = extractAmount(text)
        val type = detectType(text)
        val description = extractKBDescription(body)
        return ParsedTransaction(amount, type, null, description)
    }

    private fun extractKBDescription(body: String): String? {
        // KB 알림에서 카드번호 패턴 제거 후 상호명 추출
        val cleaned = body.replace(Regex("\\(\\d{4}\\)"), "")
        return extractDescription(cleaned)
    }
}
