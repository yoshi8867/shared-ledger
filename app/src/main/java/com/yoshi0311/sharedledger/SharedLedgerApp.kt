package com.yoshi0311.sharedledger

import android.app.Application
import com.yoshi0311.sharedledger.auth.NaverSignInHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SharedLedgerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NaverSignInHelper.initialize(this)
    }
}
