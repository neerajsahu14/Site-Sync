package com.a1.sitesync

import android.app.Application
import androidx.room.Room
import com.a1.sitesync.data.database.AppDatabase
import com.a1.sitesync.di.appModule
import com.a1.sitesync.di.authModule
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
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

        // Initialize PDFBox
        PDFBoxResourceLoader.init(applicationContext)

        // Initialize Firebase and App Check
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        // Initialize Koin
        startKoin {
            androidLogger()
            androidContext(this@MainApp)
            modules(appModule, authModule)
        }
    }
}
