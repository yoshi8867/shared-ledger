package com.yoshi0311.sharedledger.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerUrlProvider @Inject constructor() {
    companion object {
        const val DEFAULT_URL = "http://172.30.5.241:3000/"
    }

    @Volatile
    var baseUrl: String = DEFAULT_URL
}
