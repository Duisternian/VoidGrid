package com.duisternis.voidgrid.data.model

data class SearchItem(
    val link: String,
    val source: String,
    val width: Int = 0,
    val height: Int = 0,
    val thumbnail: String? = null,
    val title: String? = null
) {
    // URLs vindas da API/scraper podem ter espaços (ex: "foto final.jpg"),
    // o que quebra o carregamento via Coil. Normalizamos uma única vez aqui,
    // como propriedade computada, em vez de repetir o replace em cada tela.
    val encodedLink: String
        get() = link.replace(" ", "%20")

    val encodedThumbnail: String?
        get() = thumbnail?.replace(" ", "%20")
}