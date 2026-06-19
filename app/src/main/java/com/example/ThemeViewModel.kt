package com.example

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ThemeRepository(application)

    private val _projectType = MutableStateFlow(ProjectType.MTZ)
    val projectType = _projectType.asStateFlow()

    private val _metadata = MutableStateFlow(ThemeMetadata())
    val metadata = _metadata.asStateFlow()

    private val _icons = MutableStateFlow<List<IconItem>>(emptyList())
    val icons = _icons.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedIcon = MutableStateFlow<IconItem?>(null)
    val selectedIcon = _selectedIcon.asStateFlow()

    private val _showMetadataEditor = MutableStateFlow(false)
    val showMetadataEditor = _showMetadataEditor.asStateFlow()

    private val _showCustomIconCreator = MutableStateFlow(false)
    val showCustomIconCreator = _showCustomIconCreator.asStateFlow()

    private val _exportFileUri = MutableStateFlow<Uri?>(null)
    val exportFileUri = _exportFileUri.asStateFlow()

    private val prefs = application.getSharedPreferences("iconedit_prefs", android.content.Context.MODE_PRIVATE)

    private val _hasAppListPermission = MutableStateFlow(
        prefs.getBoolean("has_app_list_permission", false)
    )
    val hasAppListPermission = _hasAppListPermission.asStateFlow()

    private val _hasDeniedPermission = MutableStateFlow(
        prefs.getBoolean("has_denied_permission", false)
    )
    val hasDeniedPermission = _hasDeniedPermission.asStateFlow()

    private val _nativeApps = MutableStateFlow<List<IconItem>>(emptyList())
    val nativeApps = _nativeApps.asStateFlow()

    private val _themeLoaded = MutableStateFlow(false)
    val themeLoaded = _themeLoaded.asStateFlow()

    private val _originalThemeBytes = MutableStateFlow<ByteArray?>(null)
    val originalThemeBytes = _originalThemeBytes.asStateFlow()

    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps = _showSystemApps.asStateFlow()
    
    private val _themeMode = MutableStateFlow(0) // 0: Auto, 1: Light, 2: Dark
    val themeMode = _themeMode.asStateFlow()

    private val _wallpaperBytes = MutableStateFlow<ByteArray?>(null)
    val wallpaperBytes = _wallpaperBytes.asStateFlow()

    private val _previewLauncherBytes = MutableStateFlow<ByteArray?>(null)
    val previewLauncherBytes = _previewLauncherBytes.asStateFlow()

    private val _previewLockscreenBytes = MutableStateFlow<ByteArray?>(null)
    val previewLockscreenBytes = _previewLockscreenBytes.asStateFlow()

    private val _customizeUiPrint = MutableStateFlow("ui_print \"- 正在安装自定义图标主题...\"\nui_print \"- Installing custom icon theme...\"")
    val customizeUiPrint = _customizeUiPrint.asStateFlow()

    init {
    }

    fun resetTheme() {
        _themeLoaded.value = false
        _originalThemeBytes.value = null
        _icons.value = emptyList()
        _selectedIcon.value = null
        _metadata.value = ThemeMetadata()
        _wallpaperBytes.value = null
        _previewLauncherBytes.value = null
        _previewLockscreenBytes.value = null
    }

    fun setDeniedState(denied: Boolean) {
        _hasDeniedPermission.value = denied
        prefs.edit().putBoolean("has_denied_permission", denied).apply()
    }

    fun updatePermissionStateFromSystem(context: android.content.Context, isGranted: Boolean, forceRefresh: Boolean = false) {
        _hasAppListPermission.value = isGranted
        prefs.edit().putBoolean("has_app_list_permission", isGranted).apply()
        
        if (isGranted) {
            _hasDeniedPermission.value = false
            prefs.edit().putBoolean("has_denied_permission", false).apply()
            viewModelScope.launch {
                _nativeApps.value = repository.loadInstalledApps(forceRefresh = forceRefresh)
            }
        } else {
            viewModelScope.launch {
                _nativeApps.value = repository.loadInstalledApps(forceRefresh = forceRefresh)
            }
        }
    }

    fun grantAppListPermission(granted: Boolean) {
        _hasAppListPermission.value = granted
        prefs.edit().putBoolean("has_app_list_permission", granted).apply()
        if (granted) {
            _hasDeniedPermission.value = false
            prefs.edit().putBoolean("has_denied_permission", false).apply()
            viewModelScope.launch {
                _nativeApps.value = repository.loadInstalledApps(forceRefresh = true)
            }
        } else {
            _hasDeniedPermission.value = true
            prefs.edit().putBoolean("has_denied_permission", true).apply()
        }
    }

    fun loadAppIconBitmap(packageName: String): androidx.compose.ui.graphics.ImageBitmap? {
        return repository.loadAppIconBitmap(packageName)
    }

    /**
     * Swapping active design mode or reset to preset templates
     */
    fun loadTemplate(type: ProjectType) {
        _projectType.value = type
        if (!_themeLoaded.value) {
            // The user requested to remove procedural templates.
            _metadata.value = ThemeMetadata(title = if (type == ProjectType.MTZ) "新 MTZ 主题" else "新 Magisk 模块")
            _icons.value = emptyList()
            _selectedIcon.value = null
        }
    }

    fun updateWallpaperBytes(bytes: ByteArray?) {
        _wallpaperBytes.value = bytes
    }

    fun updatePreviewLauncherBytes(bytes: ByteArray?) {
        _previewLauncherBytes.value = bytes
    }

    fun updatePreviewLockscreenBytes(bytes: ByteArray?) {
        _previewLockscreenBytes.value = bytes
    }

    fun updateCustomizeUiPrint(text: String) {
        _customizeUiPrint.value = text
    }

    fun setThemeMode(mode: Int) {
        _themeMode.value = mode
    }

    fun toggleSystemApps(show: Boolean) {
        _showSystemApps.value = show
    }

    /**
     * Modify details safely
     */
    fun updateMetadata(updater: (ThemeMetadata) -> ThemeMetadata) {
        _metadata.value = updater(_metadata.value)
    }

    /**
     * Select icon from grid to view options
     */
    fun selectIcon(icon: IconItem?) {
        _selectedIcon.value = icon
    }

    fun setMetadataEditorVisible(visible: Boolean) {
        _showMetadataEditor.value = visible
    }

    fun setCustomIconCreatorVisible(visible: Boolean) {
        _showCustomIconCreator.value = visible
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Load whole theme package from user selected MTZ or Zip
     */
    fun loadStream(inputStream: InputStream, name: String) {
        viewModelScope.launch {
            try {
                val bytes = inputStream.readBytes()
                _originalThemeBytes.value = bytes
                
                val isMagisk = name.endsWith(".zip", ignoreCase = true) && !name.contains(".mtz", ignoreCase = true)
                val type = if (isMagisk) ProjectType.MAGISK_ZIP else ProjectType.MTZ
                
                val result = if (type == ProjectType.MTZ) {
                    repository.loadMtz(java.io.ByteArrayInputStream(bytes))
                } else {
                    repository.loadMagiskZip(java.io.ByteArrayInputStream(bytes))
                }
                
                _projectType.value = type
                _metadata.value = result.metadata
                _icons.value = result.icons
                _selectedIcon.value = null
                _themeLoaded.value = true
                
                _wallpaperBytes.value = result.wallpaper
                _previewLauncherBytes.value = result.previewLauncher
                _previewLockscreenBytes.value = result.previewLockscreen
                if (result.customizeUiPrint != null) {
                    _customizeUiPrint.value = result.customizeUiPrint
                }
                
                Toast.makeText(getApplication(), "Loaded successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Failed to read zip: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                try { inputStream.close() } catch (f: Exception) {}
            }
        }
    }

    /**
     * Creates a brand-new empty project
     */
    fun createEmptyProject(type: ProjectType) {
        _originalThemeBytes.value = null
        _projectType.value = type
        if (type == ProjectType.MTZ) {
            _metadata.value = ThemeMetadata(title = "新 MTZ 主题", author = "Miuix Designer", designer = "Miuix Designer")
        } else {
            _metadata.value = ThemeMetadata(title = "新 Magisk 模块", author = "Magisk Developer")
        }
        _icons.value = emptyList()
        _selectedIcon.value = null
        _wallpaperBytes.value = null
        _previewLauncherBytes.value = null
        _previewLockscreenBytes.value = null
        _themeLoaded.value = true
    }

    /**
     * Overwrites targeted main icon with some chosen image bytes
     */
    fun replaceIconBytes(targetPackage: String, suffix: String, newBytes: ByteArray) {
        val list = _icons.value.toMutableList()
        val index = list.indexOfFirst { it.packageName == targetPackage && it.suffix == suffix }
        if (index != -1) {
            list[index] = IconItem(
                packageName = targetPackage,
                suffix = suffix,
                imageBytes = newBytes,
                matchedAppName = repository.getInstalledAppName(targetPackage)
            )
            _icons.value = list
            // Update selection focus if needed
            if (_selectedIcon.value?.packageName == targetPackage && _selectedIcon.value?.suffix == suffix) {
                _selectedIcon.value = list[index]
            }
            Toast.makeText(getApplication(), "Icon updated!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Append completely custom package icon visual mapping
     */
    fun addCustomIcon(packageName: String, imageBytes: ByteArray) {
        val list = _icons.value.toMutableList()
        // Check if package already exists
        val existingIndex = list.indexOfFirst { it.packageName == packageName && it.suffix.isEmpty() }
        val item = IconItem(
            packageName = packageName,
            suffix = "",
            imageBytes = imageBytes,
            matchedAppName = repository.getInstalledAppName(packageName)
        )
        if (existingIndex != -1) {
            list[existingIndex] = item
            Toast.makeText(getApplication(), "Replaced existing package icon!", Toast.LENGTH_SHORT).show()
        } else {
            list.add(item)
            Toast.makeText(getApplication(), "Added custom icon!", Toast.LENGTH_SHORT).show()
        }
        _icons.value = list
    }

    /**
     * Swap primary icon bytes with one of its suffix alternatives
     */
    fun swapWithAlternative(basePackage: String, targetSuffix: String) {
        val list = _icons.value.toMutableList()
        val primaryIndex = list.indexOfFirst { it.packageName == basePackage && it.suffix.isEmpty() }
        val altIndex = list.indexOfFirst { it.packageName == basePackage && it.suffix == targetSuffix }

        if (primaryIndex != -1 && altIndex != -1) {
            val primaryItem = list[primaryIndex]
            val altItem = list[altIndex]

            // Swap their image byte values
            list[primaryIndex] = primaryItem.copy(imageBytes = altItem.imageBytes)
            list[altIndex] = altItem.copy(imageBytes = primaryItem.imageBytes)

            _icons.value = list
            _selectedIcon.value = list[primaryIndex]
            Toast.makeText(getApplication(), "Swapped with alternative ${targetSuffix.replace("_", "")}!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Finds list of alternate icons loaded in package mapping
     */
    fun getAlternativesFor(packageName: String): List<IconItem> {
        return _icons.value.filter { it.packageName == packageName && it.suffix.isNotEmpty() }
    }

    /**
     * Generates finished zip bundle file of active project for saving
     */
    fun generateExportFile(cacheDir: java.io.File, moduleId: String = "custom_icons"): java.io.File {
        // clean cache dir first
        cacheDir.listFiles()?.forEach { it.delete() }
        
        val prefix = if (_projectType.value == ProjectType.MTZ) "mtz" else "zip"
        val tempFile = java.io.File(cacheDir, "export_${System.currentTimeMillis()}.$prefix")
        
        val templateBytes = _originalThemeBytes.value
        return if (templateBytes != null) {
            if (_projectType.value == ProjectType.MTZ) {
                repository.exportMtzWithTemplate(
                    templateBytes,
                    _metadata.value,
                    _icons.value,
                    tempFile,
                    _wallpaperBytes.value,
                    _previewLauncherBytes.value,
                    _previewLockscreenBytes.value
                )
            } else {
                repository.exportMagiskZipWithTemplate(
                    templateBytes,
                    _metadata.value,
                    _icons.value,
                    moduleId,
                    tempFile,
                    _customizeUiPrint.value
                )
            }
        } else {
            if (_projectType.value == ProjectType.MTZ) {
                repository.exportMtz(
                    _metadata.value,
                    _icons.value,
                    tempFile,
                    _wallpaperBytes.value,
                    _previewLauncherBytes.value,
                    _previewLockscreenBytes.value
                )
            } else {
                repository.exportMagiskZip(
                    _metadata.value,
                    _icons.value,
                    moduleId,
                    tempFile,
                    _customizeUiPrint.value
                )
            }
        }
    }
}
