package com.yoshi0311.sharedledger.navigation

sealed class Routes(val route: String) {
    object Splash : Routes("splash")
    object Login : Routes("login")
    object Signup : Routes("signup")
    object Home : Routes("home")

    // id = -1 → 추가 모드 / id > 0 → 수정 모드
    // dateMillis = -1 → 오늘 / > 0 → 캘린더에서 선택한 날짜
    object TransactionEdit : Routes("transaction_edit?id={id}&dateMillis={dateMillis}") {
        fun createRoute(id: Long = -1L, dateMillis: Long = -1L) =
            "transaction_edit?id=$id&dateMillis=$dateMillis"
    }

    object SharedLedger : Routes("shared_ledger/{ledgerId}") {
        fun createRoute(ledgerId: Long) = "shared_ledger/$ledgerId"
    }

    object Settings : Routes("settings")
    object CategoryManage : Routes("category_manage")
    object AutoFill : Routes("auto_fill")
}
