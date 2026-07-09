package ai.openclaw.app.ui

import ai.openclaw.app.AirVisionStartupDestination
import ai.openclaw.app.BuildConfig
import ai.openclaw.app.GatewayAgentSummary
import ai.openclaw.app.HomeDestination
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.chat.ChatModelChoice
import ai.openclaw.app.resolveAgentIdFromMainSessionKey
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

private enum class HomeTab(
    val label: String,
    val icon: ImageVector,
) {
    Hud(label = "HUD", icon = Icons.Default.Dashboard),
    Chat(label = "Chat", icon = Icons.Default.ChatBubble),
    Voice(label = "Voice", icon = Icons.Default.RecordVoiceOver),
    Agents(label = "Agents", icon = Icons.Default.Person),
    Settings(label = "Settings", icon = Icons.Default.Settings),
}

private enum class StatusVisual {
    Connected,
    Connecting,
    Warning,
    Error,
    Offline,
}

private fun defaultStartupHomeTab(viewModel: MainViewModel): HomeTab =
    if (BuildConfig.OPENCLAW_DEFAULT_HUD) {
        viewModel.airVisionStartupDestination.value.toHomeTab()
    } else {
        HomeTab.Settings
    }

private fun AirVisionStartupDestination.toHomeTab(): HomeTab =
    when (this) {
        AirVisionStartupDestination.Hud -> HomeTab.Hud
        AirVisionStartupDestination.Chat -> HomeTab.Chat
        AirVisionStartupDestination.Voice -> HomeTab.Voice
        AirVisionStartupDestination.Agents -> HomeTab.Agents
        AirVisionStartupDestination.Settings -> HomeTab.Settings
    }

