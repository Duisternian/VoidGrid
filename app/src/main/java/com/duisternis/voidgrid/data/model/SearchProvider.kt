package com.duisternis.voidgrid.data.model

/**
 * Representa o provedor de busca selecionado pelo usuário.
 *
 * "All" é o padrão — busca sem nenhum filtro de site:.
 * Os provedores fixos mapeiam para um domínio conhecido.
 * "Custom" carrega um domínio arbitrário digitado pelo usuário (persistido em DataStore).
 *
 * O `siteFilter` é o que vira "site:dominio.com" — usado SÓ na hora de montar a
 * query que vai pra API, nunca aparece no texto que o usuário vê/edita na barra.
 */
sealed class SearchProvider(
    val id: String,
    val label: String,
    val domain: String?
) {
    val siteFilter: String?
        get() = domain?.let { "site:$it" }

    data object All : SearchProvider(id = "all", label = "Padrão", domain = null)
    data object Pinterest : SearchProvider(id = "pinterest", label = "Pinterest", domain = "pinterest.com")
    data object Reddit : SearchProvider(id = "reddit", label = "Reddit", domain = "reddit.com")
    data object Letterboxd : SearchProvider(id = "letterboxd", label = "Letterboxd", domain = "letterboxd.com")
    data object Wikipedia : SearchProvider(id = "wikipedia", label = "Wikipedia", domain = "wikipedia.org")
    data object X : SearchProvider(id = "x", label = "X (Twitter)", domain = "x.com")

    data class Custom(val customDomain: String) :
        SearchProvider(id = "custom:$customDomain", label = customDomain, domain = customDomain)

    companion object {
        // Providers fixos, na ordem que devem aparecer no dropdown (sem contar Custom salvos)
        // `by lazy` evita problema de ordem de inicialização estática: como este
        // companion object pertence à própria sealed class, referenciar os
        // `data object` filhos (All, Pinterest, ...) diretamente em um `val`
        // pode rodar antes deles existirem, retornando null na lista.
        val builtIn: List<SearchProvider> by lazy { listOf(All, Pinterest, Reddit, Letterboxd, Wikipedia, X) }

        /**
         * Reconstrói um SearchProvider a partir do id salvo (DataStore).
         * Usado pra restaurar o provider selecionado entre sessões.
         */
        fun fromId(id: String, savedCustomDomains: List<String>): SearchProvider {
            builtIn.firstOrNull { it.id == id }?.let { return it }
            if (id.startsWith("custom:")) {
                val domain = id.removePrefix("custom:")
                if (domain in savedCustomDomains) return Custom(domain)
            }
            return All
        }
    }
}