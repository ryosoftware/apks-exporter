package com.ryosoftware.apks_exporter

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.storage.StorageManager
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.core.net.toUri
import com.ryosoftware.utilities.PackageManagerUtilities
import java.io.ByteArrayInputStream
import java.io.File
import java.security.cert.CertificateFactory
import java.security.MessageDigest
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(packageName: String, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val flags = PackageManager.GET_PERMISSIONS or
            PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS or
            PackageManager.GET_SIGNING_CERTIFICATES
    val packageInfo = remember(packageName) {
        try {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } catch (e: Exception) { null }
    }
    val appIcon = remember(packageInfo) {
        packageInfo?.let { PackageManagerUtilities.getApplicationIcon(context, it.packageName) }
    }
    val isSystemApp = (packageInfo?.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
    val isLaunchable = remember(packageName) {
        try { pm.getLaunchIntentForPackage(packageName) != null } catch (_: Exception) { false }
    }
    val fromPlayStore = remember(packageName) {
        try { "com.android.vending" == pm.getInstallerPackageName(packageName) } catch (_: Exception) { false }
    }
    val installSource = remember(packageName) {
        try {
            when (val pkg = pm.getInstallerPackageName(packageName)) {
                "com.android.vending" -> "Play Store"
                "org.fdroid.fdroid" -> "F-Droid"
                "com.aurora.store" -> "Aurora Store"
                "com.sec.android.app.samsungapps" -> "Galaxy Store"
                "com.miui.packageinstaller" -> "Xiaomi Package Installer"
                null -> "Sideloaded"
                else -> pkg
            }
        } catch (_: Exception) { "?" }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_detail_info),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        if (packageInfo == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.cant_complete_requested_operation))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isDisabled = !(packageInfo.applicationInfo?.enabled ?: false)
            item { HeaderSection(packageInfo, appIcon, isSystemApp, isDisabled) }
            item {
                SectionCard {
                    InfoSection(packageInfo, installSource)
                }
            }
            item {
                SectionCard {
                    ApkSection(packageInfo)
                }
            }

            if (isUsageStatsGranted(context)) {
                item {
                    SectionCard {
                        StorageStatsSection(packageName, packageInfo)
                    }
                }
            }

            item {
                SectionCard {
                    SigningSection(packageInfo)
                }
            }
            item {
                SectionCard {
                    ManifestComponentsSection(packageInfo)
                }
            }
            val perms = packageInfo.requestedPermissions?.toList() ?: emptyList()
            if (perms.isNotEmpty()) {
                item {
                    SectionCard {
                        SectionTitle(stringResource(R.string.permissions))
                        perms.forEach { perm ->
                            val name = perm.removePrefix("android.permission.")
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            item {
                SectionCard {
                    SpecialPermissionsSection(packageInfo, perms.toSet())
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData("package:$packageName".toUri())
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, R.string.cant_complete_requested_operation, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.open_system_app_info),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (isLaunchable) {
                        FilledTonalButton(
                            onClick = {
                                try {
                                    val intent = pm.getLaunchIntentForPackage(packageName)!!
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, R.string.cant_complete_requested_operation, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(painterResource(R.drawable.ic_open_app), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.open_app),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    if (fromPlayStore) {
                        FilledTonalButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, R.string.cant_complete_requested_operation, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                stringResource(R.string.view_in_play_store),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun HeaderSection(packageInfo: PackageInfo, icon: android.graphics.drawable.Drawable?, isSystemApp: Boolean, isDisabled: Boolean = false) {
    Box {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            AndroidView(
                                factory = {
                                    ImageView(it).apply { setImageDrawable(icon) }
                                },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.sym_def_app_icon),
                                contentDescription = null,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = PackageManagerUtilities.getApplicationLabel(
                        LocalContext.current, packageInfo.packageName, packageInfo.packageName
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = packageInfo.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val badgeColor = MaterialTheme.colorScheme.tertiary

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp,
                            bottomStart = 4.dp
                        ),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (isSystemApp) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_system_app),
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(Modifier.width(4.dp))
                            }

                            Text(
                                text = if (isSystemApp) stringResource(R.string.system_app)
                                       else stringResource(R.string.user_app),
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeColor
                            )
                        }
                    }
                    if (isDisabled) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 8.dp,
                                bottomEnd = 8.dp,
                                bottomStart = 4.dp
                            ),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_block),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(Modifier.width(4.dp))

                                Text(
                                    text = stringResource(R.string.badge_disabled_app),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(packageInfo: PackageInfo, installSource: String) {
    val dateFmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    SectionTitle(stringResource(R.string.app_info))
    InfoRow(stringResource(R.string.version), "${packageInfo.versionName ?: ""} (${packageInfo.longVersionCode})")
    InfoRow(
        stringResource(R.string.requires_android),
        "${packageInfo.applicationInfo?.minSdkVersion ?: "?"} -> ${packageInfo.applicationInfo?.targetSdkVersion ?: "?"}"
    )
    InfoRow(stringResource(R.string.installed), dateFmt.format(Date(packageInfo.firstInstallTime)))
    if (packageInfo.firstInstallTime != packageInfo.lastUpdateTime) {
        InfoRow(stringResource(R.string.last_updated), dateFmt.format(Date(packageInfo.lastUpdateTime)))
    }
    InfoRow(stringResource(R.string.install_source), installSource)
}

@Composable
private fun ApkSection(packageInfo: PackageInfo) {
    val ai = packageInfo.applicationInfo ?: return
    val splits = ai.splitSourceDirs

    SectionTitle(stringResource(R.string.apk_files))

    val base = File(ai.sourceDir)

    InfoRow(stringResource(R.string.base_apk), "${base.name} (${formatBytes(base.length())})")

    if (!splits.isNullOrEmpty()) {
        splits.forEachIndexed { i, path ->
            val f = File(path)

            InfoRow(stringResource(R.string.split_apk, i + 1), "${f.name} (${formatBytes(f.length())})")
        }
        val total = splits.sumOf { File(it).length() } + base.length()

        Text(
            text = stringResource(R.string.total_size, formatBytes(total)),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun StorageStatsSection(packageName: String, packageInfo: PackageInfo) {
    val context = LocalContext.current
    val stats = remember(packageName) {
        if (!isUsageStatsGranted(context)) null
        else try {
            val ssm = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, packageName, Process.myUserHandle())
        } catch (e: Exception) { null }
    }
    if (stats != null) {
        SectionTitle(stringResource(R.string.app_storage))

        val applicationInfo = packageInfo.applicationInfo
        if (applicationInfo != null) {
            var realSize = File(applicationInfo.sourceDir).length()
            if (!applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                for (split in applicationInfo.splitSourceDirs) {
                    realSize += File(split).length()
                }
            }
            InfoRow(stringResource(R.string.apks_size), stringResource(R.string.apks_size_extended_value, formatBytes(realSize), formatBytes(stats.appBytes)))
        }
        else {
            InfoRow(stringResource(R.string.apks_size), formatBytes(stats.appBytes))
        }

        InfoRow(stringResource(R.string.data_size), formatBytes(stats.dataBytes))

        InfoRow(stringResource(R.string.cache_size), formatBytes(stats.cacheBytes))
    }
}

@Composable
private fun SigningSection(packageInfo: PackageInfo) {
    val si = packageInfo.signingInfo ?: return
    val certs = si.signingCertificateHistory ?: return
    if (certs.isEmpty()) return
    SectionTitle(stringResource(R.string.signatures))
    val signerLabel = stringResource(R.string.signer_single)
    val ctx = LocalContext.current
    certs.forEachIndexed { i, cert ->
        val sha256 = try {
            val cf = CertificateFactory.getInstance("X.509")
            val x509 = cf.generateCertificate(ByteArrayInputStream(cert.toByteArray()))
            val digest = MessageDigest.getInstance("SHA-256").digest(x509.encoded)
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) { "?" }
        val label = if (certs.size > 1) "$signerLabel ${i + 1}" else signerLabel
        Row {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = sha256,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    val clip = ClipData.newPlainText("SHA-256", sha256)
                    (ctx.getSystemService(ClipboardManager::class.java))
                        ?.setPrimaryClip(clip)
                    Toast.makeText(ctx, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun ManifestComponentsSection(packageInfo: PackageInfo) {
    val activities = packageInfo.activities?.filter { it.name.startsWith(packageInfo.packageName) }?.toList() ?: emptyList()
    val services = packageInfo.services?.filter { it.name.startsWith(packageInfo.packageName) }?.toList() ?: emptyList()
    val receivers = packageInfo.receivers?.filter { it.name.startsWith(packageInfo.packageName) }?.toList() ?: emptyList()
    val providers = packageInfo.providers?.filter { it.name.startsWith(packageInfo.packageName) }?.toList() ?: emptyList()
    if (activities.isEmpty() && services.isEmpty() && receivers.isEmpty() && providers.isEmpty()) return
    SectionTitle(stringResource(R.string.manifest_components))

    if (activities.isNotEmpty()) {
        InfoRow(stringResource(R.string.activities), activities.size.toString())
    }

    if (services.isNotEmpty()) {
        InfoRow(stringResource(R.string.services), services.size.toString())
    }

    if (receivers.isNotEmpty()) {
        InfoRow(stringResource(R.string.receivers), receivers.size.toString())
    }

    if (providers.isNotEmpty()) {
        InfoRow(stringResource(R.string.providers), providers.size.toString())
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    else -> "%.1f KB".format(bytes / 1000.0)
}

private fun isUsageStatsGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun SpecialPermissionsSection(packageInfo: PackageInfo, declaredPermissions: Set<String>) {
    val context = LocalContext.current
    val specials = remember(packageInfo, declaredPermissions) {
        val names = mutableListOf<String>()
        fun has(perm: String) = perm in declaredPermissions
        if (has("android.permission.SYSTEM_ALERT_WINDOW")) names.add(context.getString(R.string.overlay))
        if (has("android.permission.WRITE_SETTINGS")) names.add(context.getString(R.string.write_settings))
        if (has("android.permission.REQUEST_INSTALL_PACKAGES")) names.add(context.getString(R.string.install_unknown_apps))
        if (has("android.permission.MANAGE_EXTERNAL_STORAGE")) names.add(context.getString(R.string.manage_all_files))
        val servicePerms = packageInfo.services?.map { it.permission }.orEmpty()
        val receiverPerms = packageInfo.receivers?.map { it.permission }.orEmpty()
        val allComponentPerms = servicePerms + receiverPerms
        if ("android.permission.BIND_ACCESSIBILITY_SERVICE" in allComponentPerms)
            names.add(context.getString(R.string.accessibility))
        if ("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" in allComponentPerms)
            names.add(context.getString(R.string.notification_listener))
        if ("android.permission.BIND_DEVICE_ADMIN" in allComponentPerms)
            names.add(context.getString(R.string.device_admin))
        names
    }
    if (specials.isEmpty()) return

    SectionTitle(stringResource(R.string.special_permissions))

    specials.forEach { label ->
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
