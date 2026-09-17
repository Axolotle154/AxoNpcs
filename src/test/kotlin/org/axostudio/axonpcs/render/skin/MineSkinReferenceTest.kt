package org.axostudio.axonpcs.render.skin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MineSkinReferenceTest {

    @Test
    fun `parses current MineSkin links and explicit ids`() {
        val full = parseMineSkinReference("https://v2.mineskin.org/skins/47e363642c7245a192ab50a6391a061a")
        assertEquals("47e363642c7245a192ab50a6391a061a", full?.id)
        assertFalse(full!!.legacy)

        val short = parseMineSkinReference("mineskin:8162a747")
        assertEquals("8162a747", short?.id)
        assertFalse(short!!.legacy)

        val mineskInFull = parseMineSkinReference("https://minesk.in/c559889a89004fa297a8c08fb9337898")
        assertEquals("c559889a89004fa297a8c08fb9337898", mineskInFull?.id)
        assertFalse(mineskInFull!!.legacy)

        val mineskInShort = parseMineSkinReference("https://minesk.in/85b75d0c")
        assertEquals("85b75d0c", mineskInShort?.id)
        assertFalse(mineskInShort!!.legacy)
    }

    @Test
    fun `parses legacy links without accepting unrelated hosts`() {
        val legacy = parseMineSkinReference("https://mineskin.org/42903")
        assertEquals("42903", legacy?.id)
        assertTrue(legacy!!.legacy)

        assertNull(parseMineSkinReference("https://example.com/42903"))
        assertNull(parseMineSkinReference("Notch"))
    }
}
