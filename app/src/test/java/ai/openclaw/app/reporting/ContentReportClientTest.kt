package ai.openclaw.app.reporting

import ai.openclaw.app.chat.ChatMessage
import ai.openclaw.app.chat.ChatMessageContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentReportClientTest {
    @Test
    fun excerptIncludesOnlyBoundedTextParts() {
        val message =
            assistantMessage(
                ChatMessageContent(type = "text", text = " First "),
                ChatMessageContent(type = "image", base64 = "not-sent"),
                ChatMessageContent(type = "text", text = "Second"),
            )

        assertEquals("First\n\nSe", message.reportableAssistantExcerpt(maxChars = 9))
    }

    @Test
    fun hashIsStableAndDoesNotExposeTheMessageId() {
        val message = assistantMessage(ChatMessageContent(type = "text", text = "Response"))

        val hash = message.contentReportHash()

        assertTrue(hash.matches(Regex("^[a-f0-9]{64}$")))
        assertEquals(hash, message.contentReportHash())
        assertTrue(!hash.contains(message.id))
    }

    @Test
    fun submitSendsBoundedReportAndReturnsReceipt() =
        runBlocking {
            val server = MockWebServer()
            server.enqueue(
                MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"ok":true,"receipt":"0123456789abcdef0123456789abcdef"}"""),
            )
            server.start()
            try {
                val client = testClient(server)
                val message = assistantMessage(ChatMessageContent(type = "text", text = "Unsafe response"))

                val receipt = client.submit(message, ContentReportCategory.HateOrAbuse, "Please review")

                assertEquals("0123456789abcdef0123456789abcdef", receipt.id)
                val recorded = server.takeRequest()
                assertEquals("POST", recorded.method)
                val json = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
                assertEquals("ai.openclaw.app.hud", json.getValue("packageName").jsonPrimitive.content)
                assertEquals("2026.7.15-test", json.getValue("appVersion").jsonPrimitive.content)
                assertEquals("hate_or_abuse", json.getValue("category").jsonPrimitive.content)
                assertEquals("Unsafe response", json.getValue("assistantExcerpt").jsonPrimitive.content)
                assertEquals("Please review", json.getValue("userComment").jsonPrimitive.content)
                assertTrue(
                    json
                        .getValue("messageHash")
                        .jsonPrimitive
                        .content
                        .matches(Regex("^[a-f0-9]{64}$")),
                )
                assertTrue(recorded.bodySize < 16_384)
            } finally {
                server.close()
            }
        }

    @Test
    fun submitRejectsInvalidReceipt() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""{"ok":true,"receipt":"invalid"}"""),
        )
        server.start()
        try {
            val error =
                assertThrows(ContentReportException::class.java) {
                    runBlocking {
                        testClient(server).submit(
                            assistantMessage(ChatMessageContent(type = "text", text = "Response")),
                            ContentReportCategory.OtherOffensive,
                            "",
                        )
                    }
                }
            assertEquals("The report receiver returned an invalid receipt.", error.message)
        } finally {
            server.close()
        }
    }

    private fun testClient(server: MockWebServer): ContentReportClient =
        ContentReportClient(
            endpointUrl = server.url("/openclaw-hud-api/report.php").toString(),
            packageName = "ai.openclaw.app.hud",
            appVersion = "2026.7.15-test",
            client = OkHttpClient(),
            allowInsecureLocalhostForTests = true,
        )

    private fun assistantMessage(vararg content: ChatMessageContent): ChatMessage =
        ChatMessage(
            id = "assistant-message-1",
            role = "assistant",
            content = content.toList(),
            timestampMs = 1234L,
        )
}