@Composable
fun PostOnboardingTabs(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    var activeTab by rememberSaveable {
        mutableStateOf(defaultStartupHomeTab(viewModel))
    }
    var userSelectedTab by rememberSaveable { mutableStateOf(false) }
    var chatTabStarted by rememberSaveable { mutableStateOf(false) }
    var agentsTabStarted by rememberSaveable { mutableStateOf(false) }
    val requestedHomeDestination by viewModel.requestedHomeDestination.collectAsState()
    val airVisionHudPresentationActive by viewModel.airVisionHudPresentationActive.collectAsState()
    val airVisionPhysicalMainScreenVisible by viewModel.airVisionPhysicalMainScreenVisible.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

    if (BuildConfig.OPENCLAW_DEFAULT_HUD && airVisionHudPresentationActive && !airVisionPhysicalMainScreenVisible) {
        AirVisionPhoneMainScreenHidden(
            isConnected = isConnected,
            onShowPhone = { viewModel.setAirVisionPhysicalMainScreenVisible(true) },
            modifier = modifier,
        )
        return
    }

    LaunchedEffect(requestedHomeDestination) {
        val destination = requestedHomeDestination ?: return@LaunchedEffect
        activeTab =
            when (destination) {
                HomeDestination.Connect -> HomeTab.Settings
                HomeDestination.Chat -> HomeTab.Chat
                HomeDestination.Voice -> HomeTab.Voice
                HomeDestination.Screen -> HomeTab.Agents
                HomeDestination.Settings -> HomeTab.Settings
            }
        viewModel.clearRequestedHomeDestination()
    }

    // Stop TTS when user navigates away from voice tab, and lazily keep the Chat/Agents tabs
    // alive after the first visit so repeated tab switches do not rebuild their UI trees.
    LaunchedEffect(activeTab) {
        viewModel.setVoiceScreenActive(activeTab == HomeTab.Voice || activeTab == HomeTab.Hud)
        if (activeTab == HomeTab.Chat) {
            chatTabStarted = true
        }
        if (activeTab == HomeTab.Agents) {
            agentsTabStarted = true
        }
    }

    val statusText by viewModel.statusText.collectAsState()
    val statusVisual =
        remember(statusText, isConnected) {
            val lower = statusText.lowercase()
            when {
                isConnected -> StatusVisual.Connected
                lower.contains("connecting") || lower.contains("reconnecting") -> StatusVisual.Connecting
                lower.contains("pairing") || lower.contains("approval") || lower.contains("auth") -> StatusVisual.Warning
                lower.contains("error") || lower.contains("failed") -> StatusVisual.Error
                else -> StatusVisual.Offline
            }
        }

    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val hideBottomTabBar = activeTab == HomeTab.Chat && imeVisible

    BoxWithConstraints(modifier = modifier) {
        val hudPreferred = maxWidth >= 900.dp && maxWidth > maxHeight

        LaunchedEffect(hudPreferred, userSelectedTab) {
            if (hudPreferred && !userSelectedTab && activeTab == HomeTab.Settings) {
                activeTab = HomeTab.Hud
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopStatusBar(
                    statusVisual = statusVisual,
                )
            },
            bottomBar = {
                if (!hideBottomTabBar) {
                    BottomTabBar(
                        activeTab = activeTab,
                        onSelect = {
                            userSelectedTab = true
                            activeTab = it
                        },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .background(mobileBackgroundGradient),
            ) {
                if (chatTabStarted) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .alpha(if (activeTab == HomeTab.Chat) 1f else 0f)
                                .zIndex(if (activeTab == HomeTab.Chat) 1f else 0f),
                    ) {
                        ChatSheet(viewModel = viewModel)
                    }
                }

                if (agentsTabStarted) {
                    AgentsTabScreen(
                        viewModel = viewModel,
                        visible = activeTab == HomeTab.Agents,
                        modifier =
                            Modifier
                                .matchParentSize()
                                .alpha(if (activeTab == HomeTab.Agents) 1f else 0f)
                                .zIndex(if (activeTab == HomeTab.Agents) 1f else 0f),
                    )
                }

                when (activeTab) {
                    HomeTab.Hud -> HudScreen(viewModel = viewModel)
                    HomeTab.Chat -> if (!chatTabStarted) ChatSheet(viewModel = viewModel)
                    HomeTab.Voice -> VoiceTabScreen(viewModel = viewModel)
                    HomeTab.Agents -> Unit
                    HomeTab.Settings -> SettingsSheet(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun AirVisionPhoneMainScreenHidden(
    isConnected: Boolean,
    onShowPhone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val shouldRestore =
                        event.type == KeyEventType.KeyUp &&
                            (
                                event.key == Key.Enter ||
                                    event.key == Key.NumPadEnter ||
                                    event.key == Key.Back ||
                                    event.key == Key.Escape
                            )
                    if (shouldRestore) {
                        onShowPhone()
                    }
                    shouldRestore
                }
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isConnected) Color(0xFF00FF66) else Color(0xFF1F6B2B),
                modifier = Modifier.padding(2.dp),
            ) {
                Box(modifier = Modifier.padding(5.dp))
            }
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "HUD on M1",
                style = mobileTitle1.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFB8FFB0),
            )
            Text(
                text = "Phone main screen hidden",
                style = mobileCallout,
                color = Color(0xFF66FF66),
            )
            Button(
                onClick = onShowPhone,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF66),
                        contentColor = Color.Black,
                    ),
            ) {
                Text("Show Phone", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun AgentsTabScreen(
    viewModel: MainViewModel,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val agents by viewModel.gatewayAgents.collectAsState()
    val modelChoices by viewModel.chatModelChoices.collectAsState()
    val sessionModel by viewModel.chatSessionModel.collectAsState()
    val sessionModelProvider by viewModel.chatSessionModelProvider.collectAsState()
    val mainSessionKey by viewModel.mainSessionKey.collectAsState()
    val defaultAgentId by viewModel.gatewayDefaultAgentId.collectAsState()
    val activeAgentId = resolveAgentIdFromMainSessionKey(mainSessionKey) ?: defaultAgentId
    var refreshedForCurrentConnection by rememberSaveable(isConnected) { mutableStateOf(false) }

    LaunchedEffect(isConnected, visible, refreshedForCurrentConnection) {
        if (visible && isConnected && !refreshedForCurrentConnection) {
            viewModel.refreshGatewayAgents()
            viewModel.loadChat(mainSessionKey)
            viewModel.refreshChatModelChoices()
            viewModel.refreshChatSessions(limit = 50)
            refreshedForCurrentConnection = true
        }
    }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Agents",
                    style = mobileTitle1.copy(fontWeight = FontWeight.Bold),
                    color = mobileText,
                )
                Text(
                    text =
                        when {
                            !isConnected -> "Connect to your gateway to load agents."
                            agents.isEmpty() -> "No agents reported by the gateway yet."
                            else -> "${agents.size} available"
                        },
                    style = mobileCallout,
                    color = mobileTextSecondary,
                )
            }
        }

        item {
            ModelPickerRow(
                modelChoices = modelChoices,
                selectedModel = sessionModel,
                selectedProvider = sessionModelProvider,
                enabled = isConnected,
                onRefresh = {
                    viewModel.refreshChatModelChoices()
                    viewModel.refreshChatSessions(limit = 50)
                },
                onSelect = { modelId -> viewModel.setChatSessionModel(modelId) },
            )
        }

        items(agents, key = { it.id }) { agent ->
            AgentPickerRow(
                agent = agent,
                active = agent.id == activeAgentId,
                isDefault = agent.id == defaultAgentId,
                onSelect = { viewModel.selectAgent(agent.id) },
            )
        }
    }
}

