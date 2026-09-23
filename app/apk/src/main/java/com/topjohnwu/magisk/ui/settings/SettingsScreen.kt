package com.topjohnwu.magisk.ui.settings

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.ConfigBackup
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.isRunningAsStub
import com.topjohnwu.magisk.core.model.ColorMode
import com.topjohnwu.magisk.core.utils.LocaleSetting
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.ui.ThemeState
import com.topjohnwu.magisk.ui.MagiskAccentOptions
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.SettingsArrow
import com.topjohnwu.magisk.ui.component.SettingsDropdown
import com.topjohnwu.magisk.ui.component.SettingsSwitch
import com.topjohnwu.magisk.ui.component.SmallTitle
import com.topjohnwu.magisk.ui.component.verticalScrollbar
import com.topjohnwu.magisk.ui.integration.LsPosedIntegration
import com.topjohnwu.magisk.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onOpenModuleStore: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(CoreR.string.settings)) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(padding)
                .verticalScrollbar(scrollState, contentPadding = PaddingValues(vertical = 12.dp))
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            CustomizationSection(viewModel = viewModel)
            Spacer(Modifier.height(16.dp))
            ModuleStoreSection(onOpenModuleStore = onOpenModuleStore)
            Spacer(Modifier.height(16.dp))
            IntegrationsSection(onOpenModuleStore = onOpenModuleStore)
            Spacer(Modifier.height(16.dp))
            AppSettingsSection()
            Spacer(Modifier.height(16.dp))
            BackupSection()
            if (Info.env.isActive) {
                Spacer(Modifier.height(16.dp))
                MagiskSection(viewModel = viewModel)
            }
            if (Info.showSuperUser) {
                Spacer(Modifier.height(16.dp))
                SuperuserSection(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun IntegrationsSection(
    onOpenModuleStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var status by remember(context) {
        mutableStateOf(LsPosedIntegration.status(context))
    }
    val statusText = when {
        status.managerInstalled -> stringResource(CoreR.string.lsposed_manager_detected)
        status.frameworkInstalled -> stringResource(CoreR.string.lsposed_framework_detected)
        else -> stringResource(CoreR.string.lsposed_not_detected)
    }
    val actionText = if (status.managerInstalled) {
        stringResource(CoreR.string.lsposed_open_manager)
    } else {
        stringResource(CoreR.string.lsposed_browse_modules)
    }

    SmallTitle(text = stringResource(CoreR.string.integrations))
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(CoreR.string.lsposed_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(CoreR.string.lsposed_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                IconButton(onClick = { status = LsPosedIntegration.status(context) }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(CoreR.string.refresh),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = {
                        if (!LsPosedIntegration.openManager(context, status)) {
                            onOpenModuleStore()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(actionText)
                }
                IconButton(onClick = { LsPosedIntegration.openSource(context) }) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = stringResource(CoreR.string.lsposed_learn_more),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleStoreSection(
    onOpenModuleStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallTitle(text = stringResource(CoreR.string.module_store_title))
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        SettingsArrow(
            title = stringResource(CoreR.string.settings_module_store_title),
            summary = stringResource(CoreR.string.settings_module_store_summary),
            onClick = onOpenModuleStore,
        )
    }
}

// --- Customization ---

@Composable
private fun CustomizationSection(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    SmallTitle(text = stringResource(CoreR.string.settings_customization))
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        if (LocaleSetting.useLocaleManager) {
            val locale = LocaleSetting.instance.appLocale
            val summary = locale?.getDisplayName(locale) ?: stringResource(CoreR.string.system_default)
            SettingsArrow(
                title = stringResource(CoreR.string.language),
                summary = summary,
                onClick = {
                    context.startActivity(LocaleSetting.localeSettingsIntent)
                }
            )
        } else {
            val names = remember { LocaleSetting.available.names }
            val tags = remember { LocaleSetting.available.tags }
            var selectedIndex by remember {
                mutableIntStateOf(tags.indexOf(Config.locale).coerceAtLeast(0))
            }
            SettingsDropdown(
                title = stringResource(CoreR.string.language),
                items = names.toList(),
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { index ->
                    selectedIndex = index
                    Config.locale = tags[index]
                }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Color Mode
        val isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val resources = LocalResources.current
        val rawColorModeEntries = remember {
            resources.getStringArray(CoreR.array.color_mode).toList()
        }
        val colorModeEntries = remember(isDynamicColorSupported) {
            if (isDynamicColorSupported) {
                rawColorModeEntries
            } else {
                rawColorModeEntries.drop(3)
            }
        }
        var colorMode by remember { mutableIntStateOf(Config.colorMode) }
        val selectedIndex = ColorMode.fromValue(colorMode).toUiIndex(isDynamicColorSupported)

        SettingsDropdown(
            title = stringResource(CoreR.string.settings_color_mode),
            items = colorModeEntries,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { index ->
                val newMode = ColorMode.fromUiIndex(index, isDynamicColorSupported)
                colorMode = newMode.value
                Config.colorMode = newMode.value
                ThemeState.colorMode = newMode.value
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        AccentColorSelector(
            enabled = !ColorMode.fromValue(colorMode).isMonet || !isDynamicColorSupported,
            selectedColor = ThemeState.accentColor,
            onColorSelected = { color ->
                Config.accentColor = color
                ThemeState.accentColor = color
            },
        )

        if (isRunningAsStub && ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            SettingsArrow(
                title = stringResource(CoreR.string.add_shortcut_title),
                summary = stringResource(CoreR.string.setting_add_shortcut_summary),
                onClick = { viewModel.requestAddShortcut() }
            )
        }
    }
}

@Composable
private fun AccentColorSelector(
    enabled: Boolean,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(CoreR.string.settings_accent_color),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(
                if (enabled) {
                    CoreR.string.settings_accent_color_summary
                } else {
                    CoreR.string.settings_accent_color_system_summary
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MagiskAccentOptions.forEach { option ->
                val selected = option.color.toArgb() == selectedColor
                Column(
                    modifier = Modifier
                        .clickable(enabled = enabled) {
                            onColorSelected(option.color.toArgb())
                        }
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(option.color, CircleShape)
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        width = 3.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape,
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(option.nameRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                    )
                }
            }
        }
    }
}

// --- App Settings ---

@Composable
private fun AppSettingsSection(
    modifier: Modifier = Modifier
) {
    SmallTitle(text = stringResource(CoreR.string.home_app_title))
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // DoH Toggle
        var doh by remember { mutableStateOf(Config.doh) }
        SettingsSwitch(
            title = stringResource(CoreR.string.settings_doh_title),
            summary = stringResource(CoreR.string.settings_doh_description),
            checked = doh,
            onCheckedChange = {
                doh = it
                Config.doh = it
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Update Checker
        var checkUpdate by remember { mutableStateOf(Config.checkUpdate) }
        SettingsSwitch(
            title = stringResource(CoreR.string.settings_check_update_title),
            summary = stringResource(CoreR.string.settings_check_update_summary),
            checked = checkUpdate,
            onCheckedChange = { newValue ->
                checkUpdate = newValue
                Config.checkUpdate = newValue
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Download Path
        var showDownloadDialog by remember { mutableStateOf(false) }
        if (showDownloadDialog) {
            DownloadPathDialog(
                onDismiss = { showDownloadDialog = false }
            )
        }
        SettingsArrow(
            title = stringResource(CoreR.string.settings_download_path_title),
            summary = MediaStoreUtils.fullPath(Config.downloadDir),
            onClick = {
                showDownloadDialog = true
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Random Package Name
        var randName by remember { mutableStateOf(Config.randName) }
        SettingsSwitch(
            title = stringResource(CoreR.string.settings_random_name_title),
            summary = stringResource(CoreR.string.settings_random_name_description),
            checked = randName,
            onCheckedChange = {
                randName = it
                Config.randName = it
            }
        )
    }
}

// --- Backup & restore ---

@Composable
private fun BackupSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Could not open the selected file")
                output.bufferedWriter().use { it.write(ConfigBackup.export()) }
            }.onSuccess {
                Toast.makeText(
                    context,
                    CoreR.string.settings_backup_exported,
                    Toast.LENGTH_SHORT,
                ).show()
            }.onFailure {
                Toast.makeText(
                    context,
                    CoreR.string.settings_backup_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val serialized = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: error("Could not read the selected file")
                if (!ConfigBackup.restore(serialized)) {
                    error("Unsupported backup")
                }
            }.onSuccess {
                ThemeState.colorMode = Config.colorMode
                ThemeState.accentColor = Config.accentColor
                Toast.makeText(
                    context,
                    CoreR.string.settings_backup_restored,
                    Toast.LENGTH_SHORT,
                ).show()
                activity?.recreate()
            }.onFailure {
                Toast.makeText(
                    context,
                    CoreR.string.settings_backup_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    SmallTitle(text = stringResource(CoreR.string.settings_backup_title))
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        SettingsArrow(
            title = stringResource(CoreR.string.settings_backup_export),
            summary = stringResource(CoreR.string.settings_backup_export_summary),
            onClick = { exportLauncher.launch("h3cknn-magisk-settings.json") },
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
        SettingsArrow(
            title = stringResource(CoreR.string.settings_backup_import),
            summary = stringResource(CoreR.string.settings_backup_import_summary),
            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
        )
    }
}

// --- Magisk ---

@Composable
private fun MagiskSection(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    SmallTitle(text = stringResource(CoreR.string.magisk))
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Systemless Hosts
        SettingsArrow(
            title = stringResource(CoreR.string.settings_hosts_title),
            summary = stringResource(CoreR.string.settings_hosts_summary),
            onClick = { viewModel.createHosts() }
        )

        if (Const.Version.atLeast_24_0()) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Zygisk
            var zygisk by remember { mutableStateOf(Config.zygisk) }
            SettingsSwitch(
                title = stringResource(CoreR.string.zygisk),
                summary = stringResource(
                    if (zygisk != Info.isZygiskEnabled) CoreR.string.reboot_apply_change
                    else CoreR.string.settings_zygisk_summary
                ),
                checked = zygisk,
                onCheckedChange = {
                    zygisk = it
                    Config.zygisk = it
                    viewModel.notifyZygiskChange()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // DenyList
            val denyListEnabled by viewModel.denyListEnabled.collectAsStateWithLifecycle()
            SettingsSwitch(
                title = stringResource(CoreR.string.settings_denylist_title),
                summary = stringResource(CoreR.string.settings_denylist_summary),
                checked = denyListEnabled,
                onCheckedChange = { viewModel.toggleDenyList(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // DenyList Config
            SettingsArrow(
                title = stringResource(CoreR.string.settings_denylist_config_title),
                summary = stringResource(CoreR.string.settings_denylist_config_summary),
                onClick = { viewModel.navigateToDenyList() }
            )
        }
    }
}

// --- Superuser ---

@Composable
private fun SuperuserSection(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current

    SmallTitle(text = stringResource(CoreR.string.superuser))
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Tapjack (SDK < S)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            var tapjack by remember { mutableStateOf(Config.suTapjack) }
            SettingsSwitch(
                title = stringResource(CoreR.string.settings_su_tapjack_title),
                summary = stringResource(CoreR.string.settings_su_tapjack_summary),
                checked = tapjack,
                onCheckedChange = {
                    tapjack = it
                    Config.suTapjack = it
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        // Authentication
        var suAuth by remember { mutableStateOf(Config.suAuth) }
        SettingsSwitch(
            title = stringResource(CoreR.string.settings_su_auth_title),
            summary = stringResource(
                if (Info.isDeviceSecure) CoreR.string.settings_su_auth_summary
                else CoreR.string.settings_su_auth_insecure
            ),
            checked = suAuth,
            enabled = Info.isDeviceSecure,
            onCheckedChange = { newValue ->
                viewModel.withAuth {
                    suAuth = newValue
                    Config.suAuth = newValue
                }
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Access Mode
        val accessEntries = remember {
            resources.getStringArray(CoreR.array.su_access).toList()
        }
        var accessMode by remember { mutableIntStateOf(Config.rootMode) }
        SettingsDropdown(
            title = stringResource(CoreR.string.superuser_access),
            items = accessEntries,
            selectedIndex = accessMode,
            onSelectedIndexChange = {
                accessMode = it
                Config.rootMode = it
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Multiuser Mode
        val multiuserEntries = remember {
            resources.getStringArray(CoreR.array.multiuser_mode).toList()
        }
        val multiuserDescriptions = remember {
            resources.getStringArray(CoreR.array.multiuser_summary).toList()
        }
        var multiuserMode by remember { mutableIntStateOf(Config.suMultiuserMode) }
        SettingsDropdown(
            title = stringResource(CoreR.string.multiuser_mode),
            summary = multiuserDescriptions.getOrElse(multiuserMode) { "" },
            items = multiuserEntries,
            selectedIndex = multiuserMode,
            enabled = Const.USER_ID == 0,
            onSelectedIndexChange = {
                multiuserMode = it
                Config.suMultiuserMode = it
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Mount Namespace Mode
        val namespaceEntries = remember {
            resources.getStringArray(CoreR.array.namespace).toList()
        }
        val namespaceDescriptions = remember {
            resources.getStringArray(CoreR.array.namespace_summary).toList()
        }
        var mntNamespaceMode by remember { mutableIntStateOf(Config.suMntNamespaceMode) }
        SettingsDropdown(
            title = stringResource(CoreR.string.mount_namespace_mode),
            summary = namespaceDescriptions.getOrElse(mntNamespaceMode) { "" },
            items = namespaceEntries,
            selectedIndex = mntNamespaceMode,
            onSelectedIndexChange = {
                mntNamespaceMode = it
                Config.suMntNamespaceMode = it
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Automatic Response
        val autoResponseEntries = remember {
            resources.getStringArray(CoreR.array.auto_response).toList()
        }
        var autoResponse by remember { mutableIntStateOf(Config.suAutoResponse) }
        SettingsDropdown(
            title = stringResource(CoreR.string.auto_response),
            items = autoResponseEntries,
            selectedIndex = autoResponse,
            onSelectedIndexChange = { newIndex ->
                val doIt = {
                    autoResponse = newIndex
                    Config.suAutoResponse = newIndex
                }
                if (Config.suAuth) viewModel.withAuth(doIt) else doIt()
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Request Timeout
        val timeoutEntries = remember {
            resources.getStringArray(CoreR.array.request_timeout).toList()
        }
        val timeoutValues = remember { listOf(10, 15, 20, 30, 45, 60) }
        var timeoutIndex by remember {
            mutableIntStateOf(timeoutValues.indexOf(Config.suDefaultTimeout).coerceAtLeast(0))
        }
        SettingsDropdown(
            title = stringResource(CoreR.string.request_timeout),
            items = timeoutEntries,
            selectedIndex = timeoutIndex,
            onSelectedIndexChange = {
                timeoutIndex = it
                Config.suDefaultTimeout = timeoutValues[it]
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // SU Notification
        val notifEntries = remember {
            resources.getStringArray(CoreR.array.su_notification).toList()
        }
        var suNotification by remember { mutableIntStateOf(Config.suNotification) }
        SettingsDropdown(
            title = stringResource(CoreR.string.superuser_notification),
            items = notifEntries,
            selectedIndex = suNotification,
            onSelectedIndexChange = {
                suNotification = it
                Config.suNotification = it
            }
        )

        // Reauthenticate (SDK < O)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            var reAuth by remember { mutableStateOf(Config.suReAuth) }
            SettingsSwitch(
                title = stringResource(CoreR.string.settings_su_reauth_title),
                summary = stringResource(CoreR.string.settings_su_reauth_summary),
                checked = reAuth,
                onCheckedChange = {
                    reAuth = it
                    Config.suReAuth = it
                }
            )
        }

        // Restrict (version >= 30.1)
        if (Const.Version.atLeast_30_1()) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            var restrict by remember { mutableStateOf(Config.suRestrict) }
            SettingsSwitch(
                title = stringResource(CoreR.string.settings_su_restrict_title),
                summary = stringResource(CoreR.string.settings_su_restrict_summary),
                checked = restrict,
                onCheckedChange = {
                    restrict = it
                    Config.suRestrict = it
                }
            )
        }
    }
}

// --- Dialogs ---

@Composable
private fun DownloadPathDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var path by rememberSaveable { mutableStateOf(Config.downloadDir) }

    MagiskDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = stringResource(CoreR.string.settings_download_path_title),
        confirmText = stringResource(android.R.string.ok),
        onConfirm = {
            Config.downloadDir = path
            onDismiss()
        },
        dismissText = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
    ) {
        Column {
            Text(
                text = stringResource(CoreR.string.settings_download_path_message, MediaStoreUtils.fullPath(path)),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = path,
                onValueChange = { path = it },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
