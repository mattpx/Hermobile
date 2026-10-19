package com.hermobile.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermobile.app.BuildConfig
import com.hermobile.app.data.ApiKeyInfo
import com.hermobile.app.model.AgentKind
import com.hermobile.app.model.DEEPSEEK_HARNESS_PROVIDERS
import com.hermobile.app.model.DSH_PROTOCOL_PROVIDERS
import com.hermobile.app.model.DevStack
import com.hermobile.app.model.ProviderKind
import com.hermobile.app.model.ProviderProfile
import com.hermobile.app.model.providersForAgent
import com.hermobile.app.network.ConnectionValidation
import com.hermobile.app.network.DiscoveredModel
import com.hermobile.app.network.ModelDiscoveryResult
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.res.stringResource
import com.hermobile.app.R
import com.hermobile.app.runtime.AntigravityAuthStatus
import com.hermobile.app.ui.theme.AppLanguage
import com.hermobile.app.ui.theme.AppThemeMode
import com.hermobile.app.ui.theme.PocketOrange
import com.hermobile.app.ui.theme.ShopGradientEnd
import com.hermobile.app.ui.theme.ShopGradientStart
import kotlinx.coroutines.launch

/**
 * Shop-mirror shared chrome for Settings (pure styling, no logic): gradient
 * #0096FF -> #00D4FF, 20-22dp cart-row cards, blue CTA gradient, 24dp sheets.
 */
private val SettingsShopCta: Brush
    get() = Brush.horizontalGradient(listOf(ShopGradientStart, ShopGradientEnd))

@Composable
private fun SettingsDetailHeader(icon: ImageVector, title: String, subtitle: String) {
    // Mockup detail header: gradient back-square title block, bold white title + gray subtitle.
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SettingsShopCta, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF8A94A8), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingsCartRowCard(
    icon: ImageVector,
    title: String,
    meta: String,
    onClick: () -> Unit,
    action: @Composable () -> Unit,
) {
    // Cart-row style card: light thumbnail tile left, name+meta middle, action right.
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFDDE2EB), Color(0xFFBFC6D4))),
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF1A233B), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A94A8), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            action()
        }
    }
}

@Composable
private fun SettingsPrimaryCta(onClick: () -> Unit, enabled: Boolean = true, label: @Composable () -> Unit) {
    // Full-width blue gradient CTA like Checkout/Add-to-Cart buttons.
    Surface(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SettingsShopCta)
            .clickable(enabled = enabled, onClick = onClick),
        color = Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { label() }
    }
}

@Composable
private fun SettingsGhostCta(onClick: () -> Unit, enabled: Boolean = true, label: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, ShopGradientStart.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth().height(46.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
            label()
        }
    }
}

@Composable
private fun SettingsTotalsRow(label: String, value: String, valueBlue: Boolean = false) {
    // Mockup promo/totals block: muted gray label, bold white/blue value.
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Color(0xFF8A94A8), fontSize = 13.sp)
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = if (valueBlue) 15.sp else 13.sp,
            color = if (valueBlue) ShopGradientStart else Color.White,
        )
    }
}

private enum class SettingsSection { APPEARANCE, LANGUAGE, TOOLS, RUNTIME, DIAGNOSTICS, UPDATE_CHANNEL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AppUiState,
    onSaveProvider: (ProviderProfile, String) -> Unit,
    onDiscoverModels: suspend (ProviderProfile, String) -> ModelDiscoveryResult,
    onValidateProvider: suspend (ProviderProfile, String, List<DiscoveredModel>) -> ConnectionValidation,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetLanguage: (String) -> Unit = {},
    onPing: () -> Unit,
    onClearTerminal: () -> Unit,
    getSavedApiKey: (ProviderKind) -> String,
    getSavedApiKeys: (ProviderKind) -> List<ApiKeyInfo>,
    onAddApiKey: (ProviderKind, String, String) -> List<ApiKeyInfo>,
    onActivateApiKey: (ProviderKind, String) -> List<ApiKeyInfo>,
    onRemoveApiKey: (ProviderKind, String) -> List<ApiKeyInfo>,
    onInstallDevStack: (DevStack) -> Unit = {},
    onRemoveDevStack: (DevStack) -> Unit = {},
    onInstallAgent: (AgentKind) -> Unit = {},
    onCheckAgentUpdates: () -> Unit = {},
    onUpdateAgent: (AgentKind) -> Unit = {},
    onStartAntigravityLogin: () -> Unit = {},
    onSubmitAntigravityCode: (String) -> Unit = {},
    onLogoutAntigravity: () -> Unit = {},
    onRefreshAntigravityModels: () -> Unit = {},
    onSetAntigravityModel: (String) -> Unit = {},
    onSetAntigravityEffort: (String) -> Unit = {},
    initialDebugUpdateManifestUrl: String = "",
    onSetDebugUpdateManifestUrl: (String) -> Unit = {},
    onClearDebugUpdateManifestUrl: () -> Unit = {},
    onRefreshDiagnostics: () -> Unit = {},
    onStopInactiveProcesses: () -> Unit = {},
    onClearRuntimeCache: () -> Unit = {},
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var terminalCleared by remember { mutableStateOf(false) }
    var showReliabilityHelp by rememberSaveable { mutableStateOf(false) }
    var stackPendingRemoval by remember { mutableStateOf<DevStack?>(null) }

