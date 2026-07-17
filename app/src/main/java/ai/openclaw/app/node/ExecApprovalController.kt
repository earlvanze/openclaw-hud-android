package ai.openclaw.app.node

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

enum class ExecApprovalDecision(
    val wireValue: String,
) {
    AllowOnce("allow-once"),
    Deny("deny"),
}

data class ExecApprovalRequest(
    val id: String,
    val commandText: String,
    val commandPreview: String?,
    val warningText: String?,
    val host: String?,
    val nodeId: String?,
    val agentId: String?,
    val sessionKey: String?,
    val allowedDecisions: Set<ExecApprovalDecision>,
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val resolvingDecision: ExecApprovalDecision? = null,
) {
    val displayCommand: String
        get() = commandPreview?.trim()?.takeIf { it.isNotEmpty() } ?: commandText

    fun allows(decision: ExecApprovalDecision): Boolean = decision in allowedDecisions
}

data class ExecApprovalResolutionResult(
    val ok: Boolean,
    val message: String,
)

internal class ExecApprovalController(
    private val request: suspend (method: String, paramsJson: String) -> String,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val stateLock = Any()
    private val _pending = MutableStateFlow<List<ExecApprovalRequest>>(emptyList())
    val pending: StateFlow<List<ExecApprovalRequest>> = _pending.asStateFlow()
    private var stateVersion = 0L
    private var clearGeneration = 0L
    private var nextRefreshId = 0L
    private var latestAppliedRefreshId = 0L
    private val lastMutationVersionById = mutableMapOf<String, Long>()

    private data class RefreshToken(
        val id: Long,
        val clearGeneration: Long,
        val startVersion: Long,
    )

    fun clear() {
        synchronized(stateLock) {
            clearGeneration += 1
            stateVersion += 1
            lastMutationVersionById.clear()
            _pending.value = emptyList()
        }
    }

    suspend fun refresh(): Boolean {
        val token =
            synchronized(stateLock) {
                RefreshToken(
                    id = ++nextRefreshId,
                    clearGeneration = clearGeneration,
                    startVersion = stateVersion,
                )
            }
        val response =
            try {
                request("exec.approval.list", "{}")
            } catch (_: Throwable) {
                return false
            }
        val parsed = parseList(response, nowMs()) ?: return false
        synchronized(stateLock) {
            if (clearGeneration != token.clearGeneration || token.id < latestAppliedRefreshId) {
                return false
            }
            val currentById = _pending.value.associateBy { it.id }
            val mutatedAfterStart =
                lastMutationVersionById
                    .filterValues { it > token.startVersion }
                    .keys
            _pending.value =
                (
                    parsed.filterNot { it.id in mutatedAfterStart } +
                        mutatedAfterStart.mapNotNull(currentById::get)
                ).distinctBy { it.id }
                    .sortedBy { it.createdAtMs }
            latestAppliedRefreshId = token.id
        }
        return true
    }

    fun handleGatewayEvent(
        event: String,
        payloadJson: String?,
    ) {
        when (event) {
            "exec.approval.requested" -> {
                val approval = parseRequested(payloadJson, nowMs()) ?: return
                synchronized(stateLock) {
                    recordMutation(approval.id)
                    _pending.value =
                        (_pending.value.filterNot { it.id == approval.id } + approval)
                            .sortedBy { it.createdAtMs }
                }
            }
            "exec.approval.resolved" -> {
                val id = parseResolvedId(payloadJson) ?: return
                synchronized(stateLock) {
                    recordMutation(id)
                    _pending.value = _pending.value.filterNot { it.id == id }
                }
            }
        }
    }

    fun pruneExpired() {
        val now = nowMs()
        synchronized(stateLock) {
            _pending.value
                .filter { it.expiresAtMs <= now }
                .forEach { recordMutation(it.id) }
            _pending.value = _pending.value.filter { it.expiresAtMs > now }
        }
    }

    suspend fun resolve(
        id: String,
        decision: ExecApprovalDecision,
    ): ExecApprovalResolutionResult {
        val approval =
            synchronized(stateLock) {
                val current =
                    _pending.value.firstOrNull { it.id == id }
                        ?: return ExecApprovalResolutionResult(false, "Approval is no longer available")
                if (current.expiresAtMs <= nowMs()) {
                    recordMutation(id)
                    _pending.value = _pending.value.filterNot { it.id == id }
                    return ExecApprovalResolutionResult(false, "Approval expired")
                }
                if (!current.allows(decision)) {
                    return ExecApprovalResolutionResult(false, "That decision is unavailable")
                }
                if (current.resolvingDecision != null) {
                    return ExecApprovalResolutionResult(false, "Approval is already being resolved")
                }
                recordMutation(id)
                _pending.value =
                    _pending.value.map {
                        if (it.id == id) it.copy(resolvingDecision = decision) else it
                    }
                current
            }

        val params =
            buildJsonObject {
                put("id", JsonPrimitive(approval.id))
                put("decision", JsonPrimitive(decision.wireValue))
            }.toString()
        return try {
            request("exec.approval.resolve", params)
            synchronized(stateLock) {
                recordMutation(approval.id)
                _pending.value = _pending.value.filterNot { it.id == approval.id }
            }
            ExecApprovalResolutionResult(
                ok = true,
                message = if (decision == ExecApprovalDecision.Deny) "Execution denied" else "Execution allowed once",
            )
        } catch (_: Throwable) {
            synchronized(stateLock) {
                recordMutation(approval.id)
                _pending.value =
                    _pending.value.map {
                        if (it.id == approval.id) it.copy(resolvingDecision = null) else it
                    }
            }
            ExecApprovalResolutionResult(false, "Could not resolve approval")
        }
    }

    private fun parseList(
        payloadJson: String?,
        now: Long,
    ): List<ExecApprovalRequest>? {
        if (payloadJson.isNullOrBlank()) return null
        val array =
            try {
                json.parseToJsonElement(payloadJson) as? JsonArray
            } catch (_: Throwable) {
                null
            } ?: return null
        return array
            .mapNotNull { parseRecord(it as? JsonObject, now) }
            .distinctBy { it.id }
            .sortedBy { it.createdAtMs }
    }

    private fun parseRequested(
        payloadJson: String?,
        now: Long,
    ): ExecApprovalRequest? {
        if (payloadJson.isNullOrBlank()) return null
        val obj =
            try {
                json.parseToJsonElement(payloadJson) as? JsonObject
            } catch (_: Throwable) {
                null
            }
        return parseRecord(obj, now)
    }

    private fun parseRecord(
        record: JsonObject?,
        now: Long,
    ): ExecApprovalRequest? {
        record ?: return null
        val id = record.string("id") ?: return null
        val request = record["request"] as? JsonObject ?: return null
        val commandText = request.string("command") ?: return null
        val expiresAtMs = record.long("expiresAtMs") ?: return null
        if (expiresAtMs <= now) return null
        val allowed =
            (request["allowedDecisions"] as? JsonArray)
                ?.mapNotNull { value -> parseExecApprovalDecision((value as? JsonPrimitive)?.content) }
                ?.toSet()
                .orEmpty()
                .ifEmpty { setOf(ExecApprovalDecision.Deny) }
        return ExecApprovalRequest(
            id = id,
            commandText = commandText,
            commandPreview = request.string("commandPreview"),
            warningText = request.string("warningText"),
            host = request.string("host"),
            nodeId = request.string("nodeId"),
            agentId = request.string("agentId"),
            sessionKey = request.string("sessionKey"),
            allowedDecisions = allowed,
            createdAtMs = record.long("createdAtMs") ?: now,
            expiresAtMs = expiresAtMs,
        )
    }

    private fun parseResolvedId(payloadJson: String?): String? {
        if (payloadJson.isNullOrBlank()) return null
        val obj =
            try {
                json.parseToJsonElement(payloadJson) as? JsonObject
            } catch (_: Throwable) {
                null
            }
        return obj?.string("id")
    }

    private fun JsonObject.string(key: String): String? =
        (get(key) as? JsonPrimitive)
            ?.content
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun JsonObject.long(key: String): Long? = (get(key) as? JsonPrimitive)?.content?.toLongOrNull()

    private fun parseExecApprovalDecision(value: String?): ExecApprovalDecision? =
        when (value) {
            ExecApprovalDecision.AllowOnce.wireValue -> ExecApprovalDecision.AllowOnce
            ExecApprovalDecision.Deny.wireValue -> ExecApprovalDecision.Deny
            else -> null
        }

    private fun recordMutation(id: String) {
        stateVersion += 1
        lastMutationVersionById[id] = stateVersion
    }
}
