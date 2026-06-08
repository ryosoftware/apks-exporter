package com.ryosoftware.apks_exporter

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.StorageStatsManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.storage.StorageManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope

import com.ryosoftware.utilities.LogUtilities
import java.text.Collator
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val _items = MutableStateFlow<List<AppItem>>(emptyList())
    val items: StateFlow<List<AppItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSelecting = MutableStateFlow(false)
    val isSelecting: StateFlow<Boolean> = _isSelecting.asStateFlow()

    private val _selectedItems = MutableStateFlow<List<AppItem>>(emptyList())
    val selectedItems: StateFlow<List<AppItem>> = _selectedItems.asStateFlow()

    private val _isSearching = MutableStateFlow(savedStateHandle["isSearching"] ?: false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchText = MutableStateFlow<String?>(savedStateHandle["searchText"])
    val searchText: StateFlow<String?> = _searchText.asStateFlow()

    private val iconCache = HashMap<String, Drawable>()

    val filteredItems: StateFlow<List<AppItem>> = combine(
        _items,
        _searchText
    ) { items, searchText ->
        val query = searchText?.lowercase()
        if (query.isNullOrEmpty()) items
        else items.filter {
            it.appLabel.lowercase().contains(query) || it.packageName.lowercase().contains(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadData()
    }

    fun loadData() {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            delay(100)
            val loadedItems = doLoad()
            _items.value = loadedItems ?: emptyList()
            if (loadedItems != null) {
                incrementSeenCounts(loadedItems)
            }
            _isLoading.value = false
        }
    }

    private suspend fun doLoad(): List<AppItem>? {
        return withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Main>()
                val packages = context.packageManager.getInstalledPackages(0)
                val currentPrefs = ApplicationPreferences.instance.dataStore.data.first()
                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

                val hasUsageStatsPermission = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                ) == AppOpsManager.MODE_ALLOWED

                if (currentPrefs[booleanPreferencesKey(ApplicationPreferences.IS_FIRST_APP_LAUNCH_TIME)] ?: true ||
                    currentPrefs[booleanPreferencesKey(ApplicationPreferences.SEED_BACKUP_DATA_FOR_ALL_APPS)] ?: false) {
                    ApplicationPreferences.batch { edit ->
                        for (packageInfo in packages) {
                            edit[longPreferencesKey(MainService.getLastAppVersionNumberKey(packageInfo.packageName))] = packageInfo.longVersionCode
                            edit[longPreferencesKey(MainService.getLastAppUpdateTimeKey(packageInfo.packageName))] = 0L
                            edit[intPreferencesKey(MainService.getLastAppUpdateNotificationTimesKey(packageInfo.packageName))] = Int.MAX_VALUE
                            edit[longPreferencesKey(MainService.getLastBackupVersionNumberKey(packageInfo.packageName))] = packageInfo.longVersionCode
                            edit[longPreferencesKey(MainService.getLastBackupTimeKey(packageInfo.packageName))] = System.currentTimeMillis()
                        }
                        edit[booleanPreferencesKey(ApplicationPreferences.IS_FIRST_APP_LAUNCH_TIME)] = false
                        edit[booleanPreferencesKey(ApplicationPreferences.SEED_BACKUP_DATA_FOR_ALL_APPS)] = false
                    }
                }

                val showSystemPackages = currentPrefs[booleanPreferencesKey(ApplicationPreferences.SHOW_SYSTEM_PACKAGES_KEY)]
                    ?: ApplicationPreferences.SHOW_SYSTEM_PACKAGES_DEFAULT
                val showSystemPackagesOnlyIfUpdated = currentPrefs[booleanPreferencesKey(ApplicationPreferences.SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_KEY)]
                    ?: ApplicationPreferences.SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_DEFAULT
                val sortUpdatedAppsFirst = currentPrefs[booleanPreferencesKey(ApplicationPreferences.SORT_UPDATED_APPS_FIRST_KEY)]
                    ?: ApplicationPreferences.SORT_UPDATED_APPS_FIRST_DEFAULT
                val autoEnableBackup = currentPrefs[booleanPreferencesKey(ApplicationPreferences.AUTO_BACKUP_APPS_KEY)]
                    ?: ApplicationPreferences.AUTO_BACKUP_APPS_DEFAULT
                val autoEnableBackupForNewApps = currentPrefs[booleanPreferencesKey(ApplicationPreferences.AUTO_BACKUP_NEW_APPS_KEY)]
                    ?: ApplicationPreferences.AUTO_BACKUP_NEW_APPS_DEFAULT

                val result = mutableListOf<AppItem>()
                for (packageInfo in packages) {
                    if (packageInfo.applicationInfo?.packageName == null) continue
                    val canAutomaticallyBackup = MainService.resolveAutoBackupPreference(packageInfo, autoEnableBackupForNewApps)
                    val isSystemApp = MainService.isSystemApplication(packageInfo)
                    val autoBackupEnabledForThis = autoEnableBackup && canAutomaticallyBackup
                    if ((!autoBackupEnabledForThis) && isSystemApp &&
                        (!showSystemPackages || (!MainService.isSystemApplicationUpdated(packageInfo) && showSystemPackagesOnlyIfUpdated))) continue

                    val packageName = packageInfo.packageName
                    var icon = iconCache[packageName]
                    if (icon == null) {
                        icon = packageInfo.applicationInfo?.loadIcon(context.packageManager)
                        if (icon != null && iconCache.size <= 256) iconCache[packageName] = icon
                    }
                    val appLabel = packageInfo.applicationInfo?.loadLabel(context.packageManager)?.toString()
                        ?: packageName

                    val stats = if (hasUsageStatsPermission) { storageStatsManager.queryStatsForPackage(
                        StorageManager.UUID_DEFAULT,
                        packageName,
                        android.os.Process.myUserHandle()
                    ) } else null

                    val item = AppItem(
                        packageInfo = packageInfo,
                        stats = stats,
                        icon = icon,
                        appLabel = appLabel
                    )
                    result.add(item)
                }

                ApplicationPreferences.cleanupOrphanedPreferences(packages)

                result.sortWith(Comparator { left, right ->
                    val leftUpdatedAndNotBacked = left.isAppUpdated && !left.isAppBacked
                    val rightUpdatedAndNotBacked = right.isAppUpdated && !right.isAppBacked
                    if (!sortUpdatedAppsFirst || leftUpdatedAndNotBacked == rightUpdatedAndNotBacked) {
                        Collator.getInstance().compare(left.appLabel, right.appLabel)
                    } else {
                        if (leftUpdatedAndNotBacked) -1 else 1
                    }
                })

                return@withContext result
            } catch (e: Exception) {
                LogUtilities.show(this@MainViewModel, e)
                return@withContext null
            }
        }
    }

    fun toggleSelection(item: AppItem) {
        val current = _selectedItems.value.toMutableList()
        if (current.contains(item)) current.remove(item)
        else current.add(item)
        _selectedItems.value = current
        _isSelecting.value = _selectedItems.value.isNotEmpty()
    }

    fun startSelection(item: AppItem) {
        _selectedItems.value = listOf(item)
        _isSelecting.value = true
    }

    private fun incrementSeenCounts(items: List<AppItem>) {
        for (item in items) {
            if (item.isAppUpdated) {
                MainService.incrementAppUpdateNotificationCount(item.packageName)
            }
        }
    }

    fun selectAll() {
        _selectedItems.value = _items.value
        _isSelecting.value = true
    }

    fun cancelSelection() {
        _selectedItems.value = emptyList()
        _isSelecting.value = false
    }

    fun startSearch() {
        _isSearching.value = true
        _searchText.value = ""
        savedStateHandle["isSearching"] = true
        savedStateHandle["searchText"] = ""
    }

    fun updateSearch(text: String) {
        _searchText.value = text
        savedStateHandle["searchText"] = text
    }

    fun clearSearch() {
        _isSearching.value = false
        _searchText.value = null
        savedStateHandle["isSearching"] = false
        savedStateHandle.remove<String>("searchText")
    }

}
