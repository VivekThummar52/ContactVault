package com.codecraft.contactvault

import android.app.Application
import com.codecraft.contactvault.domain.ads.AdManager
import com.codecraft.contactvault.domain.ads.AppOpenAdManager

class ContactVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdManager.initialize(this)
        AppOpenAdManager.initialize(this)
    }
}
