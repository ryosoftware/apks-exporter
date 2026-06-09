package com.ryosoftware.apks_exporter

import android.content.Context
import android.content.pm.PackageInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

@Singleton
class ApplicationPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        lateinit var instance: ApplicationPreferences

        const val IS_FIRST_APP_LAUNCH_TIME = "is-first-app-launch-time"
        const val SHOW_SYSTEM_PACKAGES_KEY = "show-system-packages"
        const val SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_KEY = "show-system-packages-only-if-updated"
        const val SHOW_DISABLED_APPS_KEY = "show-disabled-apps"
        const val SAVE_AS_ZIP_FILE_KEY = "save-as-zip-file"
        const val DO_NOT_COMPRESS_SINGLE_FILES_KEY = "do-not-compress-single-files"
        const val APKM_EXTENSION_KEY = "apkm-extension"
        const val USE_APP_LABEL_KEY = "use-app-label"
        const val USE_LONG_VERSION_NUMBER_KEY = "use-long-version-number"
        const val SORT_UPDATED_APPS_FIRST_KEY = "sort-updated-apps-first"
        const val SAVE_FOLDER_KEY = "save-folder"
        const val AUTO_BACKUP_APPS_KEY = "auto-backup-apps"
        const val AUTO_BACKUP_NEW_APPS_KEY = "auto-backup-new-apps"
        const val THEME_KEY = "theme"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
        const val THEME_SYSTEM = "system"
        const val BLACK_BACKGROUND_KEY = "black-background"
        const val USE_SYSTEM_ACCENT_COLOR_KEY = "system-accent-color"
        const val SEED_BACKUP_DATA_FOR_ALL_APPS = "seed-backup-data-for-all-apps"
        const val LAST_AUTO_BACKUP_TIME_KEY = "last-auto-backup-time"
        const val LAST_BACKUP_RESULTS_KEY = "last-backup-results"
        const val NEXT_AUTO_BACKUP_TIME_KEY = "next-auto-backup-time"
        const val LAST_BACKED_APP_PACKAGES_KEY = "last-backed-app-packages"
        const val LAST_BACKUP_ERRORS_COUNT_KEY = "last-backup-errors-count"
        private const val VERSION_KEY = "version"
        private const val VERSION_VALUE = 1.1f

        // Forwarding vars (delegates to instance for runtime-initialized values)
        var SHOW_SYSTEM_PACKAGES_DEFAULT: Boolean
            get() = instance.SHOW_SYSTEM_PACKAGES_DEFAULT
            set(v) { instance.SHOW_SYSTEM_PACKAGES_DEFAULT = v }
        var SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_DEFAULT: Boolean
            get() = instance.SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_DEFAULT
            set(v) { instance.SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_DEFAULT = v }
        var SHOW_DISABLED_APPS_DEFAULT: Boolean
            get() = instance.SHOW_DISABLED_APPS_DEFAULT
            set(v) { instance.SHOW_DISABLED_APPS_DEFAULT = v }
        var SAVE_AS_ZIP_FILE_DEFAULT: Boolean
            get() = instance.SAVE_AS_ZIP_FILE_DEFAULT
            set(v) { instance.SAVE_AS_ZIP_FILE_DEFAULT = v }
        var DO_NOT_COMPRESS_SINGLE_FILES_DEFAULT: Boolean
            get() = instance.DO_NOT_COMPRESS_SINGLE_FILES_DEFAULT
            set(v) { instance.DO_NOT_COMPRESS_SINGLE_FILES_DEFAULT = v }
        var APKM_EXTENSION_DEFAULT: Boolean
            get() = instance.APKM_EXTENSION_DEFAULT
            set(v) { instance.APKM_EXTENSION_DEFAULT = v }
        var USE_APP_LABEL_DEFAULT: Boolean
            get() = instance.USE_APP_LABEL_DEFAULT
            set(v) { instance.USE_APP_LABEL_DEFAULT = v }
        var USE_LONG_VERSION_NUMBER_DEFAULT: Boolean
            get() = instance.USE_LONG_VERSION_NUMBER_DEFAULT
            set(v) { instance.USE_LONG_VERSION_NUMBER_DEFAULT = v }
        var SORT_UPDATED_APPS_FIRST_DEFAULT: Boolean
            get() = instance.SORT_UPDATED_APPS_FIRST_DEFAULT
            set(v) { instance.SORT_UPDATED_APPS_FIRST_DEFAULT = v }
        var AUTO_BACKUP_APPS_DEFAULT: Boolean
            get() = instance.AUTO_BACKUP_APPS_DEFAULT
            set(v) { instance.AUTO_BACKUP_APPS_DEFAULT = v }
        var AUTO_BACKUP_NEW_APPS_DEFAULT: Boolean
            get() = instance.AUTO_BACKUP_NEW_APPS_DEFAULT
            set(v) { instance.AUTO_BACKUP_NEW_APPS_DEFAULT = v }
        val THEME_VALUES: Array<String> get() = instance.THEME_VALUES
        var THEME_DEFAULT: String
            get() = instance.THEME_DEFAULT
            set(v) { instance.THEME_DEFAULT = v }
        var BLACK_BACKGROUND_DEFAULT: Boolean
            get() = instance.BLACK_BACKGROUND_DEFAULT
            set(v) { instance.BLACK_BACKGROUND_DEFAULT = v }
        var USE_SYSTEM_ACCENT_COLOR_DEFAULT: Boolean
            get() = instance.USE_SYSTEM_ACCENT_COLOR_DEFAULT
            set(v) { instance.USE_SYSTEM_ACCENT_COLOR_DEFAULT = v }

        // Forwarding methods
        fun initialize() = instance.initialize()
        inline fun <reified T> get(name: String, defaultValue: T): T = instance.get(name, defaultValue)
        inline fun <reified T> observe(name: String, defaultValue: T): Flow<T> = instance.observe(name, defaultValue)
        inline fun <reified T> put(name: String, value: T) = instance.put(name, value)
        fun containsKey(name: String): Boolean = instance.containsKey(name)
        fun delete(name: String) = instance.delete(name)
        fun delete(names: Array<String>) = instance.delete(names)
        suspend inline fun <reified T> getSuspend(name: String, defaultValue: T): T = instance.getSuspend(name, defaultValue)
        suspend fun batch(block: suspend (MutablePreferences) -> Unit) = instance.batch(block)
        fun removePackagePreferences(packageName: String) = instance.removePackagePreferences(packageName)
        fun cleanupOrphanedPreferences(installedPackageNames: Set<String>) = instance.cleanupOrphanedPreferences(installedPackageNames)
        fun cleanupOrphanedPreferences(installedPackages: List<PackageInfo>) = instance.cleanupOrphanedPreferences(installedPackages)
    }

    var SHOW_SYSTEM_PACKAGES_DEFAULT = false
    var SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_DEFAULT = false
    var SHOW_DISABLED_APPS_DEFAULT = true
    var SAVE_AS_ZIP_FILE_DEFAULT = false
    var DO_NOT_COMPRESS_SINGLE_FILES_DEFAULT = false
    var APKM_EXTENSION_DEFAULT = false
    var USE_APP_LABEL_DEFAULT = false
    var USE_LONG_VERSION_NUMBER_DEFAULT = false
    var SORT_UPDATED_APPS_FIRST_DEFAULT = false
    var AUTO_BACKUP_APPS_DEFAULT = false
    var AUTO_BACKUP_NEW_APPS_DEFAULT = true
    val THEME_VALUES = arrayOf(THEME_DARK, THEME_LIGHT, THEME_SYSTEM)
    var THEME_DEFAULT = THEME_SYSTEM
    var BLACK_BACKGROUND_DEFAULT = false
    var USE_SYSTEM_ACCENT_COLOR_DEFAULT = false

    val dataStore: DataStore<Preferences> = context.appDataStore

    init {
        instance = this
    }

    @PublishedApi
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun upgrade(edit: MutablePreferences, fromVersion: Float) {
    }

    fun initialize() {
        initializeConstants()
        scope.launch {
            val currentVersion = dataStore.data.first()[floatPreferencesKey(VERSION_KEY)] ?: 0f
            if (currentVersion != VERSION_VALUE) {
                if (currentVersion == 0f) {
                    val oldPrefs = context.getSharedPreferences(
                        "${context.packageName}_preferences", Context.MODE_PRIVATE
                    )
                    if (oldPrefs.all.isNotEmpty()) {
                        dataStore.edit { edit ->
                            oldPrefs.all.forEach { (key, value) ->
                                when (value) {
                                    is Boolean -> edit[booleanPreferencesKey(key)] = value
                                    is Int -> edit[intPreferencesKey(key)] = value
                                    is Long -> edit[longPreferencesKey(key)] = value
                                    is String -> edit[stringPreferencesKey(key)] = value
                                    is Set<*> -> edit[stringSetPreferencesKey(key)] = value as Set<String>
                                    is Float -> edit[floatPreferencesKey(key)] = value
                                }
                            }
                            upgrade(edit, currentVersion)
                            edit[floatPreferencesKey(VERSION_KEY)] = VERSION_VALUE
                        }
                        return@launch
                    }
                }
                dataStore.edit { edit ->
                    if (currentVersion < VERSION_VALUE) upgrade(edit, currentVersion)
                    edit[floatPreferencesKey(VERSION_KEY)] = VERSION_VALUE
                }
            }
        }
    }

    private fun initializeConstants() {
        SHOW_SYSTEM_PACKAGES_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.show_system_apps_default))
        SHOW_SYSTEM_PACKAGES_ONLY_IF_UPDATED_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.show_system_apps_only_if_updated_default))
        SHOW_DISABLED_APPS_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.show_disabled_apps_default))
        SAVE_AS_ZIP_FILE_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.save_as_zip_file_default))
        DO_NOT_COMPRESS_SINGLE_FILES_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.do_not_compress_single_files_default))
        APKM_EXTENSION_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.apkm_extension_default))
        USE_APP_LABEL_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.use_app_label_default))
        USE_LONG_VERSION_NUMBER_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.use_long_version_number_default))
        SORT_UPDATED_APPS_FIRST_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.sort_updated_apps_first_default))
        AUTO_BACKUP_APPS_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.auto_backup_apps_default))
        AUTO_BACKUP_NEW_APPS_DEFAULT = java.lang.Boolean.parseBoolean((context.getString(R.string.auto_backup_new_apps_default)))
        val themeDefault = context.getString(R.string.theme_default)
        if (themeDefault in THEME_VALUES) THEME_DEFAULT = themeDefault
        BLACK_BACKGROUND_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.black_background_default))
        USE_SYSTEM_ACCENT_COLOR_DEFAULT = java.lang.Boolean.parseBoolean(context.getString(R.string.use_system_accent_color_default))
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> get(name: String, defaultValue: T): T {
        val key = when (T::class) {
            Boolean::class -> booleanPreferencesKey(name)
            Int::class -> intPreferencesKey(name)
            Long::class -> longPreferencesKey(name)
            String::class -> stringPreferencesKey(name)
            Float::class -> floatPreferencesKey(name)
            Set::class -> stringSetPreferencesKey(name)
            else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
        }
        return runBlocking {
            (dataStore.data.first()[key as Preferences.Key<T>] ?: defaultValue)
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> observe(name: String, defaultValue: T): Flow<T> {
        val key = when (T::class) {
            Boolean::class -> booleanPreferencesKey(name)
            Int::class -> intPreferencesKey(name)
            Long::class -> longPreferencesKey(name)
            String::class -> stringPreferencesKey(name)
            Float::class -> floatPreferencesKey(name)
            Set::class -> stringSetPreferencesKey(name)
            else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
        }
        return dataStore.data.map { it[key as Preferences.Key<T>] ?: defaultValue }
    }

    fun removePackagePreferences(packageName: String) {
        scope.launch {
            dataStore.edit { prefs ->
                val keysToRemove = mutableListOf<Preferences.Key<*>>()
                for (key in prefs.asMap().keys) {
                    for (suffix in MainService.APP_SUFFIXES) {
                        if (key.name == "$packageName-$suffix") {
                            keysToRemove.add(key)
                        }
                    }
                }
                for (key in keysToRemove) {
                    @Suppress("UNCHECKED_CAST")
                    prefs.remove(key as Preferences.Key<Any>)
                }
            }
        }
    }

    fun cleanupOrphanedPreferences(installedPackageNames: Set<String>) {
        scope.launch {
            dataStore.edit { prefs ->
                val keysToRemove = mutableListOf<Preferences.Key<*>>()
                for (key in prefs.asMap().keys) {
                    for (suffix in MainService.APP_SUFFIXES) {
                        if (key.name.endsWith("-$suffix")) {
                            val packageName = key.name.removeSuffix("-$suffix")
                            if (packageName !in installedPackageNames) {
                                keysToRemove.add(key)
                            }
                        }
                    }
                }
                for (key in keysToRemove) {
                    @Suppress("UNCHECKED_CAST")
                    prefs.remove(key as Preferences.Key<Any>)
                }
            }
        }
    }

    fun cleanupOrphanedPreferences(installedPackages: List<PackageInfo>) {
        val installedPackageNames = installedPackages.mapNotNull { it.applicationInfo?.packageName }.toSet()
        cleanupOrphanedPreferences(installedPackageNames)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> put(name: String, value: T) {
        scope.launch {
            val key = when (T::class) {
                Boolean::class -> booleanPreferencesKey(name)
                Int::class -> intPreferencesKey(name)
                Long::class -> longPreferencesKey(name)
                String::class -> stringPreferencesKey(name)
                Float::class -> floatPreferencesKey(name)
                Set::class -> stringSetPreferencesKey(name)
                else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
            }
            dataStore.edit { prefs ->
                prefs[key as Preferences.Key<T>] = value
            }
        }
    }

    fun containsKey(name: String): Boolean {
        return runBlocking {
            dataStore.data.first().contains(stringPreferencesKey(name))
        }
    }

    fun delete(name: String) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs.remove(stringPreferencesKey(name))
            }
        }
    }

    fun delete(names: Array<String>) {
        scope.launch {
            dataStore.edit { prefs ->
                for (name in names) {
                    prefs.remove(stringPreferencesKey(name))
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified T> getSuspend(name: String, defaultValue: T): T {
        val key = when (T::class) {
            Boolean::class -> booleanPreferencesKey(name)
            Int::class -> intPreferencesKey(name)
            Long::class -> longPreferencesKey(name)
            String::class -> stringPreferencesKey(name)
            Float::class -> floatPreferencesKey(name)
            Set::class -> stringSetPreferencesKey(name)
            else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
        }
        return dataStore.data.first()[key as Preferences.Key<T>] ?: defaultValue
    }

    suspend fun batch(block: suspend (MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }
}
