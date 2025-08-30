package com.a1.sitesync

import android.app.Application
import androidx.room.Room
import com.a1.sitesync.data.database.AppDatabase
import com.a1.sitesync.di.appModule
import com.cloudinary.android.MediaManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApp : Application() {
    companion object {
        lateinit var instance: MainApp
            private set
    }

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        instance = this
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "site_sync_db"
        ).build()
        startKoin {
            androidLogger()
            androidContext(this@MainApp)
            modules(appModule)
        }
        val config = mapOf(
            "cloud_name" to "YOUR_CLOUD_NAME",
            "api_key" to "YOUR_API_KEY",
            "api_secret" to "YOUR_API_SECRET"
        )
        MediaManager.init(this, config)
    }
}
