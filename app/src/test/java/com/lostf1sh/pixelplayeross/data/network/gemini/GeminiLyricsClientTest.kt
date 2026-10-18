package com.lostf1sh.pixelplayeross.data.network.gemini

import com.google.gson.Gson
import com.lostf1sh.pixelplayeross.data.model.GeminiLyricItem
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GeminiLyricsClientTest {

    private lateinit var client: GeminiLyricsClient
    private val gson = Gson()

    @BeforeEach
    fun setUp() {
        client = GeminiLyricsClient(OkHttpClient(), gson)
    }

    @Test
    fun parseStructuredLyricsJson_withStandardJson_parsesCorrectly() {
        val json = """
            [
                {
                    "original": "こんにちは",
                    "romanized": "Konnichiwa",
                    "translated": "Hello"
                },
                {
                    "original": "さようなら",
                    "romanized": "Sayounara",
                    "translated": "Goodbye"
                }
            ]
        """.trimIndent()

        val items = client.parseStructuredLyricsJson(json)
        assertNotNull(items)
        assertEquals(2, items?.size)
        assertEquals("こんにちは", items?.get(0)?.original)
        assertEquals("Konnichiwa", items?.get(0)?.romanized)
        assertEquals("Hello", items?.get(0)?.translated)
        assertEquals("さようなら", items?.get(1)?.original)
        assertEquals("Sayounara", items?.get(1)?.romanized)
        assertEquals("Goodbye", items?.get(1)?.translated)
    }

    @Test
    fun parseStructuredLyricsJson_withMarkdownFences_stripsFencesAndParses() {
        val markdownJson = """
            ```json
            [
                {
                    "original": "你好",
                    "romanized": "Nǐ hǎo",
                    "translated": "Hello"
                }
            ]
            ```
        """.trimIndent()

        val items = client.parseStructuredLyricsJson(markdownJson)
        assertNotNull(items)
        assertEquals(1, items?.size)
        assertEquals("你好", items?.first()?.original)
        assertEquals("Nǐ hǎo", items?.first()?.romanized)
        assertEquals("Hello", items?.first()?.translated)
    }

    @Test
    fun parseGeminiResponse_withFullApiResponse_extractsItemsSuccessfully() {
        val apiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "[\n  {\n    \"original\": \"Привет\",\n    \"romanized\": \"Privet\",\n    \"translated\": \"Hello\"\n  }\n]"
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
        """.trimIndent()

        val items = client.parseGeminiResponse(apiResponse)
        assertNotNull(items)
        assertEquals(1, items?.size)
        assertEquals("Привет", items?.first()?.original)
        assertEquals("Privet", items?.first()?.romanized)
        assertEquals("Hello", items?.first()?.translated)
    }

    @Test
    fun parseStructuredLyricsJson_withMalformedJson_returnsNullWithoutCrashing() {
        val malformed = "{ this is not valid json }"
        val items = client.parseStructuredLyricsJson(malformed)
        assertNull(items)
    }

    @Test
    fun parseGeminiResponse_withEmptyCandidates_returnsNull() {
        val emptyCandidates = """{ "candidates": [] }"""
        val items = client.parseGeminiResponse(emptyCandidates)
        assertNull(items)
    }

    @Test
    fun validateLyricItems_withMatchingLinesAndOrder_returnsTrue() {
        val lines = listOf("First line", "Second line", "Third line")
        val items = listOf(
            GeminiLyricItem(original = "First line", romanized = "First line", translated = "Primera línea"),
            GeminiLyricItem(original = "Second line", romanized = "Second line", translated = "Segunda línea"),
            GeminiLyricItem(original = "Third line", romanized = "Third line", translated = "Tercera línea")
        )
        assertTrue(client.validateLyricItems(lines, items))
    }

    @Test
    fun validateLyricItems_withEchoedIndexPrefix_returnsTrue() {
        val lines = listOf("First line", "Second line")
        val items = listOf(
            GeminiLyricItem(original = "[0] First line", romanized = "First line", translated = "Primera línea"),
            GeminiLyricItem(original = "[1] Second line", romanized = "Second line", translated = "Segunda línea")
        )
        assertTrue(client.validateLyricItems(lines, items))
    }

    @Test
    fun validateLyricItems_withMismatchedCount_returnsFalse() {
        val lines = listOf("Line 1", "Line 2", "Line 3")
        val items = listOf(
            GeminiLyricItem(original = "Line 1", translated = "L1"),
            GeminiLyricItem(original = "Line 2", translated = "L2")
        )
        assertFalse(client.validateLyricItems(lines, items))
    }

    @Test
    fun validateLyricItems_withMismatchedOrder_returnsFalse() {
        val lines = listOf("First line", "Second line")
        val items = listOf(
            GeminiLyricItem(original = "Second line", translated = "L2"),
            GeminiLyricItem(original = "First line", translated = "L1")
        )
        assertFalse(client.validateLyricItems(lines, items))
    }
}
