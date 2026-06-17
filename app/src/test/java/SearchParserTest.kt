import com.duisternis.voidgrid.data.parser.SearchParser
import org.junit.Test
import kotlin.test.assertEquals

class SearchParserTest {

    private val parser = SearchParser()

    @Test
    fun `quando json e valido deve retornar lista de itens`() {
        val jsonMock = """
        {
            "results": [
                { "image": "http://test.com/img.jpg", "source": "test", "width": 100, "height": 100 }
            ],
            "next": "s=20"
        }
        """.trimIndent()

        val (items, nextS) = parser.parse(jsonMock)

        assertEquals(1, items.size)
        assertEquals("http://test.com/img.jpg", items[0].link)
        assertEquals(20, nextS)
    }

    @Test
    fun `quando json e invalido deve retornar lista vazia`() {
        val jsonInvalido = "{ \"erro\": \"json errado\" }"
        val (items, nextS) = parser.parse(jsonInvalido)

        assertEquals(0, items.size)
        assertEquals(null, nextS)
    }
}