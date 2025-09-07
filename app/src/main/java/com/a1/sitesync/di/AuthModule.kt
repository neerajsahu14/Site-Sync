package com.a1.sitesync.di

import com.a1.sitesync.data.repository.AuthRepository
import com.a1.sitesync.ui.viewmodel.AuthViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single { Firebase.auth }
    single { Firebase.firestore }
    single { AuthRepository(get(), get()) }

    viewModel { AuthViewModel(get()) }
}