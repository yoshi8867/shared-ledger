package com.yoshi0311.sharedledger.service.autofill.parsers

import com.yoshi0311.sharedledger.service.autofill.BaseKoreanFinanceParser
import com.yoshi0311.sharedledger.service.autofill.ParsedTransaction

/**
 * 카카오페이 파서 (카카오페이 앱 + 카카오톡 내 알림)
 *
 * 예시 알림:
 *  - "이마트에서 10,000원을 결제했습니다."  (body)
 *  - "카카오페이 결제" (title) + "이마트 10,000원 결제" (body)
 *  - "카카오뱅크" (title) + "이마트 10,000원 출금" (body)
 */
class KakaoPayParser : BaseKoreanFinanceParser() {

    override val packageNames = listOf(
        "com.kakaocorp.mobile.kakaopay", // 카카오페이 앱
        "com.kakao.talk",                 // 카카오톡 (카카오페이 알림 경유)
    )

    override fun parse(title: String?, body: String): ParsedTransaction {
        val text = "${title ?: ""} $body"
        val amount = extractAmount(text)
        val type = detectType(text)
        // "에서 X원을 결제" 패턴에서 상호명 추출 시도
        val merchantFromPattern = MERCHANT_REGEX.find(body)?.groupValues?.getOrNull(1)?.trim()
        val description = merchantFromPattern?.takeIf { it.length >= 2 }
            ?: extractDescription(body)
        return ParsedTransaction(amount, type, null, description)
    }

    companion object {
        // "XXX에서 N원" 패턴에서 XXX 추출
        private val MERCHANT_REGEX = Regex("(.+?)에서\\s*[\\d,]+원")
    }
}
