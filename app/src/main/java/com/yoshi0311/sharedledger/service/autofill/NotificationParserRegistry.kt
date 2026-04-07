package com.yoshi0311.sharedledger.service.autofill

import com.yoshi0311.sharedledger.service.autofill.parsers.GenericKoreanFinanceParser
import com.yoshi0311.sharedledger.service.autofill.parsers.HanaCardParser
import com.yoshi0311.sharedledger.service.autofill.parsers.KBBankParser
import com.yoshi0311.sharedledger.service.autofill.parsers.KakaoBankParser
import com.yoshi0311.sharedledger.service.autofill.parsers.KakaoPayParser
import com.yoshi0311.sharedledger.service.autofill.parsers.ShinhanCardParser
import com.yoshi0311.sharedledger.service.autofill.parsers.TossParser

data class SupportedApp(val packageName: String, val displayName: String)

object NotificationParserRegistry {

    /** 수집 대상 앱 목록 (패키지 선택 모달에 표시) */
    val SUPPORTED_APPS: List<SupportedApp> = listOf(
        SupportedApp("com.kbstar.kbbank",                          "KB국민은행"),
        SupportedApp("com.kbcard.kbfinancegroup",                  "KB국민카드"),
        SupportedApp("com.hanacard.mobilecard",                    "하나카드"),
        SupportedApp("com.hanabank.ebk.channel.android.hananbank", "하나은행"),
        SupportedApp("com.hanaskcard.paycla",                      "하나페이"),
        SupportedApp("com.shcard.shinhancard",                     "신한카드"),
        SupportedApp("com.shinhan.smartcaremng",                   "신한SOL"),
        SupportedApp("com.kakaocorp.mobile.kakaopay",              "카카오페이"),
        SupportedApp("com.kakao.talk",                             "카카오톡(카카오페이)"),
        SupportedApp("com.kakaobank.channel",                      "카카오뱅크"),
        SupportedApp("viva.republica.toss",                        "토스"),
    )

    /** 기본 활성화 패키지 (최초 설치 시) */
    val DEFAULT_ENABLED: Set<String> = SUPPORTED_APPS.map { it.packageName }.toSet()

    private val parsers: List<NotificationParser> = listOf(
        KBBankParser(),
        KakaoBankParser(),
        HanaCardParser(),
        ShinhanCardParser(),
        KakaoPayParser(),
        TossParser(),
    )
    private val genericParser = GenericKoreanFinanceParser()

    fun parse(packageName: String, title: String?, body: String): ParsedTransaction =
        parsers.find { it.packageNames.contains(packageName) }
            ?.parse(title, body)
            ?: genericParser.parse(title, body)

    /** SMS는 항상 generic 파서 사용 */
    fun parseForSms(body: String): ParsedTransaction =
        genericParser.parse(null, body)

    /** 패키지명 → 표시용 앱 이름 (없으면 원래 문자열 반환) */
    fun displayName(packageNameOrSender: String): String =
        SUPPORTED_APPS.find { it.packageName == packageNameOrSender }?.displayName
            ?: packageNameOrSender
}
