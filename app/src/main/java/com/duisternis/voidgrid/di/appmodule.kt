package com.duisternis.voidgrid.di

import com.duisternis.voidgrid.data.api.DuckDuckGoApi
import com.duisternis.voidgrid.data.api.RetrofitClient
import com.duisternis.voidgrid.data.local.dao.PinsDao
import com.duisternis.voidgrid.data.local.ProviderPreferences
import com.duisternis.voidgrid.data.local.VoidGridDatabase
import com.duisternis.voidgrid.data.parser.SearchParser
import com.duisternis.voidgrid.data.repository.FavoritesRepository
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import com.duisternis.voidgrid.ui.viewmodel.FavoritesViewModel
import com.duisternis.voidgrid.ui.viewmodel.ForYouViewModel
import com.duisternis.voidgrid.ui.viewmodel.ImageSearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { SearchParser() }
    single<DuckDuckGoApi> { RetrofitClient.duckDuckGoApi }
    single { ImageSearchRepository(get(), get()) }
    single { ProviderPreferences(androidContext()) }

    // Room
    single { VoidGridDatabase.create(androidContext()) }
    single<PinsDao> { get<VoidGridDatabase>().pinsDao() }
    single { FavoritesRepository(get()) }

    // ViewModels
    viewModel { ImageSearchViewModel(get(), get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { ForYouViewModel(get()) }
}