    stackPendingRemoval?.let { stack ->
        AlertDialog(
            onDismissRequest = { stackPendingRemoval = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.tools_remove_dialog_title, stack.localizedLabel())) },
            text = {
                Text(stringResource(R.string.tools_remove_dialog_desc))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        stackPendingRemoval = null
                        onRemoveDevStack(stack)
                    },
                ) { Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { stackPendingRemoval = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    fun toggle(section: SettingsSection) {
        expanded = if (expanded == section) null else section
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = {
                    // Mockup detail header: gradient back-square title block, bold white title + gray subtitle.
                    SettingsDetailHeader(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.settings_title),
                        subtitle = stringResource(R.string.settings_subtitle),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            item {
                SettingsAccordion(
                    title = stringResource(R.string.settings_appearance),
                    subtitle = when (state.themeMode) {
                        AppThemeMode.DARK -> stringResource(R.string.theme_dark_subtitle)
                        AppThemeMode.LIGHT -> stringResource(R.string.theme_light_subtitle)
                        AppThemeMode.SYSTEM -> stringResource(R.string.theme_system_subtitle)
                    },
                    icon = Icons.Default.Tune,
                    expanded = expanded == SettingsSection.APPEARANCE,
                    onClick = { toggle(SettingsSection.APPEARANCE) },
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernThemeChoice(stringResource(R.string.theme_dark), Icons.Default.DarkMode, state.themeMode == AppThemeMode.DARK, { onSetThemeMode(AppThemeMode.DARK) }, Modifier.weight(1f), selectedBrush = SettingsShopCta)
                        ModernThemeChoice(stringResource(R.string.theme_light), Icons.Default.LightMode, state.themeMode == AppThemeMode.LIGHT, { onSetThemeMode(AppThemeMode.LIGHT) }, Modifier.weight(1f), selectedBrush = SettingsShopCta)
                        ModernThemeChoice(stringResource(R.string.theme_system), Icons.Default.PhoneAndroid, state.themeMode == AppThemeMode.SYSTEM, { onSetThemeMode(AppThemeMode.SYSTEM) }, Modifier.weight(1f), selectedBrush = SettingsShopCta)
                    }
                }
            }

            item {
                SettingsAccordion(
                    title = stringResource(R.string.diagnostics_title),
                    subtitle = stringResource(R.string.diagnostics_subtitle),
                    icon = Icons.Default.Memory,
                    expanded = expanded == SettingsSection.DIAGNOSTICS,
                    onClick = {
                        val opening = expanded != SettingsSection.DIAGNOSTICS
                        toggle(SettingsSection.DIAGNOSTICS)
                        if (opening) onRefreshDiagnostics()
                    },
                ) {
                    DeveloperDiagnosticsContent(
                        state = state,
                        onRefresh = onRefreshDiagnostics,
                        onStopInactiveProcesses = onStopInactiveProcesses,
                        onClearRuntimeCache = onClearRuntimeCache,
                    )
                }
            }

            item {
                val currentLang = AppLanguage.fromCode(state.languageCode)
                val langSubtitle = if (currentLang == AppLanguage.SYSTEM) {
                    stringResource(R.string.language_system_default)
                } else {
                    "${currentLang.displayName} (${currentLang.nativeName})"
                }
                SettingsAccordion(
                    title = stringResource(R.string.settings_language),
                    subtitle = langSubtitle,
                    icon = Icons.Default.Language,
                    expanded = expanded == SettingsSection.LANGUAGE,
                    onClick = { toggle(SettingsSection.LANGUAGE) },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppLanguage.entries.forEachIndexed { index, lang ->
                            val isSelected = (state.languageCode == lang.code) ||
                                (state.languageCode.isBlank() && lang == AppLanguage.SYSTEM)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSetLanguage(lang.code) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (lang == AppLanguage.SYSTEM) {
                                            stringResource(R.string.language_system_default)
                                        } else {
                                            lang.nativeName
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                    )
                                    if (lang != AppLanguage.SYSTEM) {
                                        Text(
                                            text = lang.displayName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                SelectionDot(isSelected)
                            }
                            if (index != AppLanguage.entries.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }

            item {
                val installedCount = state.installedDevStacks.count { it != DevStack.WEB }
                SettingsAccordion(
                    title = stringResource(R.string.settings_tools),
                    subtitle = stringResource(R.string.tools_subtitle, installedCount),
                    icon = Icons.Default.Code,
                    expanded = expanded == SettingsSection.TOOLS,
                    onClick = { toggle(SettingsSection.TOOLS) },
                ) {
                    Text(stringResource(R.string.st_tools_included_note), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    DevStack.entries.forEachIndexed { index, stack ->
                        val installed = stack in state.installedDevStacks
                        val installing = state.devStackInstalling == stack
                        val removing = installing && state.devStackRemoving
                        // Cart-row style card: light tile acts as thumb, name+meta middle, stepper-like action right.
                        SettingsCartRowCard(
                            icon = Icons.Default.Code,
                            title = stack.localizedLabel(),
                            meta = stack.localizedInstallsSummary(),
                            onClick = {},
                            action = {
                                when {
                                    removing -> Text(stringResource(R.string.tools_removing), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    installing -> Text("${(state.devStackProgress * 100).toInt()}%", color = ShopGradientStart, fontWeight = FontWeight.Bold)
                                    installed && stack == DevStack.WEB -> Text(stringResource(R.string.tools_included), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    installed -> TextButton(
                                        onClick = { stackPendingRemoval = stack },
                                        enabled = state.devStackInstalling == null,
                                    ) { Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error) }
                                    else -> OutlinedButton(onClick = { onInstallDevStack(stack) }, enabled = state.devStackInstalling == null) { Text(stringResource(R.string.action_add)) }
                                }
                            },
                        )
                        if (installing) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { state.devStackProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                color = ShopGradientStart,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Spacer(Modifier.height(9.dp))
                            state.devStackBytes?.let { (downloaded, total) ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                ) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                stringResource(R.string.st_transfer_progress, formatTransferMb(downloaded), formatTransferMb(total)),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            state.devStackBytesPerSecond?.takeIf { it > 0L }?.let { speed ->
                                                Text(
                                                    stringResource(R.string.st_transfer_speed_eta, formatTransferSpeed(speed), formatTransferEta(downloaded, total, speed)),
                                                    fontSize = 11.sp,
                                                    color = PocketOrange,
                                                    fontFamily = FontFamily.Monospace,
                                                )
                                            }
                                        }
                                        Text(
                                            state.devStackMessage ?: stringResource(R.string.st_downloading),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            } ?: Text(
                                state.devStackMessage ?: stringResource(R.string.st_processing),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (index != DevStack.entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }

            item {
                SettingsAccordion(
                    title = stringResource(R.string.settings_runtime),
                    subtitle = "Ubuntu 20.04 PRoot · ARM64",
                    icon = Icons.Default.Terminal,
                    expanded = expanded == SettingsSection.RUNTIME,
                    onClick = { toggle(SettingsSection.RUNTIME) },
                ) {
                    RuntimeInfoRow(stringResource(R.string.runtime_architecture), "ARM64 (aarch64)")
                    RuntimeInfoRow(stringResource(R.string.runtime_environment), "Ubuntu 20.04 PRoot")
                    RuntimeInfoRow(
                        stringResource(R.string.runtime_active_agent),
                        state.agentKind.localizedTitle() + if (state.installedAgentVersions.containsKey(state.agentKind)) "" else " · " + stringResource(R.string.st_not_installed),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        stringResource(R.string.runtime_installed_agents),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.installedAgentVersions.isEmpty()) {
                        RuntimeInfoRow(stringResource(R.string.st_status), stringResource(R.string.st_no_verified_agent))
                    } else {
                        AgentKind.entries.forEach { agent ->
                            state.installedAgentVersions[agent]?.let { version ->
                                RuntimeInfoRow(agent.localizedTitle(), "v$version")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Shop ghost CTA keeps the clear-history callback + same behavior.
                    SettingsGhostCta(onClick = { onClearTerminal(); terminalCleared = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeleteSweep, null, Modifier.size(17.dp), tint = ShopGradientStart)
                            Spacer(Modifier.width(7.dp))
                            Text(if (terminalCleared) stringResource(R.string.runtime_history_cleared) else stringResource(R.string.runtime_clear_history), color = ShopGradientStart, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    SettingsGhostCta(onClick = { showReliabilityHelp = !showReliabilityHelp }) {
                        Text(stringResource(R.string.runtime_advanced_reliability), color = ShopGradientStart, fontWeight = FontWeight.SemiBold)
                    }
                    AnimatedVisibility(showReliabilityHelp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.st_reliability_help),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // Shop Checkout-style gradient CTA keeps the same open-settings callback.
                            SettingsPrimaryCta(onClick = {
                                runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                                    .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                            }) { Text(stringResource(R.string.st_open_developer_options), fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                item {
                    DebugUpdateChannelSection(
                        initialUrl = initialDebugUpdateManifestUrl,
                        onSave = onSetDebugUpdateManifestUrl,
                        onClear = onClearDebugUpdateManifestUrl,
                    )
                }
            }

            item {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, Modifier.size(20.dp), tint = ShopGradientStart)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            // Shop totals-block: bold white name, gray meta (strings unchanged).
                            Text("Hermobile", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(stringResource(R.string.settings_app_desc), fontSize = 12.sp, color = Color(0xFF8A94A8))
                        }
                        Text("v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ShopGradientStart)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)),
                                )
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.PrivacyTip,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_privacy), fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(R.string.settings_privacy_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.settings_privacy),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun DeveloperDiagnosticsContent(
    state: AppUiState,
    onRefresh: () -> Unit,
    onStopInactiveProcesses: () -> Unit,
    onClearRuntimeCache: () -> Unit,
) {
    val diagnostics = state.diagnostics
    val busy = state.isRunning || state.projectTerminalRunning || state.androidBuildRunning
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onRefresh, enabled = !diagnostics.loading) {
            if (diagnostics.loading) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Refresh, null, Modifier.size(17.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.diagnostics_refresh))
        }
    }
    DiagnosticsCard(stringResource(R.string.diagnostics_memory)) {
        RuntimeInfoRow(stringResource(R.string.diagnostics_app_memory), formatDiagnosticBytes(diagnostics.appPssBytes))
        RuntimeInfoRow(
            stringResource(R.string.diagnostics_java_heap),
            "${formatDiagnosticBytes(diagnostics.javaHeapBytes)} / ${formatDiagnosticBytes(diagnostics.javaHeapMaxBytes)}",
        )
    }
    DiagnosticsCard(stringResource(R.string.diagnostics_processes)) {
        Text(
            if (diagnostics.activeProcesses.isEmpty()) stringResource(R.string.diagnostics_no_processes)
            else diagnostics.activeProcesses.joinToString(" · "),
            fontSize = 13.sp,
            color = if (diagnostics.activeProcesses.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        // Shop ghost CTA keeps the stop-processes callback + behavior.
        SettingsGhostCta(onClick = onStopInactiveProcesses) {
            Text(stringResource(R.string.diagnostics_stop_inactive), color = ShopGradientStart, fontWeight = FontWeight.SemiBold)
        }
    }
    DiagnosticsCard(stringResource(R.string.diagnostics_buffers)) {
        RuntimeInfoRow(stringResource(R.string.diagnostics_messages), diagnostics.messageCount.toString())
        RuntimeInfoRow(stringResource(R.string.diagnostics_live_activity), diagnostics.liveActivityCount.toString())
        RuntimeInfoRow(stringResource(R.string.diagnostics_terminal_buffer), formatDiagnosticBytes(diagnostics.terminalBufferBytes))
        RuntimeInfoRow(stringResource(R.string.diagnostics_build_log), formatDiagnosticBytes(diagnostics.buildLogBytes))
        RuntimeInfoRow(stringResource(R.string.diagnostics_logcat_lines), diagnostics.logcatLines.toString())
        RuntimeInfoRow(stringResource(R.string.diagnostics_file_entries), diagnostics.fileEntries.toString())
    }
    DiagnosticsCard(stringResource(R.string.diagnostics_runtime_cache)) {
        RuntimeInfoRow(stringResource(R.string.diagnostics_cached_output), formatDiagnosticBytes(diagnostics.runtimeCacheBytes))
        Spacer(Modifier.height(10.dp))
        // Shop ghost CTA keeps the clear-cache callback + enabled logic.
        SettingsGhostCta(onClick = onClearRuntimeCache, enabled = !busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeleteSweep, null, Modifier.size(17.dp), tint = ShopGradientStart)
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.diagnostics_clear_cache), color = ShopGradientStart, fontWeight = FontWeight.SemiBold)
            }
        }
        if (busy) {
            Text(
                stringResource(R.string.diagnostics_cache_disabled),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    DiagnosticsCard(stringResource(R.string.diagnostics_recent_exit)) {
        Text(
            diagnostics.recentExit ?: stringResource(R.string.diagnostics_no_recent_exit),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiagnosticsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Shop totals-block header: bold white title.
            Text(title, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

private fun formatDiagnosticBytes(bytes: Long): String = when {
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

private fun formatTransferMb(bytes: Long): String = "%.1f MB".format(bytes.coerceAtLeast(0L) / 1_048_576.0)

private fun formatTransferSpeed(bytesPerSecond: Long): String = when {
    bytesPerSecond >= 1_048_576L -> "%.1f MB/s".format(bytesPerSecond / 1_048_576.0)
    else -> "%.0f KB/s".format(bytesPerSecond / 1_024.0)
}

private fun formatTransferEta(downloaded: Long, total: Long, bytesPerSecond: Long): String {
    val seconds = ((total - downloaded).coerceAtLeast(0L) / bytesPerSecond.coerceAtLeast(1L)).coerceAtLeast(1L)
    return if (seconds >= 60L) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
}

@Composable
private fun SettingsAccordion(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            // Cart-row style accordion header: light thumbnail tile + bold white title + gray meta.
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFDDE2EB), Color(0xFFBFC6D4))),
                            RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, Modifier.size(24.dp), tint = Color(0xFF1A233B))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    Text(subtitle, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A94A8), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    if (expanded) stringResource(R.string.st_collapse) else stringResource(R.string.st_expand),
                    tint = Color(0xFF8A94A8),
                )
            }
            AnimatedVisibility(expanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
                }
            }
        }
    }
}

@Composable
private fun AntigravityConnectionSettings(
    state: AppUiState,
    code: String,
    onCode: (String) -> Unit,
    onStartLogin: () -> Unit,
    onSubmitCode: () -> Unit,
    onLogout: () -> Unit,
    onRefreshModels: () -> Unit,
    onSetModel: (String) -> Unit,
    onSetEffort: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val auth = state.antigravityAuth
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Shop totals-block header: bold white title + gray meta (strings/callbacks unchanged).
            Text(stringResource(R.string.st_official_antigravity_cli), fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                auth.message ?: if (auth.status == AntigravityAuthStatus.SIGNED_IN) {
                    auth.accountEmail?.let { stringResource(R.string.st_connected_as, it) } ?: stringResource(R.string.st_google_account_connected)
                } else stringResource(R.string.st_sign_in_google_flow),
                fontSize = 12.sp,
                color = Color(0xFF8A94A8),
            )
            when (auth.status) {
                AntigravityAuthStatus.SIGNED_IN -> SettingsGhostCta(onClick = onLogout) {
                    Text(stringResource(R.string.st_log_out_antigravity), color = ShopGradientStart, fontWeight = FontWeight.SemiBold)
                }
                AntigravityAuthStatus.STARTING, AntigravityAuthStatus.COMPLETING -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                AntigravityAuthStatus.AWAITING_CODE -> {
                    auth.authorizationUrl?.let { url ->
                        SettingsGhostCta(onClick = { clipboard.setText(AnnotatedString(url)) }) {
                            Text(stringResource(R.string.st_copy_google_signin_url), color = ShopGradientStart, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = onCode,
                        label = { Text(stringResource(R.string.st_one_time_auth_code)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    )
                    // Shop Checkout-style gradient CTA (same submit callback + enabled logic).
                    SettingsPrimaryCta(onClick = onSubmitCode, enabled = code.isNotBlank()) {
                        Text(stringResource(R.string.st_complete_signin), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                AntigravityAuthStatus.SIGNED_OUT, AntigravityAuthStatus.ERROR -> SettingsPrimaryCta(onClick = onStartLogin) {
                    Text(if (auth.status == AntigravityAuthStatus.ERROR) stringResource(R.string.st_reconnect_google) else stringResource(R.string.agent_google_signin), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (auth.status == AntigravityAuthStatus.SIGNED_IN) {
        Text(stringResource(R.string.st_model), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = state.antigravityModel,
            onValueChange = onSetModel,
            label = { Text(stringResource(R.string.st_antigravity_model_id)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        // Shop ghost CTA keeps the refresh-models callback + enabled logic.
        SettingsGhostCta(onClick = onRefreshModels, enabled = !state.antigravityModelsLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.antigravityModelsLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, null, Modifier.size(18.dp), tint = ShopGradientStart)
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.provider_refresh_models), color = ShopGradientStart, fontWeight = FontWeight.SemiBold)
            }
        }
        state.antigravityModels.forEach { model ->
            Row(
                Modifier.fillMaxWidth().clickable { onSetModel(model) }.padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(model, Modifier.weight(1f), fontSize = 12.sp)
                SelectionDot(state.antigravityModel == model)
            }
        }
        Text(stringResource(R.string.st_reasoning_effort), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A94A8))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("low", "medium", "high").forEach { effort ->
                // Shop category-chip feel: selected effort = blue gradient tile (same set-effort callback).
                val effortSelected = state.antigravityEffort == effort || (state.antigravityEffort.isBlank() && effort == "medium")
                if (effortSelected) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SettingsShopCta)
                            .clickable { onSetEffort(effort) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            when (effort) {
                                "low" -> stringResource(R.string.st_effort_low)
                                "medium" -> stringResource(R.string.st_effort_medium)
                                "high" -> stringResource(R.string.st_effort_high)
                                else -> effort
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    OutlinedButton(onClick = { onSetEffort(effort) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                        Text(
                            when (effort) {
                                "low" -> stringResource(R.string.st_effort_low)
                                "medium" -> stringResource(R.string.st_effort_medium)
                                "high" -> stringResource(R.string.st_effort_high)
                                else -> effort
                            },
                        )
                    }
                }
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f), shape = RoundedCornerShape(12.dp)) {
        Text(
            stringResource(R.string.st_antigravity_approval_warning),
            Modifier.fillMaxWidth().padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ConnectionSettings(
    state: AppUiState,
    selectedKind: ProviderKind,
    baseUrl: String,
    model: String,
    dshApi: String,
    apiKey: String,
    models: List<DiscoveredModel>,
    isDiscovering: Boolean,
    isValidating: Boolean,
    status: String?,
    statusOk: Boolean,
    savedKeys: List<ApiKeyInfo>,
    newKeyName: String,
    newApiKey: String,
    newKeyVisible: Boolean,
    onPing: () -> Unit,
    onProvider: (ProviderKind) -> Unit,
    onBaseUrl: (String) -> Unit,
    onModel: (String) -> Unit,
    onDshApi: (String) -> Unit,
    onNewKeyName: (String) -> Unit,
    onNewApiKey: (String) -> Unit,
    onToggleNewKey: () -> Unit,
    onAddKey: () -> Unit,
    onActivateKey: (String) -> Unit,
    onRemoveKey: (String) -> Unit,
    onModels: () -> Unit,
    onValidate: () -> Unit,
) {
    val visibleKinds = remember(state.agentKind) { providersForAgent(state.agentKind) }
    var providerExpanded by rememberSaveable { mutableStateOf(false) }
    var addKeyExpanded by rememberSaveable(savedKeys.isEmpty()) { mutableStateOf(savedKeys.isEmpty()) }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(
                when (state.apiPingStatus) {
                    ApiPingStatus.OK -> Color(0xFF58C9A3)
                    ApiPingStatus.FAILED -> MaterialTheme.colorScheme.error
                    ApiPingStatus.PINGING -> ShopGradientStart
                    ApiPingStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                }, CircleShape,
            ))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                // Shop totals-block: muted gray label, bold white/blue values (same strings).
                Text(stringResource(R.string.provider_active_connection), fontSize = 12.sp, color = Color(0xFF8A94A8))
                Text(state.provider.model.ifBlank { stringResource(R.string.st_not_configured) }, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                state.activeApiKeyName?.let { name ->
                    Text(stringResource(R.string.st_key_label, name), fontSize = 11.sp, color = ShopGradientStart, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                state.apiPingMessage?.let {
                    Text(it, fontSize = 11.sp, color = if (state.apiPingStatus == ApiPingStatus.FAILED) MaterialTheme.colorScheme.error else Color(0xFF8A94A8), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            // Shop stepper-like mini action keeps the same ping callback + enabled logic.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SettingsShopCta)
                    .clickable(enabled = state.apiPingStatus != ApiPingStatus.PINGING, onClick = onPing)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (state.apiPingStatus == ApiPingStatus.PINGING) stringResource(R.string.provider_testing) else stringResource(R.string.action_test),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
    }

    Text(stringResource(R.string.provider_label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A94A8))
    // Cart-row style provider picker: light tile + name/meta + stepper-like expand action.
    SettingsCartRowCard(
        icon = Icons.Default.Settings,
        title = selectedKind.localizedTitle(),
        meta = selectedKind.localizedSubtitle(),
        onClick = { providerExpanded = !providerExpanded },
        action = {
            Icon(if (providerExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, stringResource(R.string.st_choose_provider), tint = Color(0xFF8A94A8))
        },
    )
    AnimatedVisibility(providerExpanded) {
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)) {
            Column {
                visibleKinds.forEachIndexed { index, kind ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        onProvider(kind)
                        providerExpanded = false
                    }.padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(kind.localizedTitle(), fontWeight = FontWeight.Medium)
                        Text(kind.localizedSubtitle(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    SelectionDot(selectedKind == kind)
                }
                if (index != visibleKinds.lastIndex) HorizontalDivider(Modifier.padding(start = 13.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            }
            }
        }
    }

    if (selectedKind.fixedBaseUrl) {
        Text(
            selectedKind.defaultBaseUrl,
            fontSize = 11.sp,
            color = Color(0xFF8A94A8),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        OutlinedTextField(baseUrl, onBaseUrl, label = { Text(stringResource(R.string.provider_base_url)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
    }
    if (state.agentKind == AgentKind.DEEPSEEK_HARNESS && selectedKind in DSH_PROTOCOL_PROVIDERS) {
        Text(stringResource(R.string.provider_gateway_protocol), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
            Column {
                listOf("anthropic-messages", "openai-completions", "openai-responses").forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onDshApi(option) }.padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option, Modifier.weight(1f), fontSize = 13.sp)
                        SelectionDot(dshApi == option)
                    }
                }
            }
        }
    }
    Text(stringResource(R.string.st_model), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A94A8))
    OutlinedTextField(model, onModel, label = { Text(stringResource(R.string.provider_model_id)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
    // Shop ghost CTA keeps the find-models callback + enabled logic.
    SettingsGhostCta(onClick = onModels, enabled = baseUrl.isNotBlank() && apiKey.isNotBlank() && !isDiscovering) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isDiscovering) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(if (models.isEmpty()) Icons.Default.Search else Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp), tint = ShopGradientStart)
            Spacer(Modifier.width(7.dp))
            Text(if (models.isEmpty()) stringResource(R.string.provider_find_models) else stringResource(R.string.provider_available_models_count, models.size), color = ShopGradientStart, fontWeight = FontWeight.SemiBold)
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.provider_api_keys), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A94A8))
            Text(stringResource(R.string.st_saved_keys_failover, savedKeys.size), fontSize = 11.sp, color = Color(0xFF8A94A8))
        }
        // Shop stepper-like mini action keeps the same expand toggle.
        if (addKeyExpanded) {
            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .clickable { addKeyExpanded = !addKeyExpanded }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SettingsShopCta)
                    .clickable { addKeyExpanded = !addKeyExpanded }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.provider_add_key), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
    if (savedKeys.isNotEmpty()) {
        Text(stringResource(R.string.st_saved_api_keys), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A94A8))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
            Column {
                savedKeys.forEachIndexed { index, key ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onActivateKey(key.id) }.padding(start = 13.dp, top = 9.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(key.name, fontWeight = FontWeight.Medium)
                            Text(
                                if (key.isActive) stringResource(R.string.st_active_now_switch) else stringResource(R.string.st_tap_to_make_active),
                                fontSize = 11.sp,
                                color = if (key.isActive) PocketOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SelectionDot(key.isActive)
                        IconButton(onClick = { onRemoveKey(key.id) }) {
                            Icon(Icons.Default.DeleteSweep, stringResource(R.string.st_remove_named, key.name), Modifier.size(18.dp))
                        }
                    }
                    if (index != savedKeys.lastIndex) HorizontalDivider(Modifier.padding(start = 13.dp))
                }
            }
        }
    }
    AnimatedVisibility(addKeyExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                newKeyName,
                onNewKeyName,
                label = { Text(stringResource(R.string.provider_key_name)) },
                placeholder = { Text(stringResource(R.string.st_key_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            )
            OutlinedTextField(
                newApiKey,
                onNewApiKey,
                label = { Text(stringResource(R.string.provider_api_key)) },
                singleLine = true,
                visualTransformation = if (newKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = onToggleNewKey) {
                        Icon(if (newKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, stringResource(R.string.st_show_hide_new_key))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            )
            // Shop Checkout-style gradient CTA (same save-key callback + enabled logic).
            SettingsPrimaryCta(
                onClick = {
                    onAddKey()
                    addKeyExpanded = false
                },
                enabled = newKeyName.isNotBlank() && newApiKey.isNotBlank(),
            ) {
                Text(stringResource(R.string.st_save_api_key), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
    if (status != null) {
        Text(status, fontSize = 12.sp, color = if (statusOk) Color(0xFF8A94A8) else MaterialTheme.colorScheme.error)
    }
    // Shop Checkout-style gradient CTA keeps the same validate callback + enabled logic.
    SettingsPrimaryCta(
        onClick = onValidate,
        enabled = baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank() && !isDiscovering && !isValidating,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isValidating) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (isValidating) stringResource(R.string.st_checking_connection_plain) else stringResource(R.string.st_test_connection_and_save),
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun SelectionDot(selected: Boolean) {
    Box(
        Modifier.size(20.dp).border(if (selected) 2.dp else 1.dp, if (selected) PocketOrange else MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(9.dp).background(PocketOrange, CircleShape))
    }
}

@Composable
private fun ModernThemeChoice(title: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, selectedBrush: Brush? = null) {
    // Shop category-chip feel: selected = blue gradient tile, unselected = dark card.
    if (selected && selectedBrush != null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(selectedBrush)
                .clickable(onClick = onClick),
        ) {
            Column(Modifier.padding(vertical = 13.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, title, Modifier.size(20.dp), tint = Color.White)
                Spacer(Modifier.height(5.dp))
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        return
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, title, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Normal)
        }
    }
}

@Composable
private fun RuntimeInfoRow(label: String, value: String) {
    // Shop promo/totals block: muted gray label, bold white value (same label/value).
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Color(0xFF8A94A8), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
    }
}

@Composable
private fun DebugUpdateChannelSection(
    initialUrl: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable(initialUrl) { mutableStateOf(initialUrl) }
    val isOverridden = initialUrl.isNotBlank()
    SettingsAccordion(
        title = stringResource(R.string.st_update_channel),
        subtitle = if (isOverridden) stringResource(R.string.st_update_channel_overridden) else stringResource(R.string.st_update_channel_default),
        icon = Icons.Default.Tune,
        expanded = expanded,
        onClick = { expanded = !expanded },
    ) {
        Text(
            stringResource(R.string.st_update_channel_help),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.st_manifest_url)) },
            placeholder = { Text("https://your-tunnel.example/hermobile-update.json") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Shop Checkout-style gradient CTA keeps the same save callback + enabled logic.
            Box(Modifier.weight(1f)) {
                SettingsPrimaryCta(onClick = { onSave(url) }, enabled = url.startsWith("https://")) {
                    Text(if (isOverridden) stringResource(R.string.st_replace) else stringResource(R.string.st_use_and_check), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Box(Modifier.weight(1f)) {
                SettingsGhostCta(onClick = onClear, enabled = isOverridden) {
                    Text(stringResource(R.string.st_reset), color = ShopGradientStart, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (isOverridden) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.st_current_url, initialUrl),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
