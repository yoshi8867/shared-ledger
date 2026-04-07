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
        // 계좌번호 패턴(343602-**-***066) 이후 ~ 거래유형 키워드 이전 텍스트 추출
        val accountMatch = ACCOUNT_REGEX.find(body)
        if (accountMatch != null) {
            val afterAccount = body.substring(accountMatch.range.last + 1).trim()
            val keywordMatch = TX_TYPE_REGEX.find(afterAccount)
            if (keywordMatch != null) {
                return afterAccount.substring(0, keywordMatch.range.first).trim()
                    .takeIf { it.isNotBlank() }
            }
        }
        return extractDescription(body)
    }

    companion object {
        private val ACCOUNT_REGEX  = Regex("""[\d*]+-\*+-[\d*]+""")
        private val TX_TYPE_REGEX  = Regex("""(스마트폰출금|인터넷이체|ATM출금|이체|출금|결제)""")
    }
}
