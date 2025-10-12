package com.app.railnav

import android.app.Application
import org.osmdroid.config.Configuration

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // This line sets up the OSMDroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
    }
}