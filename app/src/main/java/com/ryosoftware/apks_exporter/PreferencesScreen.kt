package com.ryosoftware.apks_exporter

import android.Manifest
import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.ryosoftware.utilities.PermissionUtilities
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    val showSystemPackagesFlow by ApplicationPreferences.observe(
        ApplicationPreferences.SHOW_SYSTEM_PACKAGES_KEY,
        ApplicationPreferences.SHOW_SYSTEM_PACKAGES_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.SHOW_SYSTEM_PACKAGES_DEFAULT)
    val showSystemPackagesOnlyIfUpdatedFlow by ApplicationPreferences.observe(
        ApplicationPreferences.SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_KEY,
        ApplicationPreferences.SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_DEFAULT)
    val saveAsZipFileFlow by ApplicationPreferences.observe(
        ApplicationPreferences.SAVE_AS_ZIP_FILE_KEY,
        ApplicationPreferences.SAVE_AS_ZIP_FILE_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.SAVE_AS_ZIP_FILE_DEFAULT)
    val doNotCompressSingleFilesFlow by ApplicationPreferences.observe(
        ApplicationPreferences.DO_NOT_COMPRESS_SINGLE_FILES_KEY,
        ApplicationPreferences.DO_NOT_COMPRESS_SINGLE_FILES_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.DO_NOT_COMPRESS_SINGLE_FILES_DEFAULT)
    val apkmExtensionFlow by ApplicationPreferences.observe(
        ApplicationPreferences.APKM_EXTENSION_KEY,
        ApplicationPreferences.APKM_EXTENSION_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.APKM_EXTENSION_DEFAULT)
    val useAppLabelFlow by ApplicationPreferences.observe(
        ApplicationPreferences.USE_APP_LABEL_KEY,
        ApplicationPreferences.USE_APP_LABEL_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.USE_APP_LABEL_DEFAULT)
    val useLongVersionNumberFlow by ApplicationPreferences.observe(
        ApplicationPreferences.USE_LONG_VERSION_NUMBER_KEY,
        ApplicationPreferences.USE_LONG_VERSION_NUMBER_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.USE_LONG_VERSION_NUMBER_DEFAULT)
    val sortUpdatedAppsFirstFlow by ApplicationPreferences.observe(
        ApplicationPreferences.SORT_UPDATED_APPS_FIRST_KEY,
        ApplicationPreferences.SORT_UPDATED_APPS_FIRST_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.SORT_UPDATED_APPS_FIRST_DEFAULT)
    val saveFolderUriFlow by ApplicationPreferences.observe<String?>(
        ApplicationPreferences.SAVE_FOLDER_KEY, null
    ).collectAsStateWithLifecycle(initialValue = null)
    val saveFolderName = saveFolderUriFlow?.let { DocumentFile.fromTreeUri(context, it.toUri())?.name }
    val autoBackupAppsFlow by ApplicationPreferences.observe(
        ApplicationPreferences.AUTO_BACKUP_APPS_KEY,
        ApplicationPreferences.AUTO_BACKUP_APPS_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.AUTO_BACKUP_APPS_DEFAULT)
    val autoEnableNewAppsFlow by ApplicationPreferences.observe(
        ApplicationPreferences.AUTO_BACKUP_NEW_APPS_KEY,
        ApplicationPreferences.AUTO_BACKUP_NEW_APPS_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.AUTO_BACKUP_NEW_APPS_DEFAULT)
    val themeFlow by ApplicationPreferences.observe(
        ApplicationPreferences.THEME_KEY,
        ApplicationPreferences.THEME_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.get(
        ApplicationPreferences.THEME_KEY,
        ApplicationPreferences.THEME_DEFAULT
    ))
    val blackBackgroundFlow by ApplicationPreferences.observe(
        ApplicationPreferences.BLACK_BACKGROUND_KEY,
        ApplicationPreferences.BLACK_BACKGROUND_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.get(
        ApplicationPreferences.BLACK_BACKGROUND_KEY,
        ApplicationPreferences.BLACK_BACKGROUND_DEFAULT
    ))
    val useSystemAccentColorFlow by ApplicationPreferences.observe(
        ApplicationPreferences.USE_SYSTEM_ACCENT_COLOR_KEY,
        ApplicationPreferences.USE_SYSTEM_ACCENT_COLOR_DEFAULT
    ).collectAsStateWithLifecycle(initialValue = ApplicationPreferences.get(
        ApplicationPreferences.USE_SYSTEM_ACCENT_COLOR_KEY,
        ApplicationPreferences.USE_SYSTEM_ACCENT_COLOR_DEFAULT
    ))

    var showSystemPackages by remember { mutableStateOf(showSystemPackagesFlow) }
    var showSystemPackagesOnlyIfUpdated by remember { mutableStateOf(showSystemPackagesOnlyIfUpdatedFlow) }
    var saveAsZipFile by remember { mutableStateOf(saveAsZipFileFlow) }
    var doNotCompressSingleFiles by remember { mutableStateOf(doNotCompressSingleFilesFlow) }
    var useApkmExtension by remember { mutableStateOf(apkmExtensionFlow) }
    var useAppLabel by remember { mutableStateOf(useAppLabelFlow) }
    var useLongVersionNumber by remember { mutableStateOf(useLongVersionNumberFlow) }
    var sortUpdatedAppsFirst by remember { mutableStateOf(sortUpdatedAppsFirstFlow) }
    var autoBackupApps by remember { mutableStateOf(autoBackupAppsFlow) }
    var autoBackupForNewApps by remember { mutableStateOf(autoEnableNewAppsFlow) }
    var theme by remember { mutableStateOf(themeFlow) }
    var blackBackground by remember { mutableStateOf(blackBackgroundFlow) }
    var useSystemAccentColor by remember { mutableStateOf(useSystemAccentColorFlow) }

    LaunchedEffect(showSystemPackagesFlow) { showSystemPackages = showSystemPackagesFlow }
    LaunchedEffect(showSystemPackagesOnlyIfUpdatedFlow) { showSystemPackagesOnlyIfUpdated = showSystemPackagesOnlyIfUpdatedFlow }
    LaunchedEffect(saveAsZipFileFlow) { saveAsZipFile = saveAsZipFileFlow }
    LaunchedEffect(doNotCompressSingleFilesFlow) { doNotCompressSingleFiles = doNotCompressSingleFilesFlow }
    LaunchedEffect(apkmExtensionFlow) { useApkmExtension = apkmExtensionFlow }
    LaunchedEffect(useAppLabelFlow) { useAppLabel = useAppLabelFlow }
    LaunchedEffect(useLongVersionNumberFlow) { useLongVersionNumber = useLongVersionNumberFlow }
    LaunchedEffect(sortUpdatedAppsFirstFlow) { sortUpdatedAppsFirst = sortUpdatedAppsFirstFlow }
    LaunchedEffect(autoBackupAppsFlow) { autoBackupApps = autoBackupAppsFlow }
    LaunchedEffect(autoEnableNewAppsFlow) { autoBackupForNewApps = autoEnableNewAppsFlow }
    LaunchedEffect(themeFlow) { theme = themeFlow }
    LaunchedEffect(blackBackgroundFlow) { blackBackground = blackBackgroundFlow }
    LaunchedEffect(useSystemAccentColorFlow) { useSystemAccentColor = useSystemAccentColorFlow }

    fun isUsageStatsGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    var usageStatsGranted by remember { mutableStateOf(isUsageStatsGranted(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageStatsGranted = isUsageStatsGranted(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val initialShowSystem = remember {
        ApplicationPreferences.get(ApplicationPreferences.SHOW_SYSTEM_PACKAGES_KEY, ApplicationPreferences.SHOW_SYSTEM_PACKAGES_DEFAULT)
    }
    val initialShowSystemUpdated = remember {
        ApplicationPreferences.get(ApplicationPreferences.SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_KEY, ApplicationPreferences.SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_DEFAULT)
    }
    val initialAutoBackup = remember {
        ApplicationPreferences.get(ApplicationPreferences.AUTO_BACKUP_APPS_KEY, ApplicationPreferences.AUTO_BACKUP_APPS_DEFAULT)
    }
    val initialAutoBackupForNewApps = remember {
        ApplicationPreferences.get(ApplicationPreferences.AUTO_BACKUP_NEW_APPS_KEY, ApplicationPreferences.AUTO_BACKUP_NEW_APPS_DEFAULT)
    }
    var showBackupWarningDialog by remember { mutableStateOf(false) }
    var isBackupRunning by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isBackupRunning = false
            }
        }
        val filter = IntentFilter(MainBackupWorker.ACTION_AUTO_BACKUP_APPS_DONE)
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    fun handleBack() {
        if (autoBackupApps && (
                (showSystemPackages && (!initialShowSystem)) ||
                (showSystemPackages && (!showSystemPackagesOnlyIfUpdated) && initialShowSystemUpdated) ||
                (autoBackupForNewApps && (!initialAutoBackupForNewApps)) ||
                (!initialAutoBackup)
            )) {
            showBackupWarningDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { handleBack() }
    if (showBackupWarningDialog) {
        AlertDialog(
            onDismissRequest = { showBackupWarningDialog = false },
            title = { Text(stringResource(R.string.backup_warning_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.backup_warning_message))
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showBackupWarningDialog = false }) {
                        Text(stringResource(R.string.backup_warning_continue_editing))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    ApplicationPreferences.put(ApplicationPreferences.SEED_BACKUP_DATA_FOR_ALL_APPS, true)
                    showBackupWarningDialog = false
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.backup_warning_skip))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupWarningDialog = false
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.backup_warning_continue))
                }
            }
        )
    }

    var folderPickerLauncherOnSuccessCallback: ((uri: Uri) -> Unit)? = null
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            ApplicationPreferences.put(ApplicationPreferences.SAVE_FOLDER_KEY, uri.toString())
            folderPickerLauncherOnSuccessCallback?.invoke(uri)
        }
    }
    fun launchFolderPicker(
        uri: Uri?,
        onFolderSelected: ((uri: Uri) -> Unit)? = null
    ) {
        folderPickerLauncherOnSuccessCallback = onFolderSelected
        folderPickerLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun setAutoBackupApps(newValue: Boolean, uri: Uri? = null) {
        autoBackupApps = newValue
        val hasFolder = ((uri != null) || (saveFolderUriFlow != null))

        if (newValue && (!hasFolder)) {
            launchFolderPicker(null) { uri ->
                setAutoBackupApps(true, uri)
            }
            return
        }
        ApplicationPreferences.put(ApplicationPreferences.AUTO_BACKUP_APPS_KEY, newValue)
        if (newValue) {
            if (!PermissionUtilities.permissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SectionHeader(stringResource(R.string.apps_list))
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.show_system_apps),
                    summary = stringResource(
                        if (showSystemPackages) R.string.show_system_apps_on
                        else R.string.show_system_apps_off
                    ),
                    checked = showSystemPackages,
                    onCheckedChange = {
                        showSystemPackages = it
                        ApplicationPreferences.put(ApplicationPreferences.SHOW_SYSTEM_PACKAGES_KEY, it)
                    }
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.show_system_apps_only_if_updated),
                    summary = stringResource(
                        if (showSystemPackagesOnlyIfUpdated) R.string.show_system_apps_only_if_updated_on
                        else R.string.show_system_apps_only_if_updated_off
                    ),
                    checked = showSystemPackagesOnlyIfUpdated,
                    enabled = showSystemPackages,
                    onCheckedChange = {
                        showSystemPackagesOnlyIfUpdated = it
                        ApplicationPreferences.put(ApplicationPreferences.SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_KEY, it)
                    }
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.sort_updated_apps_first),
                    summary = stringResource(
                        if (sortUpdatedAppsFirst) R.string.sort_updated_apps_first_on
                        else R.string.sort_updated_apps_first_off
                    ),
                    checked = sortUpdatedAppsFirst,
                    onCheckedChange = {
                        sortUpdatedAppsFirst = it
                        ApplicationPreferences.put(ApplicationPreferences.SORT_UPDATED_APPS_FIRST_KEY, it)
                    }
                )
            }

            item {
                SectionHeader(stringResource(R.string.storage))
            }
            item {
                ClickablePreference(
                    title = stringResource(R.string.save_folder),
                    summary = saveFolderName ?: stringResource(R.string.save_folder_not_set),
                    onClick = { launchFolderPicker(saveFolderUriFlow?.toUri()) }
                )
            }

            item {
                SectionHeader(stringResource(R.string.backup_format))
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.use_app_label),
                    summary = stringResource(
                        if (useAppLabel) R.string.use_app_label_on
                        else R.string.use_app_label_off
                    ),
                    checked = useAppLabel,
                    onCheckedChange = {
                        useAppLabel = it
                        ApplicationPreferences.put(ApplicationPreferences.USE_APP_LABEL_KEY, it)
                    }
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.use_long_version_number),
                    summary = stringResource(
                        if (useLongVersionNumber) R.string.use_long_version_number_on
                        else R.string.use_long_version_number_off
                    ),
                    checked = useLongVersionNumber,
                    onCheckedChange = {
                        useLongVersionNumber = it
                        ApplicationPreferences.put(ApplicationPreferences.USE_LONG_VERSION_NUMBER_KEY, it)
                    }
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.save_as_zip_file),
                    summary = stringResource(
                        if (saveAsZipFile) R.string.save_as_zip_file_on
                        else R.string.save_as_zip_file_off
                    ),
                    checked = saveAsZipFile,
                    onCheckedChange = {
                        saveAsZipFile = it
                        ApplicationPreferences.put(ApplicationPreferences.SAVE_AS_ZIP_FILE_KEY, it)
                    }
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.apkm_extension),
                    summary = stringResource(
                        if (useApkmExtension) R.string.apkm_extension_on
                        else R.string.apkm_extension_off
                    ),
                    checked = useApkmExtension,
                    enabled = saveAsZipFile,
                    onCheckedChange = {
                        useApkmExtension = it
                        ApplicationPreferences.put(ApplicationPreferences.APKM_EXTENSION_KEY, it)
                    }
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.do_not_compress_single_files),
                    summary = stringResource(
                        if (doNotCompressSingleFiles) R.string.do_not_compress_single_files_on
                        else R.string.do_not_compress_single_files_off
                    ),
                    checked = doNotCompressSingleFiles,
                    enabled = saveAsZipFile,
                    onCheckedChange = {
                        doNotCompressSingleFiles = it
                        ApplicationPreferences.put(ApplicationPreferences.DO_NOT_COMPRESS_SINGLE_FILES_KEY, it)
                    }
                )
            }

            item {
                SectionHeader(stringResource(R.string.automation))
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.auto_backup_apps),
                    summary = stringResource(
                        if (autoBackupApps) R.string.auto_backup_apps_on
                        else R.string.auto_backup_apps_off
                    ),
                    checked = autoBackupApps,
                    enabled = true,
                    onCheckedChange = ::setAutoBackupApps
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.auto_enable_new_apps),
                    summary = stringResource(
                        if (autoBackupForNewApps) R.string.auto_enable_new_apps_on
                        else R.string.auto_enable_new_apps_off
                    ),
                    checked = autoBackupForNewApps,
                    enabled = autoBackupApps,
                    onCheckedChange = {
                        autoBackupForNewApps = it
                        ApplicationPreferences.put(ApplicationPreferences.AUTO_BACKUP_NEW_APPS_KEY, it)
                    }
                )
            }
            item {
                ClickablePreference(
                    title = stringResource(R.string.force_backup_now),
                    summary = if (isBackupRunning) {
                        stringResource(R.string.force_backup_running)
                    } else buildString {
                        val lastBackupTime = ApplicationPreferences.get(ApplicationPreferences.LAST_AUTO_BACKUP_TIME_KEY, 0L)
                        val nextBackupTime = ApplicationPreferences.get(ApplicationPreferences.NEXT_AUTO_BACKUP_TIME_KEY, 0L)
                        if (lastBackupTime > 0) {
                            val lastBackupResult = ApplicationPreferences.get<String?>(ApplicationPreferences.LAST_BACKUP_RESULTS_KEY, null)
                            append(
                                stringResource(
                                    R.string.auto_backup_last_date,
                                    java.time.Instant.ofEpochMilli(lastBackupTime)
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDateTime()
                                        .format(
                                            java.time.format.DateTimeFormatter.ofPattern(
                                                stringResource(R.string.auto_backup_last_date_format)
                                            )
                                        )
                                )
                            )
                            append("\n")
                            if (!lastBackupResult.isNullOrEmpty()) {
                                append(lastBackupResult)
                                append("\n")
                            }
                            append("\n")
                        }
                        if (nextBackupTime > 0) {
                            append(
                                stringResource(
                                    R.string.auto_backup_next_date,
                                    java.time.Instant.ofEpochMilli(nextBackupTime)
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDateTime()
                                        .format(
                                            java.time.format.DateTimeFormatter.ofPattern(
                                                stringResource(R.string.auto_backup_last_date_format)
                                            )
                                        )
                                )
                            )
                            append("\n\n")
                        }
                        append(stringResource(R.string.force_backup_now_summary))
                    },
                    enabled = autoBackupApps && (!isBackupRunning),
                    onClick = {
                        isBackupRunning = true
                        MainBackupWorker.onBackupForced(context)
                    }
                )
            }

            item {
                SectionHeader(stringResource(R.string.appearance))
            }
            item {
                SettingsDropdownPreference(
                    title = stringResource(R.string.theme),
                    options = listOf(
                        ApplicationPreferences.THEME_DARK to stringResource(R.string.dark_theme),
                        ApplicationPreferences.THEME_LIGHT to stringResource(R.string.light_theme),
                        ApplicationPreferences.THEME_SYSTEM to stringResource(R.string.system_theme)
                    ),
                    selected = theme,
                    onSelected = { newValue ->
                        theme = newValue
                        ApplicationPreferences.put(
                            ApplicationPreferences.THEME_KEY,
                            newValue
                        )
                    }
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.black_background),
                    summary = stringResource(
                        if (blackBackground) R.string.black_background_on
                        else R.string.black_background_off
                    ),
                    checked = blackBackground,
                    enabled = (theme == ApplicationPreferences.THEME_DARK) || ((theme == ApplicationPreferences.THEME_SYSTEM) && isSystemInDarkTheme()),
                    onCheckedChange = { newValue ->
                        blackBackground = newValue
                        ApplicationPreferences.put(ApplicationPreferences.BLACK_BACKGROUND_KEY, newValue)
                    }
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.use_system_accent_color),
                    summary = stringResource(
                        if (useSystemAccentColor) R.string.use_system_accent_color_on
                        else R.string.use_system_accent_color_off
                    ),
                    checked = useSystemAccentColor,
                    onCheckedChange = { newValue ->
                        useSystemAccentColor = newValue
                        ApplicationPreferences.put(ApplicationPreferences.USE_SYSTEM_ACCENT_COLOR_KEY, newValue)
                    }
                )
            }

            item {
                SectionHeader(stringResource(R.string.permissions_title))
            }
            item {
                ClickablePreference(
                    title = stringResource(R.string.usage_access),
                    summary = stringResource(
                        if (usageStatsGranted) R.string.usage_access_granted
                        else R.string.usage_access_not_granted
                    ),
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }

            item {
                SectionHeader(stringResource(R.string.about))
            }
            item {
                val url = stringResource(R.string.github_repo_link).toUri()
                ClickablePreference(
                    title = stringResource(R.string.github_repo),
                    summary = stringResource(R.string.github_repo_summary),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            url
                        )
                        context.startActivity(intent)
                    }
                )
            }
            item {
                InfoPreference(
                    title = stringResource(R.string.app_version_name),
                    summary = stringResource(R.string.app_version_name_summary, BuildConfig.versionName, BuildConfig.versionCode)
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SwitchPreference(
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
    )
}

@Composable
private fun ClickablePreference(
    title: String,
    summary: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
private fun InfoPreference(
    title: String,
    summary: String
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdownPreference(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedLabel = options.first { it.first == selected }.second

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {

            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {

                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
