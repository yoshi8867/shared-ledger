package com.yoshi0311.sharedledger.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerUrlProvider @Inject constructor() {
    companion object {
        const val DEFAULT_URL = "https://shared-ledger-api.onrender.com/"
    }

    @Volatile
    var baseUrl: String = DEFAULT_URL
}
