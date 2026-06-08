package com.ryosoftware.apks_exporter

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContextMenuItem(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.width(16.dp))
        text()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultTopBar(
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onInstallApp: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    hasItems: Boolean
) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = onInstallApp) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.install_app))
            }
            if (hasItems) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                }
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onCancel: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.selected_count, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun SelectionBottomBar(
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onAutoOn: () -> Unit,
    onAutoOff: () -> Unit,
    onSave: () -> Unit,
    onMarkDone: () -> Unit
) {
    val accentTint = MaterialTheme.colorScheme.primary

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarAction(
                icon = {
                    SelectAllIcon(checked = allSelected, tint = accentTint)
                },
                label = stringResource(
                    if (allSelected) R.string.select_none else R.string.select_all
                ),
                onClick = onSelectAll
            )
            BottomBarAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_content_save_outline),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = accentTint
                    )
                },
                label = stringResource(R.string.backup_on),
                onClick = onAutoOn
            )
            BottomBarAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_auto_backup_off),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = accentTint
                    )
                },
                label = stringResource(R.string.backup_off),
                onClick = onAutoOff
            )
            BottomBarAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_actionbar_save_apps),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = accentTint
                    )
                },
                label = stringResource(R.string.save_selected),
                onClick = onSave
            )
            BottomBarAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_actionbar_accept),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = accentTint
                    )
                },
                label = stringResource(R.string.done),
                onClick = onMarkDone
            )
        }
    }
}

@Composable
fun BottomBarAction(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        icon()
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun SelectAllIcon(checked: Boolean, tint: Color) {
    val size = 24.dp
    Canvas(modifier = Modifier.size(size)) {
        val s = size.toPx()
        val stroke = 1.5.dp.toPx()
        val pad = 2.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(pad, pad),
            size = Size(s - pad * 2, s - pad * 2),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = stroke)
        )
        if (checked) {
            val cx = s / 2
            val cy = s / 2
            val checkPath = Path().apply {
                moveTo(cx - 5.dp.toPx(), cy)
                lineTo(cx - 1.5.dp.toPx(), cy + 3.dp.toPx())
                lineTo(cx + 5.dp.toPx(), cy - 3.dp.toPx())
            }
            drawPath(checkPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_no_apps_big),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_apps),
            color = Color.Gray,
            fontSize = 18.sp
        )
    }
}

