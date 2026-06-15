package com.ryosoftware.apks_exporter

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ryosoftware.apks_exporter.main_activity.InstallAppTask
import com.ryosoftware.utilities.PermissionUtilities
import com.ryosoftware.utilities.StatusBarUtilities
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToAppDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSelecting by viewModel.isSelecting.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()

    var contextMenuItem by remember { mutableStateOf<AppItem?>(null) }
    var showInstallDescription by remember { mutableStateOf(false) }
    var showNoFolderDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var pendingInstallUri by remember { mutableStateOf<Uri?>(null) }

    val saveFolderUri = ApplicationPreferences.get<String?>(ApplicationPreferences.SAVE_FOLDER_KEY, null)
    val autoBackupEnabled = ApplicationPreferences.get(
        ApplicationPreferences.AUTO_BACKUP_APPS_KEY,
        ApplicationPreferences.AUTO_BACKUP_APPS_DEFAULT
    )

    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (autoBackupEnabled && !PermissionUtilities.isBatteryOptimizationIgnored(context)) {
            showBatteryOptimizationDialog = true
        }
    }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        pendingInstallUri?.let { uri ->
            if (PermissionUtilities.permissionGranted(activity, Manifest.permission.INSTALL_PACKAGES)) {
                isInstalling = true
                doInstallApp(activity, uri) { isInstalling = false }
                pendingInstallUri = null
            }
        }
    }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    val apkFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            if (PermissionUtilities.permissionGranted(activity, Manifest.permission.INSTALL_PACKAGES)) {
                isInstalling = true
                doInstallApp(activity, uri) { isInstalling = false }
            } else {
                pendingInstallUri = uri
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${activity.packageName}".toUri()
                )
                installPermissionLauncher.launch(intent)
            }
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
                    intent.action == Intent.ACTION_PACKAGE_REPLACED) {
                    context?.let { MainBackupWorker.onPackageAddedOrUpdated(it) }
                }
                if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
                    intent.data?.schemeSpecificPart?.let { packageName ->
                        ApplicationPreferences.removePackagePreferences(packageName)
                    }
                }
                viewModel.loadData()
            }
        }
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, packageFilter, Context.RECEIVER_EXPORTED)

        val backupFilter = IntentFilter(MainBackupWorker.ACTION_AUTO_BACKUP_APPS_DONE)
        context.registerReceiver(receiver, backupFilter, Context.RECEIVER_EXPORTED)

        val saveFilter = IntentFilter("${BuildConfig.APPLICATION_ID}.SAVE_TASKS_FINISHED")
        context.registerReceiver(receiver, saveFilter, Context.RECEIVER_EXPORTED)

        onDispose { context.unregisterReceiver(receiver) }
    }

    StatusBarUtilities.setColor(activity, MaterialTheme.colorScheme.primary.toArgb())

    val isScrollEnabled = !isSearching && !isSelecting
    val scrollBehavior = if (isScrollEnabled) {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    } else {
        TopAppBarDefaults.pinnedScrollBehavior()
    }
    val allSelected = isSelecting && selectedItems.size == filteredItems.size && filteredItems.isNotEmpty()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

        topBar = {
            when {
                isSearching -> SearchTopBar(
                    query = searchText ?: "",
                    onQueryChange = { viewModel.updateSearch(it) },
                    onClose = { viewModel.clearSearch() }
                )
                isSelecting -> SelectionTopBar(
                    selectedCount = selectedItems.size,
                    onCancel = { viewModel.cancelSelection() }
                )
                else -> DefaultTopBar(
                    onSearch = { viewModel.startSearch() },
                    onSettings = onNavigateToSettings,
                    onInstallApp = {
                        val doNotShow = ApplicationPreferences.get(
                            DO_NOT_SHOW_INSTALL_APP_FILE_PICKER_DESCRIPTION_KEY, false
                        )
                        if (doNotShow) {
                            apkFilePickerLauncher.launch(arrayOf(
                                "application/vnd.android.package-archive",
                                "application/zip",
                                "application/octet-stream"
                            ))
                        } else {
                            showInstallDescription = true
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    hasItems = items.isNotEmpty()
                )
            }
        },
        bottomBar = {
            if (isSelecting) {
                SelectionBottomBar(
                    allSelected = allSelected,
                    onSelectAll = {
                        if (allSelected) {
                            viewModel.cancelSelection()
                        } else {
                            viewModel.selectAll()
                        }
                    },
                    onAutoOn = {
                        for (item in selectedItems) {
                            item.canAutomaticallyBackup(true)
                        }
                        viewModel.loadData()
                    },
                    onAutoOff = {
                        for (item in selectedItems) {
                            item.canAutomaticallyBackup(false)
                        }
                        viewModel.loadData()
                    },
                    onSave = {
                        if (saveFolderUri == null) {
                            showNoFolderDialog = true
                        } else if (!DocumentFile.fromTreeUri(activity, saveFolderUri.toUri())!!.canWrite()) {
                            activity.contentResolver.takePersistableUriPermission(
                                saveFolderUri.toUri(),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } else {
                            isSaving = true
                            doSaveApps(activity, selectedItems, scope) { isSaving = false }
                        }
                    },
                    onMarkDone = {
                        for (item in selectedItems) {
                            if (item.isAppUpdated) item.setBackupDone()
                        }
                        viewModel.cancelSelection()
                        viewModel.loadData()
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading && items.isEmpty() -> LoadingState()
                filteredItems.isEmpty() && !isLoading -> EmptyState()
                else -> AppList(
                    items = filteredItems,
                    viewModel = viewModel,
                    onContextMenuChange = { contextMenuItem = it }
                )
            }

            if (isSaving || isInstalling) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showInstallDescription) {
        InstallDescriptionDialog(
            onAccept = {
                showInstallDescription = false
                apkFilePickerLauncher.launch(arrayOf(
                    "application/vnd.android.package-archive",
                    "application/zip"
                ))
            },
            onDismiss = { showInstallDescription = false }
        )
    }

    if (contextMenuItem != null) {
        val item = contextMenuItem!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hasLaunchIntent = activity.packageManager.getLaunchIntentForPackage(item.packageName) != null
        val canUninstall = (!item.isSystemApp) || item.isUpdatedSystemApp

        ModalBottomSheet(
            onDismissRequest = { contextMenuItem = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                        .size(72.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.icon != null) {
                        Image(
                            painter = rememberDrawablePainter(item.icon),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.sym_def_app_icon),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Text(
                    text = item.appLabel,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                Text(
                    text = if (item.totalSize != -1L) {
                        pluralStringResource(R.plurals.count_apks_extended_with_data_size, item.apkCount, item.apkCount, sizeToStr(item.apkSize), sizeToStr(item.totalSize))
                    } else {
                        pluralStringResource(R.plurals.count_apks_extended, item.apkCount, item.apkCount, sizeToStr(item.apkSize))
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp)
                )
                AppContextMenuItem(
                    icon = { Icon(painterResource(R.drawable.ic_actionbar_save_apps), contentDescription = null) },
                    text = { Text(pluralStringResource(R.plurals.save_app_package, item.apkCount, item.apkCount)) },
                    onClick = {
                        contextMenuItem = null
                        scope.launch {
                            val folder = ApplicationPreferences.get<String?>(ApplicationPreferences.SAVE_FOLDER_KEY, null)
                            if (folder != null && DocumentFile.fromTreeUri(activity, folder.toUri())!!.canWrite()) {
                                doSaveApps(activity, listOf(item), scope) {}
                            }
                        }
                    }
                )
                AppContextMenuItem(
                    icon = { Icon(Icons.Default.Share, contentDescription = null) },
                    text = { Text(pluralStringResource(R.plurals.share_app_package, item.apkCount, item.apkCount)) },
                    onClick = {
                        contextMenuItem = null
                        item.shareApk(activity)
                    }
                )
                if (canUninstall && (item.packageName != LocalContext.current.packageName)) {
                    AppContextMenuItem(
                        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        text = {
                            Text(if (item.isUpdatedSystemApp) stringResource(R.string.uninstall_updates)
                            else stringResource(R.string.uninstall_app))
                        },
                        onClick = {
                            contextMenuItem = null
                            item.uninstallApp(activity)
                        },
                    )
                }
                AppContextMenuItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    text = { Text(stringResource(R.string.app_info)) },
                    onClick = {
                        contextMenuItem = null
                        onNavigateToAppDetail(item.packageName)
                    }
                )
                if (hasLaunchIntent && (item.packageName != LocalContext.current.packageName)) {
                    AppContextMenuItem(
                        icon = { Icon(painterResource(R.drawable.ic_open_app), contentDescription = null) },
                        text = { Text(stringResource(R.string.open_app)) },
                        onClick = {
                            contextMenuItem = null
                            item.openApp(activity)
                        }
                    )
                }
                if (autoBackupEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                contextMenuItem = null
                                item.canAutomaticallyBackup(!item.canAutomaticallyBackup())
                                scope.launch { viewModel.loadData() }
                            }
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.can_automatically_backup))
                        Switch(
                            checked = item.canAutomaticallyBackup(),
                            onCheckedChange = {
                                contextMenuItem = null
                                item.canAutomaticallyBackup(!item.canAutomaticallyBackup())
                            }
                        )
                    }
                }
            }
        }
    }
    if (showNoFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNoFolderDialog = false },
            title = { Text(stringResource(R.string.information)) },
            text = { Text(stringResource(R.string.no_save_folder_selected)) },
            confirmButton = {
                TextButton(onClick = { showNoFolderDialog = false }) {
                    Text(stringResource(R.string.accept_button))
                }
            }
        )
    }

    if (showBatteryOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationDialog = false },
            title = { Text(stringResource(R.string.battery_optimization_dialog_title)) },
            text = { Text(stringResource(R.string.battery_optimization_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryOptimizationDialog = false
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    batteryOptimizationLauncher.launch(intent)
                }) {
                    Text(stringResource(R.string.accept_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryOptimizationDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

private fun doSaveApps(
    activity: Activity,
    items: List<AppItem>,
    scope: kotlinx.coroutines.CoroutineScope,
    onFinish: () -> Unit
) {
    scope.launch {
        var success = true
        for (item in items) {
            if (!MainService.doBackup(activity, item.packageInfo)) {
                success = false
                break
            }
        }
        onFinish()
        Toast.makeText(
            activity,
            if (success) R.string.operation_done else R.string.cant_complete_requested_operation,
            Toast.LENGTH_LONG
        ).show()
        activity.sendBroadcast(Intent(ACTION_SAVE_TASKS_FINISHED))
    }
}

internal const val ACTION_SAVE_TASKS_FINISHED = BuildConfig.APPLICATION_ID + ".SAVE_TASKS_FINISHED"

internal const val DO_NOT_SHOW_INSTALL_APP_FILE_PICKER_DESCRIPTION_KEY = "do-not-show-install-app-file-picker-description";

private fun doInstallApp(
    activity: Activity,
    uri: android.net.Uri,
    onFinish: () -> Unit
) {
    InstallAppTask(activity, uri, object : InstallAppTask.Listener {
        override fun onInstallAppTaskStarted() {}
        override fun onInstallAppTaskFinished(success: Boolean) {
            onFinish()
            Toast.makeText(
                activity,
                if (success) R.string.operation_done else R.string.cant_complete_requested_operation,
                Toast.LENGTH_LONG
            ).show()
        }
    }).execute()
}


