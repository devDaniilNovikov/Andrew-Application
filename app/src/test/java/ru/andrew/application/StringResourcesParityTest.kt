package ru.andrew.application

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Русские строки лежат и в values/, и в values-ru/. На русской локали Android берёт values-ru,
 * поэтому правка только values/ не доходит до пользователя. Тест не даёт файлам разойтись.
 */
class StringResourcesParityTest {

    @Test
    fun russianStringsMatchDefaultStrings() {
        val defaultStrings = readStrings("src/main/res/values/strings.xml")
        val russianStrings = readStrings("src/main/res/values-ru/strings.xml")

        val problems = buildList<String> {
            (defaultStrings.keys - russianStrings.keys).sorted()
                .forEach { add("нет в values-ru: $it") }
            (russianStrings.keys - defaultStrings.keys).sorted()
                .forEach { add("нет в values: $it") }
            (defaultStrings.keys intersect russianStrings.keys).sorted()
                .filter { defaultStrings[it] != russianStrings[it] }
                .forEach { add("$it: values=\"${defaultStrings[it]}\", values-ru=\"${russianStrings[it]}\"") }
        }

        assertTrue(
            "values/strings.xml и values-ru/strings.xml разошлись:\n" + problems.joinToString("\n"),
            problems.isEmpty()
        )
    }

    private fun readStrings(path: String): Map<String, String> {
        val file = File(path)
        assertTrue("Не найден файл ресурсов: ${file.absolutePath}", file.exists())

        val document = DocumentBuilderFactory.newInstance()
            .apply { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            .newDocumentBuilder()
            .parse(file)

        val nodes = document.documentElement.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent }
    }
}