@Composable
fun AppList(
    items: List<AppItem>,
    viewModel: MainViewModel,
    onContextMenuChange: (AppItem?) -> Unit
) {
    val listState = rememberLazyListState()
    val listScope = rememberCoroutineScope()
    var lettersHeight by remember { mutableStateOf(0f) }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.packageName }
            ) { _, item ->
                AppCardWithSpacing(item, viewModel, onContextMenuChange)
            }
        }

        val letters = remember(items.toList()) {
            items.map { it.appLabel.first().uppercaseChar() }.distinct().sorted()
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(20.dp)
                .fillMaxHeight()
                .onSizeChanged { lettersHeight = it.height.toFloat() }
                .pointerInput(lettersHeight) {
                    detectTapGestures { offset ->
                        val index = if (lettersHeight > 0) {
                            (offset.y / lettersHeight * letters.size).toInt()
                                .coerceIn(0, letters.size - 1)
                        } else 0
                        val letter = letters[index]
                        val targetIndex = items.indexOfFirst {
                            it.appLabel.first().uppercaseChar() >= letter
                        }
                        if (targetIndex >= 0) {
                            listScope.launch { listState.animateScrollToItem(targetIndex) }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxHeight()
            ) {
                for (letter in letters) {
                    Text(
                        text = letter.toString(),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AppCardWithSpacing(
    item: AppItem,
    viewModel: MainViewModel,
    onContextMenuChange: (AppItem?) -> Unit
) {
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val isSelecting by viewModel.isSelecting.collectAsStateWithLifecycle()
    val isSelected = selectedItems.contains(item)
    AppCard(
        item = item,
        isSelected = isSelected,
        isSelecting = isSelecting,
        onClick = {
            if (isSelecting) {
                viewModel.toggleSelection(item)
            } else {
                onContextMenuChange(item)
            }
        },
        onLongClick = {
            if (!isSelecting) {
                viewModel.startSelection(item)
            }
        },
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
fun StatusBadge(
    icon: Painter,
    containerColor: Color,
    tint: Color,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor.copy(alpha = 0.15f),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = containerColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = containerColor
            )
        }
    }
}

@Composable
fun AppCard(
    item: AppItem,
    isSelected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val autoBackupEnabled = remember {
        ApplicationPreferences.get(ApplicationPreferences.AUTO_BACKUP_APPS_KEY, false)
    }
    val initialBackupValue = remember(item) { item.canAutomaticallyBackup() }
    val autoBackupFlow by item.observeCanAutomaticallyBackup()
        .collectAsStateWithLifecycle(initialValue = initialBackupValue)
    var autoBackupApp by remember { mutableStateOf(autoBackupFlow) }
    LaunchedEffect(autoBackupFlow) { autoBackupApp = autoBackupFlow }

    val badgeContainer = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.primary
    val badgeTint = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape)) {
                if (item.icon != null) {
                    Image(
                        painter = rememberDrawablePainter(item.icon),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isSelecting) Modifier.alpha(0.5f) else Modifier
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.sym_def_app_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isSelecting) Modifier.alpha(0.5f) else Modifier
                            )
                    )
                }
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checked),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = item.appLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.packageName != item.appLabel) {
                    Text(
                        text = item.packageName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item.versionName?.let {
                    Text(
                        text = stringResource(R.string.app_version_name_and_number, item.versionName?:"", item.versionCode),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.apkCount > 0) {
                        StatusBadge(
                            icon = painterResource(R.drawable.ic_split_apk),
                            containerColor = badgeContainer,
                            tint = badgeTint,
                            label = pluralStringResource(R.plurals.count_apks, item.apkCount, item.apkCount, sizeToStr(item.apkSize))
                        )
                    }
                    if (item.isSystemApp) {
                        StatusBadge(
                            icon = painterResource(R.drawable.ic_system_app),
                            containerColor = badgeContainer,
                            tint = badgeTint,
                            label = stringResource(R.string.badge_system_app)
                        )
                    }
                    if (item.isAppUpdated && (!item.isAppBacked)) {
                        StatusBadge(
                            icon = painterResource(R.drawable.ic_app_updated),
                            containerColor = badgeContainer,
                            tint = badgeTint,
                            label = stringResource(R.string.badge_app_recently_updated)
                        )
                    }
                    if (autoBackupEnabled && (!autoBackupApp)) {
                        StatusBadge(
                            icon = painterResource(R.drawable.ic_auto_backup_off),
                            containerColor = MaterialTheme.colorScheme.error,
                            tint = MaterialTheme.colorScheme.onError,
                            label = stringResource(R.string.badge_autobackup_off),
                            onClick = {
                                item.canAutomaticallyBackup(true)
                                autoBackupApp = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun sizeToStr(size: Long): String {
    val (resId, arg) = remember(size) {
        when {
            size >= 1_000_000_000 -> R.string.size_in_gb to (size / 1_000_000_000.0)
            size >= 1_000_000 -> R.string.size_in_mb to (size / 1_000_000.0)
            else -> R.string.size_in_kb to (size / 1000.0)
        }
    }
    return stringResource(resId, arg)
}

@Composable
fun rememberDrawablePainter(drawable: Drawable): Painter {
    return remember(drawable) {
        DrawablePainter(drawable)
    }
}

private class DrawablePainter(private val drawable: Drawable) : Painter() {
    override val intrinsicSize: Size
        get() {
            val w = drawable.intrinsicWidth
            val h = drawable.intrinsicHeight
            return if (w > 0 && h > 0) Size(w.toFloat(), h.toFloat())
            else Size(48f, 48f)
        }

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawable.draw(canvas.nativeCanvas)
        }
    }
}

@Composable
fun InstallDescriptionDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    var notShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.install_app)) },
        text = {
            Column {
                Text(stringResource(R.string.install_app_description))
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = notShowAgain, onCheckedChange = { notShowAgain = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.do_not_show_anymore))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (notShowAgain) {
                    ApplicationPreferences.put(
                        DO_NOT_SHOW_INSTALL_APP_FILE_PICKER_DESCRIPTION_KEY, true
                    )
                }
                onAccept()
            }) {
                Text(stringResource(R.string.accept_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
