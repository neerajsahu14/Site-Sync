package com.a1.sitesync.di

import androidx.room.Room
import com.a1.sitesync.data.database.AppDatabase
import com.a1.sitesync.data.database.dao.SurveyDao
import com.a1.sitesync.data.repository.FirebaseSyncRepository
import com.a1.sitesync.data.repository.SiteSyncRepository
import com.a1.sitesync.data.service.FirebaseSyncService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.a1.sitesync.ui.viewmodel.FirebaseSyncViewModel
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import  org.koin.core.module.dsl.*

val appModule = module {
    // Firebase instances
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> { FirebaseFirestore.getInstance() }
    single<FirebaseStorage> { FirebaseStorage.getInstance() }

    // Local Room database
    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "site_sync_db"
        ).build()
    }

    // DAO
    single<SurveyDao> { get<AppDatabase>().surveyDao() }

    // Services
    single { FirebaseSyncService(get()) }
    single<FirebaseSyncRepository> { FirebaseSyncRepository() }

    // Repositories
    single { SiteSyncRepository(get(), get(), get()) }

    // ViewModels
    viewModel { SiteSyncViewModel(get()) }
    viewModel { FirebaseSyncViewModel(get()) }
}