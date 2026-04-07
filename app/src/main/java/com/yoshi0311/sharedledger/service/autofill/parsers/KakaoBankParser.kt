package com.yoshi0311.sharedledger.service.autofill.parsers

import com.yoshi0311.sharedledger.service.autofill.BaseKoreanFinanceParser
import com.yoshi0311.sharedledger.service.autofill.ParsedTransaction

/**
 * 카카오뱅크 파서 (com.kakaobank.channel)
 *
 * 출금 알림 예시:
 *   title: "출금 20,000원"
 *   body:  "장*호님 04/07 21:36 343602-**-***066 야호 스마트폰출금 20,000 잔액11,100,000"
 *   → description: "야호"  (계좌번호 패턴 이후 ~ 거래유형 키워드 이전)
 *
 * 입금 알림 예시:
 *   title: "입금 20,000원"
 *   body:  "간다요 → 입출금통장(7887)    20000"
 *   → description: "간다요"  (→ 왼쪽)
 */
class KakaoBankParser : BaseKoreanFinanceParser() {

    override val packageNames = listOf("com.kakaobank.channel")

    override fun parse(title: String?, body: String): ParsedTransaction {
        val text = "${title ?: ""} $body"
        val amount = extractAmount(text)
        val type = detectType(text)
        val description = extractKakaoBankDescription(body)
        return ParsedTransaction(amount, type, null, description)
    }

    private fun extractKakaoBankDescription(body: String): String? {
        // → 가 있으면 양쪽 중 계좌명(숫자) 패턴이 없는 쪽이 description
        // 입금: "간다요 → 입출금통장(7887)    20000"  → 왼쪽
        // 출금: "입출금통장(7887) → 간다요"           → 오른쪽
        if (body.contains('→')) {
            val left  = body.substringBefore('→').trim()
            val right = body.substringAfter('→').trim()
                .replace(AMOUNT_REGEX, "").trim()   // 금액 제거
            return if (ACCOUNT_NAME_REGEX.containsMatchIn(left)) right.takeIf { it.isNotBlank() }
            else left.takeIf { it.isNotBlank() }
        }

        // 출금 형식: "xxx님 날짜 시간 계좌번호 내용 거래유형 금액 잔액..."
        // 계좌번호 패턴 (예: 343602-**-***066) 이후 텍스트에서 추출
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
        // 마스킹된 계좌번호 패턴: 343602-**-***066
        private val ACCOUNT_REGEX = Regex("""[\d*]+-\*+-[\d*]+""")
        private val TX_TYPE_REGEX = Regex("""(스마트폰출금|인터넷이체|ATM출금|이체|출금|결제)""")
        // 계좌명 패턴: "입출금통장(7887)" 처럼 뒤에 (숫자)가 붙음
        private val ACCOUNT_NAME_REGEX = Regex("""\(\d+\)""")
    }
}
