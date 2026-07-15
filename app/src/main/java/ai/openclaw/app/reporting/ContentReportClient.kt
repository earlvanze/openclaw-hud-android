package ai.openclaw.app.reporting

import ai.openclaw.app.chat.ChatMessage
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

enum class ContentReportCategory(
    val wireName: String,
    val displayName: String,
) {
    HateOrAbuse("hate_or_abuse", "Hate or abuse"),
    SexualContent("sexual_content", "Sexual content"),
    Violence("violence", "Violence"),
    SelfHarm("self_harm", "Self-harm"),
    IllegalOrDangerous("illegal_or_dangerous", "Illegal or dangerous"),
    OtherOffensive("other_offensive", "Other offensive content"),
}

data class ContentReportReceipt(
    val id: String,
)

class ContentReportException(
    override val message: String,
) : IOException(message)

class ContentReportClient(
    private val endpointUrl: String,
    private val packageName: String,
    private val appVersion: String,
    private val client: OkHttpClient = defaultClient(),
    private val allowInsecureLocalhostForTests: Boolean = false,
) {
    suspend fun submit(
        message: ChatMessage,
        category: ContentReportCategory,
        userComment: String,
    ): ContentReportReceipt =
        withContext(Dispatchers.IO) {
            val excerpt = message.reportableAssistantExcerpt()
            if (excerpt.isBlank()) {
                throw ContentReportException("This response has no reportable text.")
            }
            val secureEndpoint = endpointUrl.startsWith("https://")
            val testEndpoint =
                allowInsecureLocalhostForTests &&
                    (endpointUrl.startsWith("http://127.0.0.1:") || endpointUrl.startsWith("http://localhost:"))
            if (!secureEndpoint && !testEndpoint) {
                throw ContentReportException("Content reporting is unavailable in this build.")
            }

            val payload =
                buildJsonObject {
                    put("packageName", packageName)
                    put("appVersion", appVersion)
                    put("category", category.wireName)
                    put("assistantExcerpt", excerpt)
                    put("userComment", userComment.trim().take(MAX_COMMENT_CHARS))
                    put("messageHash", message.contentReportHash(excerpt))
                }.toString()
            val request =
                Request
                    .Builder()
                    .url(endpointUrl)
                    .header("Accept", "application/json")
                    .header("User-Agent", "OpenClaw-HUD/$appVersion")
                    .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseText = response.body.string().take(MAX_RESPONSE_CHARS)
                    if (!response.isSuccessful) {
                        val message =
                            if (response.code == 429) {
                                "Reporting is temporarily busy. Try again later."
                            } else {
                                "The report could not be delivered. Try again."
                            }
                        throw ContentReportException(message)
                    }
                    val receipt =
                        runCatching {
                            Json.parseToJsonElement(responseText).jsonObject["receipt"]?.jsonPrimitive?.content
                        }.getOrNull()
                    if (receipt.isNullOrBlank() || !RECEIPT_PATTERN.matches(receipt)) {
                        throw ContentReportException("The report receiver returned an invalid receipt.")
                    }
                    ContentReportReceipt(id = receipt)
                }
            } catch (error: ContentReportException) {
                throw error
            } catch (_: IOException) {
                throw ContentReportException("The report could not be delivered. Check your connection and try again.")
            }
        }

    companion object {
        const val MAX_EXCERPT_CHARS = 4000
        const val MAX_COMMENT_CHARS = 500
        private const val MAX_RESPONSE_CHARS = 4096
        private val RECEIPT_PATTERN = Regex("^[a-f0-9]{32}$")
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient =
            OkHttpClient
                .Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build()
    }
}

fun ChatMessage.reportableAssistantExcerpt(maxChars: Int = ContentReportClient.MAX_EXCERPT_CHARS): String =
    content
        .asSequence()
        .filter { it.type == "text" }
        .mapNotNull { it.text?.trim()?.takeIf(String::isNotEmpty) }
        .joinToString("\n\n")
        .take(maxChars.coerceAtLeast(0))

internal fun ChatMessage.contentReportHash(excerpt: String = reportableAssistantExcerpt()): String {
    val bytes = "$id\n$excerpt".toByteArray(Charsets.UTF_8)
    return MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
