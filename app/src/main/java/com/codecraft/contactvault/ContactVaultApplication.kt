package com.codecraft.contactvault

import android.app.Application
import com.codecraft.contactvault.domain.ads.AdManager

class ContactVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdManager.initialize(this)
    }
}
