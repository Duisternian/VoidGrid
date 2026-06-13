package com.duisternis.voidgrid

// 🟢 Definição ÚNICA do modelo de dados para todo o projeto
data class SearchItem(
    val link: String,
    val source: String,
    val image: String? = null // Mantido nulo por padrão para compatibilidade com o app antigo
)