@Composable
private fun ModelPickerRow(
    modelChoices: List<ChatModelChoice>,
    selectedModel: String?,
    selectedProvider: String?,
    enabled: Boolean,
    onRefresh: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val activeLabel = modelDisplayLabel(selectedProvider, selectedModel)

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = {
                if (enabled) {
                    onRefresh()
                    expanded = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = mobileCardSurface,
            border = BorderStroke(1.dp, mobileBorder),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Model",
                        style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold),
                        color = mobileTextSecondary,
                    )
                    Text(
                        text = activeLabel,
                        style = mobileHeadline.copy(fontWeight = FontWeight.SemiBold),
                        color = if (enabled) mobileText else mobileTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Select model",
                    tint = mobileTextTertiary,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = mobileCardSurface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, mobileBorder),
        ) {
            DropdownMenuItem(
                text = { Text("Agent default", style = mobileCallout, color = mobileText) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
                trailingIcon = {
                    if (selectedModel.isNullOrBlank()) {
                        Text("✓", style = mobileCallout, color = mobileAccent)
                    }
                },
            )
            for (choice in modelChoices) {
                val modelId = choiceModelId(choice)
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = choice.alias ?: choice.name,
                                style = mobileCallout.copy(fontWeight = FontWeight.SemiBold),
                                color = mobileText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = modelDisplayLabel(choice.provider, modelId),
                                style = mobileCaption1,
                                color = mobileTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    onClick = {
                        onSelect(modelId)
                        expanded = false
                    },
                    trailingIcon = {
                        if (modelId == selectedModel) {
                            Text("✓", style = mobileCallout, color = mobileAccent)
                        }
                    },
                )
            }
        }
    }
}

private fun choiceModelId(choice: ChatModelChoice): String {
    val id = choice.id.trim()
    val provider = choice.provider.trim()
    if (provider.isEmpty() || id.startsWith("$provider/")) return id
    return "$provider/$id"
}

@Composable
private fun AgentPickerRow(
    agent: GatewayAgentSummary,
    active: Boolean,
    isDefault: Boolean,
    onSelect: () -> Unit,
) {
    val title = agent.name?.takeIf { it.isNotBlank() } ?: agent.id
    val badge = agent.emoji?.takeIf { it.isNotBlank() } ?: agentInitials(title)
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (active) mobileAccentSoft else mobileCardSurface,
        border = BorderStroke(1.dp, if (active) mobileAccentBorderStrong else mobileBorder),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (active) mobileAccent else mobileAccentSoft,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badge,
                        style = mobileHeadline.copy(fontWeight = FontWeight.Bold),
                        color = if (active) Color.White else mobileAccent,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = mobileHeadline.copy(fontWeight = FontWeight.SemiBold),
                    color = mobileText,
                    maxLines = 1,
                )
                Text(
                    text =
                        when {
                            active -> "Active"
                            isDefault -> "Default"
                            else -> agent.id
                        },
                    style = mobileCaption1,
                    color = if (active) mobileAccent else mobileTextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun modelDisplayLabel(
    provider: String?,
    model: String?,
): String {
    val modelText = model?.trim()?.takeIf { it.isNotEmpty() } ?: return "Agent default"
    val providerText = provider?.trim()?.takeIf { it.isNotEmpty() }
    return if (providerText != null && !modelText.startsWith("$providerText/")) {
        "$providerText/$modelText"
    } else {
        modelText
    }
}

private fun agentInitials(value: String): String {
    val initials =
        value
            .split(' ', '-', '_', '.')
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
    return initials.ifEmpty { "OC" }
}

@Composable
private fun TopStatusBar(
    statusVisual: StatusVisual,
) {
    val safeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

    val statusDot =
        when (statusVisual) {
            StatusVisual.Connected -> mobileSuccess
            StatusVisual.Connecting -> mobileAccent
            StatusVisual.Warning -> mobileWarning
            StatusVisual.Error -> mobileDanger
            StatusVisual.Offline -> mobileTextTertiary
        }

    Surface(
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(safeInsets),
        color = Color.Black,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = statusDot,
            ) {
                Box(modifier = Modifier.padding(5.dp))
            }
        }
    }
}

@Composable
private fun BottomTabBar(
    activeTab: HomeTab,
    onSelect: (HomeTab) -> Unit,
) {
    val safeInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)

    Box(
        modifier =
            Modifier
                .fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = mobileCardSurface.copy(alpha = 0.97f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, mobileBorder),
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(safeInsets)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeTab.entries.forEach { tab ->
                    val active = tab == activeTab
                    Surface(
                        onClick = { onSelect(tab) },
                        modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (active) mobileAccentSoft else Color.Transparent,
                        border = if (active) BorderStroke(1.dp, LocalMobileColors.current.chipBorderConnecting) else null,
                        shadowElevation = 0.dp,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (active) mobileAccent else mobileTextTertiary,
                            )
                            Text(
                                text = tab.label,
                                color = if (active) mobileAccent else mobileTextSecondary,
                                style = mobileCaption2.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
                            )
                        }
                    }
                }
            }
        }
    }
}
