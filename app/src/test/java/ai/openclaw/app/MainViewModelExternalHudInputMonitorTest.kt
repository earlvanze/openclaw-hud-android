package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MainViewModelExternalHudInputMonitorTest {
    @Test
    fun monitor_isSessionScopedBoundedAndDisabledByDefault() {
        val app = RuntimeEnvironment.getApplication() as NodeApp
        val viewModel = MainViewModel(app)

        viewModel.recordExternalHudInput(
            kind = ExternalHudInputKind.Key,
            input = "Ignored",
            source = "Keyboard",
            deviceName = null,
            mappedAction = "Ignored",
            handled = false,
            showOnHud = false,
        )
        assertFalse(viewModel.externalHudInputMonitorEnabled.value)
        assertTrue(viewModel.externalHudInputEvents.value.isEmpty())

        viewModel.setExternalHudInputMonitorEnabled(true)
        repeat(10) { index ->
            viewModel.recordExternalHudInput(
                kind = ExternalHudInputKind.Key,
                input = "Key ${index + 1}",
                source = "Keyboard",
                deviceName = "Wearable\nControl",
                mappedAction = "Observed",
                handled = true,
                showOnHud = false,
            )
        }

        assertTrue(viewModel.externalHudInputMonitorEnabled.value)
        assertEquals(
            (10L downTo 3L).toList(),
            viewModel.externalHudInputEvents.value.map { it.sequence },
        )
        assertEquals(
            "Wearable Control",
            viewModel.externalHudInputEvents.value
                .first()
                .deviceName,
        )

        viewModel.setExternalHudInputMonitorEnabled(false)
        viewModel.recordExternalHudInput(
            kind = ExternalHudInputKind.Touch,
            input = "Ignored after disable",
            source = "Touchscreen",
            deviceName = null,
            mappedAction = "Ignored",
            handled = true,
            showOnHud = false,
        )
        assertEquals(8, viewModel.externalHudInputEvents.value.size)

        viewModel.clearExternalHudInputEvents(showMessage = false)
        assertTrue(viewModel.externalHudInputEvents.value.isEmpty())
    }
}
