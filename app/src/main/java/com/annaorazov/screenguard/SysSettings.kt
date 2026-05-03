package com.annaorazov.screenguard

import android.app.Application
import android.content.Context

class SysSettings : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(base))
    }
}
