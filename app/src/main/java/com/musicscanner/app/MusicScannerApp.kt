package com.musicscanner.app

import android.app.Application

class MusicScannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MusicScannerApp
            private set
    }
}
