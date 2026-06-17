package com.duisternis.voidgrid.di

import com.duisternis.voidgrid.data.api.DuckDuckGoApi
import com.duisternis.voidgrid.data.api.RetrofitClient
import com.duisternis.voidgrid.data.parser.SearchParser
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import com.duisternis.voidgrid.ui.viewmodel.ImageSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// ─── Módulo de injeção de dependências ───────────────────────────────────────

val appModule = module {
    // Parser isolado para testes
    single { SearchParser() }

    // API
    single<DuckDuckGoApi> { RetrofitClient.duckDuckGoApi }

    // Repositório agora recebe API e o novo Parser
    single { ImageSearchRepository(get(), get()) }

    // ViewModel injetado
    viewModel { ImageSearchViewModel(get()) }
}