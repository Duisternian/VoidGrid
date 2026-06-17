package com.duisternis.voidgrid
import com.duisternis.voidgrid.data.parser.SearchParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchParserTest {

    private val parser = SearchParser()

    // ─── Casos de sucesso ─────────────────────────────────────────────────────

    @Test
    fun `quando json valido deve retornar lista de itens`() {
        val json = """
        {
            "results": [
                { "image": "http://test.com/img.jpg", "source": "Test", "width": 100, "height": 200 }
            ],
            "next": "s=20&other=param"
        }
        """.trimIndent()

        val (items, nextS) = parser.parse(json)

        assertEquals(1, items.size)
        assertEquals("http://test.com/img.jpg", items[0].link)
        assertEquals(20, nextS)
    }

    @Test
    fun `deve parsear multiplos itens corretamente`() {
        val json = """
        {
            "results": [
                { "image": "http://a.com/1.jpg", "source": "A", "width": 100, "height": 100 },
                { "image": "http://b.com/2.jpg", "source": "B", "width": 200, "height": 200 },
                { "image": "http://c.com/3.jpg", "source": "C", "width": 300, "height": 300 }
            ]
        }
        """.trimIndent()

        val (items, _) = parser.parse(json)

        assertEquals(3, items.size)
    }

    @Test
    fun `deve converter source para lowercase`() {
        val json = """
        {
            "results": [
                { "image": "http://test.com/img.jpg", "source": "REDDIT", "width": 100, "height": 100 }
            ]
        }
        """.trimIndent()

        val (items, _) = parser.parse(json)

        assertEquals("reddit", items[0].source)
    }

    @Test
    fun `quando width e height ausentes deve usar zero como default`() {
        val json = """
        {
            "results": [
                { "image": "http://test.com/img.jpg", "source": "test" }
            ]
        }
        """.trimIndent()

        val (items, _) = parser.parse(json)

        assertEquals(1, items.size)
        assertEquals(0, items[0].width)
        assertEquals(0, items[0].height)
    }

    @Test
    fun `quando source ausente deve usar unknown como default`() {
        val json = """
        {
            "results": [
                { "image": "http://test.com/img.jpg", "width": 100, "height": 100 }
            ]
        }
        """.trimIndent()

        val (items, _) = parser.parse(json)

        assertEquals("unknown", items[0].source)
    }

    // ─── Casos de paginação ───────────────────────────────────────────────────

    @Test
    fun `quando next ausente deve retornar nextS nulo`() {
        val json = """{ "results": [] }""".trimIndent()

        val (_, nextS) = parser.parse(json)

        assertNull(nextS)
    }

    @Test
    fun `deve extrair nextS corretamente do campo next`() {
        val json = """
        {
            "results": [],
            "next": "v7_b28b8d4ea1234abc&q=gatos&s=40&o=json"
        }
        """.trimIndent()

        val (_, nextS) = parser.parse(json)

        assertEquals(40, nextS)
    }

    // ─── Casos de filtragem ───────────────────────────────────────────────────

    @Test
    fun `deve ignorar itens sem link de imagem`() {
        val json = """
        {
            "results": [
                { "source": "test", "width": 100, "height": 100 },
                { "image": "http://valido.com/img.jpg", "source": "test", "width": 100, "height": 100 }
            ]
        }
        """.trimIndent()

        val (items, _) = parser.parse(json)

        assertEquals(1, items.size)
        assertEquals("http://valido.com/img.jpg", items[0].link)
    }

    @Test
    fun `deve ignorar itens com link que nao começa com http`() {
        val json = """
        {
            "results": [
                { "image": "data:image/png;base64,abc", "source": "test", "width": 100, "height": 100 },
                { "image": "http://valido.com/img.jpg", "source": "test", "width": 100, "height": 100 }
            ]
        }
        """.trimIndent()

        val (items, _) = parser.parse(json)

        assertEquals(1, items.size)
    }

    // ─── Casos de erro ────────────────────────────────────────────────────────

    @Test
    fun `quando json invalido deve retornar lista vazia e nextS nulo`() {
        val (items, nextS) = parser.parse("{ erro sem aspas }")

        assertTrue(items.isEmpty())
        assertNull(nextS)
    }

    @Test
    fun `quando json vazio deve retornar lista vazia`() {
        val (items, nextS) = parser.parse("{}")

        assertTrue(items.isEmpty())
        assertNull(nextS)
    }

    @Test
    fun `quando string vazia deve retornar lista vazia`() {
        val (items, nextS) = parser.parse("")

        assertTrue(items.isEmpty())
        assertNull(nextS)
    }

    @Test
    fun `quando results e array vazio deve retornar lista vazia`() {
        val json = """{ "results": [] }"""

        val (items, nextS) = parser.parse(json)

        assertTrue(items.isEmpty())
        assertNull(nextS)
    }
}