package at.sellemond.software.rechnung_generator_spring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File

internal class RechnungenFolderCreatorTest {

    private val testKundePrefix = "junit"
    private val getKundeWithPrefix = { name: String -> "$testKundePrefix$name" }

    @AfterEach
    internal fun tearDown() {
        File(RechnungenFolderCreator.destination).listFiles()?.asList()
            ?.filter { it.name.startsWith(testKundePrefix) }
            ?.forEach {
                println("deleting file ${it.name}")
                it.deleteRecursively()
            }
    }

    @Test
    internal fun `given no rechnungen present then folder structure is generated`() {
        val givenKunde = getKundeWithPrefix("a")
        RechnungenFolderCreator.createFolderIfNotPresent(givenKunde)
        assertThat(File(RechnungenFolderCreator.getFileDestination(givenKunde)).exists()).isTrue
    }

    @Test
    internal fun `given rechnungen already present when creating rechnung for new kunde then new folder is created`() {
        listOf("a", "b").map { getKundeWithPrefix(it) }.forEach {
            RechnungenFolderCreator.createFolderIfNotPresent(it)
            assertThat(File(RechnungenFolderCreator.getFileDestination(it)).exists()).isTrue
        }
    }
}