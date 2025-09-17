package com.a1.sitesync.di

import androidx.room.Room
import com.a1.sitesync.data.database.AppDatabase
import com.a1.sitesync.data.repository.FirebaseSyncRepository
import com.a1.sitesync.data.repository.LocalGateRepository
import com.a1.sitesync.data.repository.SiteSyncRepository
import com.a1.sitesync.data.service.FirebaseSyncService
import com.a1.sitesync.ui.viewmodel.FirebaseSyncViewModel
import com.a1.sitesync.ui.viewmodel.OverlayViewModel
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Firebase Instances
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }

    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "site_sync_db"
        ).build()
    }

    // DAOs
    single { get<AppDatabase>().surveyDao() }

    // Services
    single { FirebaseSyncService(androidContext(), get()) }
    single { FirebaseSyncRepository() } // Assuming this has a no-arg constructor

    // Repositories
    single { SiteSyncRepository(androidContext(), get(), get()) }
    single { LocalGateRepository() }

    // ViewModels
    viewModel { SiteSyncViewModel(get()) }
    viewModel { FirebaseSyncViewModel(get()) }
    viewModel { OverlayViewModel(get()) }
}
