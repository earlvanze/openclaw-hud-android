package ai.openclaw.app

internal class HudPresentationSessionController<T : Any>(
    private val recoveryDelaysMs: List<Long> = DEFAULT_RECOVERY_DELAYS_MS,
) {
    init {
        require(recoveryDelaysMs.isNotEmpty())
        require(recoveryDelaysMs.all { it > 0L })
    }

    var current: T? = null
        private set

    private var recoveryAttempt = 0

    fun attach(session: T): T? {
        val previous = current
        current = session
        return previous
    }

    fun release(session: T): Boolean {
        if (current !== session) return false
        current = null
        return true
    }

    fun markShown(session: T): Boolean {
        if (current !== session) return false
        resetRecovery()
        return true
    }

    fun nextRecoveryDelayMs(): Long {
        val delay = recoveryDelaysMs[recoveryAttempt.coerceAtMost(recoveryDelaysMs.lastIndex)]
        if (recoveryAttempt < recoveryDelaysMs.lastIndex) {
            recoveryAttempt += 1
        }
        return delay
    }

    fun resetRecovery() {
        recoveryAttempt = 0
    }

    private companion object {
        val DEFAULT_RECOVERY_DELAYS_MS = listOf(500L, 1_000L, 2_000L, 4_000L, 8_000L, 15_000L, 30_000L)
    }
}
