package com.yoshi0311.sharedledger.navigation

sealed class Routes(val route: String) {
    object Splash : Routes("splash")
    object Login : Routes("login")
    object Signup : Routes("signup")
    object Home : Routes("home")

    // id = -1 → 추가 모드 / id > 0 → 수정 모드
    object TransactionEdit : Routes("transaction_edit?id={id}") {
        fun createRoute(id: Long = -1L) = "transaction_edit?id=$id"
    }
}
