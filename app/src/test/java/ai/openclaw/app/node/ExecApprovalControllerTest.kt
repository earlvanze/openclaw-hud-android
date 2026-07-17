package ai.openclaw.app.node

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecApprovalControllerTest {
    @Test
    fun refreshParsesVisiblePendingApprovalsAndOmitsPersistentAllow() =
        runTest {
            val controller =
                ExecApprovalController(
                    request = { method, _ ->
                        assertEquals("exec.approval.list", method)
                        """
                        [{
                          "id":"approval-1",
                          "request":{
                            "command":"rm -rf /tmp/example",
                            "commandPreview":"rm -rf /tmp/example",
                            "warningText":"Destructive command",
                            "host":"gateway",
                            "agentId":"main",
                            "allowedDecisions":["allow-once","allow-always","deny"]
                          },
                          "createdAtMs":1000,
                          "expiresAtMs":5000
                        }]
                        """.trimIndent()
                    },
                    nowMs = { 2000L },
                )

            assertTrue(controller.refresh())

            val approval = controller.pending.value.single()
            assertEquals("approval-1", approval.id)
            assertEquals("rm -rf /tmp/example", approval.displayCommand)
            assertEquals(
                setOf(ExecApprovalDecision.AllowOnce, ExecApprovalDecision.Deny),
                approval.allowedDecisions,
            )
        }

    @Test
    fun requestedResolvedAndExpiredEventsKeepStateCurrent() {
        var now = 2000L
        val controller = ExecApprovalController(request = { _, _ -> "[]" }, nowMs = { now })
        val requested =
            """
            {
              "id":"approval-2",
              "request":{"command":"echo ready","allowedDecisions":["deny"]},
              "createdAtMs":1500,
              "expiresAtMs":3000
            }
            """.trimIndent()

        controller.handleGatewayEvent("exec.approval.requested", requested)
        assertEquals(
            "approval-2",
            controller.pending.value
                .single()
                .id,
        )

        controller.handleGatewayEvent(
            "exec.approval.resolved",
            """{"id":"approval-2","decision":"deny"}""",
        )
        assertTrue(controller.pending.value.isEmpty())

        controller.handleGatewayEvent("exec.approval.requested", requested)
        now = 3000L
        controller.pruneExpired()
        assertTrue(controller.pending.value.isEmpty())
    }

    @Test
    fun resolveSendsFullIdAndSafeDecisionThenRemovesRequest() =
        runTest {
            val calls = mutableListOf<Pair<String, String>>()
            val controller =
                ExecApprovalController(
                    request = { method, params ->
                        calls += method to params
                        if (method == "exec.approval.list") {
                            """
                            [{
                              "id":"approval-full-id",
                              "request":{"command":"echo ready","allowedDecisions":["allow-once","deny"]},
                              "createdAtMs":1000,
                              "expiresAtMs":5000
                            }]
                            """.trimIndent()
                        } else {
                            "{}"
                        }
                    },
                    nowMs = { 2000L },
                )
            assertTrue(controller.refresh())

            val result = controller.resolve("approval-full-id", ExecApprovalDecision.AllowOnce)

            assertTrue(result.ok)
            assertEquals("Execution allowed once", result.message)
            assertEquals("exec.approval.resolve", calls.last().first)
            assertTrue(calls.last().second.contains("\"id\":\"approval-full-id\""))
            assertTrue(calls.last().second.contains("\"decision\":\"allow-once\""))
            assertTrue(controller.pending.value.isEmpty())
        }

    @Test
    fun missingDecisionMetadataDefaultsToDenyOnly() =
        runTest {
            val controller =
                ExecApprovalController(
                    request = { _, _ ->
                        """
                        [{
                          "id":"approval-3",
                          "request":{"command":"echo legacy"},
                          "createdAtMs":1000,
                          "expiresAtMs":5000
                        }]
                        """.trimIndent()
                    },
                    nowMs = { 2000L },
                )
            assertTrue(controller.refresh())

            val approval = controller.pending.value.single()
            assertTrue(approval.allows(ExecApprovalDecision.Deny))
            assertFalse(approval.allows(ExecApprovalDecision.AllowOnce))
        }

    @Test
    fun refreshMergesRequestsReceivedWhileListIsInFlight() =
        runTest {
            val requestStarted = CompletableDeferred<Unit>()
            val response = CompletableDeferred<String>()
            val controller =
                ExecApprovalController(
                    request = { _, _ ->
                        requestStarted.complete(Unit)
                        response.await()
                    },
                    nowMs = { 2000L },
                )

            val refresh = async { controller.refresh() }
            requestStarted.await()
            controller.handleGatewayEvent(
                "exec.approval.requested",
                approvalJson(id = "event-approval", createdAtMs = 1500),
            )
            response.complete("[${approvalJson(id = "listed-approval", createdAtMs = 1000)}]")

            assertTrue(refresh.await())
            assertEquals(
                listOf("listed-approval", "event-approval"),
                controller.pending.value.map { it.id },
            )
        }

    @Test
    fun refreshDoesNotRestoreApprovalResolvedWhileListIsInFlight() =
        runTest {
            val requestStarted = CompletableDeferred<Unit>()
            val response = CompletableDeferred<String>()
            val controller =
                ExecApprovalController(
                    request = { _, _ ->
                        requestStarted.complete(Unit)
                        response.await()
                    },
                    nowMs = { 2000L },
                )
            controller.handleGatewayEvent(
                "exec.approval.requested",
                approvalJson(id = "resolved-approval", createdAtMs = 1000),
            )

            val refresh = async { controller.refresh() }
            requestStarted.await()
            controller.handleGatewayEvent(
                "exec.approval.resolved",
                """{"id":"resolved-approval","decision":"deny"}""",
            )
            response.complete("[${approvalJson(id = "resolved-approval", createdAtMs = 1000)}]")

            assertTrue(refresh.await())
            assertTrue(controller.pending.value.isEmpty())
        }

    private fun approvalJson(
        id: String,
        createdAtMs: Long,
    ): String =
        """
        {
          "id":"$id",
          "request":{"command":"echo ready","allowedDecisions":["allow-once","deny"]},
          "createdAtMs":$createdAtMs,
          "expiresAtMs":5000
        }
        """.trimIndent()
}
