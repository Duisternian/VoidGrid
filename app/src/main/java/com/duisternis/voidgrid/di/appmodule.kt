package com.duisternis.voidgrid.di

import coil.ImageLoader
import com.duisternis.voidgrid.data.api.DuckDuckGoApi
import com.duisternis.voidgrid.data.api.RetrofitClient
import com.duisternis.voidgrid.data.local.ProviderPreferences
import com.duisternis.voidgrid.data.local.VoidGridDatabase
import com.duisternis.voidgrid.data.local.dao.PinsDao
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

    // Data Layer: API, Parsers & Image Loader
    single { SearchParser() }
    single<DuckDuckGoApi> { RetrofitClient.duckDuckGoApi }
    single { ImageLoader.Builder(androidContext()).build() } // Adicionado para o Coil

    // Repositories
    single { ImageSearchRepository(get(), get()) }
    single {
        FavoritesRepository(
            dao = get(),
            imageLoader = get(),
            appContext = androidContext()
        )
    }

    // Local Storage / Database
    single { ProviderPreferences(androidContext()) }
    single { VoidGridDatabase.create(androidContext()) }
    single<PinsDao> { get<VoidGridDatabase>().pinsDao() }

    // ViewModels
    viewModel { ImageSearchViewModel(get(), get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { ForYouViewModel(get()) }
}