package com.duisternis.voidgrid.data.model

/**
 * Representa uma query do feed "Para Você", já com seu filtro de cor associado
 * (se houver). O filtro de cor vem do pin de origem que gerou esta query
 * (ver ForYouViewModel.buildQueryForFolder), e é repassado como parâmetro
 * separado `f=,,,,,color:X` na chamada à API interna do DuckDuckGo —
 * NUNCA concatenado dentro do texto da busca.
 */
data class ForYouQuery(
    val text: String,
    val colorFilter: String? = null
)