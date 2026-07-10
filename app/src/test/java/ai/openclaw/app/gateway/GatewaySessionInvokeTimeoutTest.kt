package ai.openclaw.app.gateway

import org.junit.Assert.assertEquals
import org.junit.Test

class GatewaySessionInvokeTimeoutTest {
    @Test
    fun formatGatewayAuthority_bracketsIpv6Hosts() {
        assertEquals("[::1]:18789", formatGatewayAuthority("::1", 18_789))
    }

    @Test
    fun buildGatewayWebSocketUrl_bracketsIpv6Hosts() {
        assertEquals("ws://[::1]:18789", buildGatewayWebSocketUrl("::1", 18_789, useTls = false))
        assertEquals("wss://[::1]:443", buildGatewayWebSocketUrl("::1", 443, useTls = true))
    }

    @Test
    fun buildGatewayWebSocketUrl_normalizesPersistedBracketedIpv6Hosts() {
        assertEquals("ws://[::1]:18789", buildGatewayWebSocketUrl("[::1]", 18_789, useTls = false))
        assertEquals("wss://[::1]:443", buildGatewayWebSocketUrl("[::1]", 443, useTls = true))
    }

    @Test
    fun normalizeGatewayCanvasHostUrl_rewritesLoopbackMetadataToTailnetEndpoint() {
        val endpoint =
            GatewayEndpoint(
                stableId = "manual|100.88.253.107|45219",
                name = "Cyber gateway",
                host = "100.88.253.107",
                port = 45_219,
                tlsEnabled = false,
            )

        assertEquals(
            "http://100.88.253.107:45219/__openclaw__/cap/cap-1",
            normalizeGatewayCanvasHostUrl(
                raw = "http://127.0.0.1:18789/__openclaw__/cap/cap-1",
                endpoint = endpoint,
                isTlsConnection = false,
            ),
        )
    }

    @Test
    fun normalizeGatewayCanvasHostUrl_prefersAdvertisedTailnetDnsOverLoopbackMetadata() {
        val endpoint =
            GatewayEndpoint(
                stableId = "manual|cyber.tailnet.ts.net|443",
                name = "Cyber gateway",
                host = "100.88.253.107",
                port = 443,
                tailnetDns = "cyber.tailnet.ts.net",
                tlsEnabled = true,
            )

        assertEquals(
            "https://cyber.tailnet.ts.net/__openclaw__/cap/cap-2",
            normalizeGatewayCanvasHostUrl(
                raw = "http://localhost:18789/__openclaw__/cap/cap-2",
                endpoint = endpoint,
                isTlsConnection = true,
            ),
        )
    }

    @Test
    fun normalizeGatewayCanvasHostUrl_keepsNonLoopbackCleartextMetadata() {
        val endpoint =
            GatewayEndpoint(
                stableId = "manual|100.88.253.107|45219",
                name = "Cyber gateway",
                host = "100.88.253.107",
                port = 45_219,
                tlsEnabled = false,
            )

        assertEquals(
            "http://100.88.253.107:45219/__openclaw__/cap/cap-3",
            normalizeGatewayCanvasHostUrl(
                raw = "http://100.88.253.107:45219/__openclaw__/cap/cap-3",
                endpoint = endpoint,
                isTlsConnection = false,
            ),
        )
    }

    @Test
    fun resolveInvokeResultAckTimeoutMs_usesFloorWhenMissingOrTooSmall() {
        assertEquals(15_000L, resolveInvokeResultAckTimeoutMs(null))
        assertEquals(15_000L, resolveInvokeResultAckTimeoutMs(0L))
        assertEquals(15_000L, resolveInvokeResultAckTimeoutMs(5_000L))
    }

    @Test
    fun resolveInvokeResultAckTimeoutMs_usesInvokeBudgetWithinBounds() {
        assertEquals(30_000L, resolveInvokeResultAckTimeoutMs(30_000L))
        assertEquals(90_000L, resolveInvokeResultAckTimeoutMs(90_000L))
    }

    @Test
    fun resolveInvokeResultAckTimeoutMs_capsAtUpperBound() {
        assertEquals(120_000L, resolveInvokeResultAckTimeoutMs(121_000L))
        assertEquals(120_000L, resolveInvokeResultAckTimeoutMs(Long.MAX_VALUE))
    }

    @Test
    fun replaceCanvasCapabilityInScopedHostUrl_rewritesTerminalCapabilitySegment() {
        assertEquals(
            "http://127.0.0.1:18789/__openclaw__/cap/new-token",
            replaceCanvasCapabilityInScopedHostUrl(
                "http://127.0.0.1:18789/__openclaw__/cap/old-token",
                "new-token",
            ),
        )
    }

    @Test
    fun replaceCanvasCapabilityInScopedHostUrl_rewritesWhenQueryAndFragmentPresent() {
        assertEquals(
            "http://127.0.0.1:18789/__openclaw__/cap/new-token?a=1#frag",
            replaceCanvasCapabilityInScopedHostUrl(
                "http://127.0.0.1:18789/__openclaw__/cap/old-token?a=1#frag",
                "new-token",
            ),
        )
    }
}
