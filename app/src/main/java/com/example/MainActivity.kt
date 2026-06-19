package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRow
import androidx.compose.material3.Tab
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import java.io.File
import java.io.FileOutputStream

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn

class MainActivity : ComponentActivity() {

    private val viewModel: ThemeViewModel by viewModels()

    override fun onDestroy() {
        super.onDestroy()
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    override fun onResume() {
        super.onResume()
        val isSystemGranted = isAppListPermissionGranted(this)
        viewModel.updatePermissionStateFromSystem(this, isSystemGranted)
        
        val prefs = getSharedPreferences("iconedit_prefs", android.content.Context.MODE_PRIVATE)
        if (!isSystemGranted) {
            val hasPrompted = prefs.getBoolean("has_prompted_once", false)
            val canAskSystem = !hasPrompted || androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.GET_INSTALLED_APPS")
            if (canAskSystem) {
                viewModel.setDeniedState(false)
            } else {
                viewModel.setDeniedState(true)
            }
        } else {
            viewModel.setDeniedState(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val file = File(applicationContext.getExternalFilesDir(null), "crash_log.txt")
            file.writeText("Crash in thread ${thread.name}: ${throwable.stackTraceToString()}")
        }
        
        enableEdgeToEdge()

        setContent {
            val myNavigationDispatcherOwner = remember {
                object : androidx.navigationevent.NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = androidx.navigationevent.NavigationEventDispatcher()
                }
            }
            CompositionLocalProvider(
                LocalNavigationEventDispatcherOwner provides myNavigationDispatcherOwner
            ) {
                var currentTab by remember { mutableStateOf(0) }
                val themeMode by viewModel.themeMode.collectAsState()
                val isDarkTheme = when (themeMode) {
                    1 -> false
                    2 -> true
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }
                
                val activeColors = if (isDarkTheme) top.yukonga.miuix.kmp.theme.darkColorScheme() else top.yukonga.miuix.kmp.theme.lightColorScheme()
                
                MiuixTheme(colors = activeColors) {
                    androidx.compose.runtime.CompositionLocalProvider(top.yukonga.miuix.kmp.theme.LocalIsDarkTheme provides isDarkTheme) {
                        val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

                val projectType by viewModel.projectType.collectAsState()
                val metadata by viewModel.metadata.collectAsState()
                val icons by viewModel.icons.collectAsState()
                val nativeApps by viewModel.nativeApps.collectAsState()
                val themeLoaded by viewModel.themeLoaded.collectAsState()
                val showSystemApps by viewModel.showSystemApps.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val selectedIcon by viewModel.selectedIcon.collectAsState()
                val showMetadataEditor by viewModel.showMetadataEditor.collectAsState()
                val showCustomIconCreator by viewModel.showCustomIconCreator.collectAsState()
                val hasAppListPermission by viewModel.hasAppListPermission.collectAsState()

                // State holding for icon override targeting
                var pendingReplaceIcon by remember { mutableStateOf<IconItem?>(null) }
                var currentSubPage by remember { mutableStateOf("home") }
                var secondaryScreen by remember { mutableStateOf<String?>(null) }
                var showMenuDialog by remember { mutableStateOf(false) }
                val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior()
                // State for new custom icon creator
                var tempCreatorPackageName by remember { mutableStateOf("") }
                var tempCreatorImageBytes by remember { mutableStateOf<ByteArray?>(null) }
                
                val prefs = remember { getSharedPreferences("iconedit_prefs", android.content.Context.MODE_PRIVATE) }
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    prefs.edit().putBoolean("has_prompted_once", true).apply()
                    viewModel.grantAppListPermission(isGranted)
                }

                LaunchedEffect(Unit) {
                    val isSystemGranted = isAppListPermissionGranted(this@MainActivity)
                    viewModel.updatePermissionStateFromSystem(this@MainActivity, isSystemGranted, forceRefresh = true)
                    
                    if (!isSystemGranted) {
                        viewModel.setDeniedState(true) // Start out denying so user has to explicitly click "Grant Permission"
                    } else {
                        viewModel.setDeniedState(false)
                    }
                }

                var showExitConfirmDialog by remember { mutableStateOf(false) }

                LaunchedEffect(themeLoaded) {
                    currentTab = 0
                    secondaryScreen = null
                }

                BackHandler(enabled = true) {
                    if (secondaryScreen != null) {
                        secondaryScreen = null
                    } else if (themeLoaded) {
                        if (currentTab != 0) {
                            currentTab = 0
                        } else {
                            showExitConfirmDialog = true
                        }
                    } else {
                        if (currentTab != 0) {
                            currentTab = 0
                        } else {
                            finish()
                        }
                    }
                }

                // Contracts definition for file picking & document generation
                val openFileLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        try {
                            val name = getFileName(uri) ?: "theme.mtz"
                            contentResolver.openInputStream(uri)?.use { stream ->
                                viewModel.loadStream(stream, name)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Failed to load bundle: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Image processor for 1:1 and max 1024*1024
                fun processUploadedImageBytes(bytes: ByteArray): ByteArray {
                    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw Exception("Invalid image data")
                    if (bitmap.width != bitmap.height) {
                        val size = minOf(bitmap.width, bitmap.height)
                        val x = (bitmap.width - size) / 2
                        val y = (bitmap.height - size) / 2
                        bitmap = Bitmap.createBitmap(bitmap, x, y, size, size)
                    }
                    if (bitmap.width > 1024) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, 1024, 1024, true)
                    }
                    val bos = java.io.ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
                    return bos.toByteArray()
                }

                val pickImageForReplaceLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null && pendingReplaceIcon != null) {
                        try {
                            contentResolver.openInputStream(uri)?.use { stream ->
                                val processedBytes = processUploadedImageBytes(stream.readBytes())
                                viewModel.replaceIconBytes(
                                    pendingReplaceIcon!!.packageName,
                                    pendingReplaceIcon!!.suffix,
                                    processedBytes
                                )
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val pickImageForCreatorLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        try {
                            contentResolver.openInputStream(uri)?.use { stream ->
                                val processedBytes = processUploadedImageBytes(stream.readBytes())
                                tempCreatorImageBytes = processedBytes
                                Toast.makeText(this, "Image loaded cleanly!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val pickWallpaperLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        try {
                            contentResolver.openInputStream(uri)?.use { stream ->
                                viewModel.updateWallpaperBytes(stream.readBytes())
                                Toast.makeText(this, "桌面壁纸已上传 (Wallpaper updated)", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "读取壁纸失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val pickPreviewLauncherLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        try {
                            contentResolver.openInputStream(uri)?.use { stream ->
                                viewModel.updatePreviewLauncherBytes(stream.readBytes())
                                Toast.makeText(this, "桌面预览图已上传 (Launcher preview updated)", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "读取桌面预览图失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val pickPreviewLockscreenLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        try {
                            contentResolver.openInputStream(uri)?.use { stream ->
                                viewModel.updatePreviewLockscreenBytes(stream.readBytes())
                                Toast.makeText(this, "锁屏预览图已上传 (Lockscreen preview updated)", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "读取锁屏预览图失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                val exportDocumentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
                ) { uri ->
                    if (uri != null) {
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val defaultId = metadata.title.lowercase().replace(" ", "_").ifEmpty { "custom_icons" }
                                val exportedFile = viewModel.generateExportFile(cacheDir, defaultId)
                                contentResolver.openOutputStream(uri)?.use { os ->
                                    exportedFile.inputStream().use { input ->
                                        input.copyTo(os)
                                    }
                                }
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(this@MainActivity, "导出成功 (Exported)!", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(this@MainActivity, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

                val bottomBar: @Composable () -> Unit = {
                    MyBottomBar(
                        currentTab = currentTab,
                        themeLoaded = themeLoaded,
                        onTabSelected = { selectedTab ->
                            currentTab = selectedTab
                        }
                    )
                }

                top.yukonga.miuix.kmp.basic.Scaffold(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (secondaryScreen == "visual_config") {
                        VisualConfigScreen(
                            metadata = metadata,
                            projectType = projectType,
                            viewModel = viewModel,
                            onBottomBar = {},
                            onPickWallpaper = { pickWallpaperLauncher.launch("image/*") },
                            onPickPreviewL = { pickPreviewLauncherLauncher.launch("image/*") },
                            onPickPreviewS = { pickPreviewLockscreenLauncher.launch("image/*") },
                            onExport = { defaultName ->
                                exportDocumentLauncher.launch(defaultName)
                            },
                            context = this@MainActivity,
                            onBack = { secondaryScreen = null }
                        )
                    } else if (!themeLoaded) {
                        when (currentTab) {
                            0 -> {
                                HomeScreen(
                                    nativeApps = nativeApps,
                                    showSystemApps = showSystemApps,
                                    hasAppListPermission = hasAppListPermission,
                                    onShowPermissionRequest = {
                                        val hasPrompted = prefs.getBoolean("has_prompted_once", false)
                                        val canAskSystem = !hasPrompted || androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, "android.permission.GET_INSTALLED_APPS")
                                        if (canAskSystem) {
                                            requestPermissionLauncher.launch("android.permission.GET_INSTALLED_APPS")
                                        } else {
                                            try {
                                                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", packageName, null)
                                                }
                                                startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(this@MainActivity, "请去设置页面手动开启权限", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onImportRequested = {
                                        if (projectType == ProjectType.MTZ) {
                                            openFileLauncher.launch("*/*")
                                        } else {
                                            openFileLauncher.launch("application/zip")
                                        }
                                    },
                                    onBottomBar = bottomBar,
                                    viewModel = viewModel
                                )
                            }
                            1 -> {
                                SettingsScreen(
                                    themeMode = themeMode,
                                    onThemeModeChange = { viewModel.setThemeMode(it) },
                                    onBottomBar = bottomBar
                                )
                            }
                        }
                    } else {
                        when (currentTab) {
                            0 -> {
                                IconPageScreen(
                                    icons = icons,
                                    nativeApps = nativeApps,
                                    hasAppListPermission = hasAppListPermission,
                                    showSystemApps = showSystemApps,
                                    onIconSelected = { icon ->
                                        viewModel.selectIcon(icon)
                                    },
                                    onCustomIconRequested = {
                                        viewModel.setCustomIconCreatorVisible(true)
                                    },
                                    onCloseProjectRequested = {
                                        showExitConfirmDialog = true
                                    },
                                    onBottomBar = bottomBar,
                                    onEnterVisualConfigRequested = {
                                        secondaryScreen = "visual_config"
                                    },
                                    viewModel = viewModel
                                )
                            }
                            1 -> {
                                SettingsScreen(
                                    themeMode = themeMode,
                                    onThemeModeChange = { viewModel.setThemeMode(it) },
                                    onBottomBar = bottomBar
                                )
                            }
                        }
                    }

                    // 5. Interactive Floating Selected Icon Workbench Detail Overlay Sheet
                    WindowBottomSheet(
                        show = selectedIcon != null,
                        title = "编辑样式",
                        onDismissRequest = { viewModel.selectIcon(null) }
                    ) {
                        val icon = selectedIcon
                        if (icon != null) {
                            val alternatives = viewModel.getAlternativesFor(icon.packageName)
                            val dismiss = top.yukonga.miuix.kmp.theme.LocalDismissState.current
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                // Header Row details
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Large Icon Display
                                        Box(
                                            modifier = Modifier
                                                .size(96.dp)
                                                .clip(RoundedCornerShape(0.dp))
                                                .background(colors.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            icon.imageBitmap?.let { bmp ->
                                                Image(
                                                    bitmap = bmp,
                                                    contentDescription = icon.packageName,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } ?: Icon(
                                                Icons.Default.Image,
                                                contentDescription = "损坏"
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = icon.matchedAppName ?: "APP",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = colors.onSurface
                                            )
                                            Text(
                                                text = icon.packageName,
                                                fontSize = 12.sp,
                                                color = colors.onSurface.copy(alpha=0.6f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (icon.matchedAppName != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 4.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(colors.primary.copy(alpha = 0.1f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "检测到本地应用",
                                                        fontSize = 9.sp,
                                                        color = colors.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Actions Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Option 1: Replace / Upload Image Override
                                    top.yukonga.miuix.kmp.basic.Button(
                                        onClick = {
                                            pendingReplaceIcon = icon
                                            dismiss?.invoke()
                                            pickImageForReplaceLauncher.launch("image/*")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            top.yukonga.miuix.kmp.basic.Icon(
                                                Icons.Default.Image,
                                                contentDescription = "导入替代图",
                                                tint = colors.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            top.yukonga.miuix.kmp.basic.Text("导入图片覆盖", fontSize = 12.sp, color = colors.onPrimary)
                                        }
                                    }
                                }

                                // Option 2: Choose Alternatives if any exist in package list
                                if (alternatives.isNotEmpty()) {
                                    Text(
                                        text = "可用样式:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        // Also display current primary as first choice
                                        AlternativeRow(
                                            item = icon,
                                            isSelected = true,
                                            label = "默认",
                                            onClick = {}
                                        )

                                        for (alt in alternatives) {
                                            AlternativeRow(
                                                item = alt,
                                                isSelected = false,
                                                label = "样式 " + alt.suffix.replace("_", ""),
                                                onClick = {
                                                    viewModel.swapWithAlternative(icon.packageName, alt.suffix)
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }

                    // 7. Custom Package Selector & Creator Dialog Modal
                    WindowBottomSheet(
                        show = showCustomIconCreator,
                        title = "手动选择包名导入新图标",
                        onDismissRequest = { viewModel.setCustomIconCreatorVisible(false) }
                    ) {
                        val dismiss = top.yukonga.miuix.kmp.theme.LocalDismissState.current
                        Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TextField(
                                    value = tempCreatorPackageName,
                                    onValueChange = { tempCreatorPackageName = it.trim() },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = "输入包名"
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Image selector representation
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(0.dp))
                                        .background(colors.surfaceVariant)
                                        .clickable { pickImageForCreatorLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (tempCreatorImageBytes != null) {
                                        val bmp = android.graphics.BitmapFactory.decodeByteArray(
                                            tempCreatorImageBytes, 0, tempCreatorImageBytes!!.size
                                        )
                                        bmp?.let {
                                            Image(
                                                bitmap = it.asImageBitmap(),
                                                contentDescription = "预览图",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.AddPhotoAlternate,
                                                contentDescription = "选择图片",
                                                tint = colors.onSurface.copy(alpha=0.6f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Text("点击选择图片", fontSize = 10.sp, color = colors.onSurface.copy(alpha=0.6f))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    top.yukonga.miuix.kmp.basic.TextButton(
                                        "取消",
                                        onClick = {
                                            dismiss?.invoke()
                                            tempCreatorPackageName = ""
                                            tempCreatorImageBytes = null
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    top.yukonga.miuix.kmp.basic.Button(
                                        onClick = {
                                            val pName = tempCreatorPackageName
                                            val bytes = tempCreatorImageBytes
                                            if (pName.isNotEmpty() && bytes != null) {
                                                viewModel.addCustomIcon(pName, bytes)
                                                dismiss?.invoke()
                                                tempCreatorPackageName = ""
                                                tempCreatorImageBytes = null
                                            } else {
                                                Toast.makeText(this@MainActivity, "请填写完整包名并选择图片", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        top.yukonga.miuix.kmp.basic.Text("导入", color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                    // 7b. Exit theme confirmation dialog
                    OverlayDialog(
                        title = "确认返回",
                        show = showExitConfirmDialog,
                        onDismissRequest = { showExitConfirmDialog = false }
                    ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "确认要放弃当前编辑并回到主页吗？",
                                    fontSize = 14.sp,
                                    color = colors.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    top.yukonga.miuix.kmp.basic.TextButton(
                                        "取消",
                                        onClick = { showExitConfirmDialog = false }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    top.yukonga.miuix.kmp.basic.Button(
                                        onClick = {
                                            showExitConfirmDialog = false
                                            viewModel.resetTheme()
                                        }
                                    ) {
                                        top.yukonga.miuix.kmp.basic.Text("确认", color = Color.White)
                                    }
                                }
                            }
                        }

                    // Custom self-drawn permission prompt was removed to utilize Native System Permission Popup
                    }
                    }
                }
                }
                }
            }
        }

    /**
     * Compile current in-memory zip asset and invoke standard share sheet
     */
    private fun shareCompiledArchive() {
        try {
            val title = viewModel.metadata.value.title.ifEmpty { "miuix_theme" }.lowercase().replace(" ", "_")
            val exportedFile = viewModel.generateExportFile(cacheDir, title)
            
            // Standard Android FileProvider shares
            val auth = "$packageName.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(this, auth, exportedFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_TITLE, exportedFile.name)
            }
            startActivity(Intent.createChooser(intent, "分享主题包..."))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to compile share: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    private fun isAppListPermissionGranted(context: android.content.Context): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            "android.permission.GET_INSTALLED_APPS"
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun IconTileLayout(
    icon: IconItem,
    onClick: () -> Unit
) {
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(1.dp, colors.surfaceVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Visual Drawing
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                icon.imageBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = icon.packageName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: Icon(
                    Icons.Default.Apps,
                    contentDescription = "未适配",
                    tint = colors.onSurface.copy(alpha=0.6f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Attached Name Details
            Text(
                text = icon.matchedAppName ?: icon.packageName.substringAfterLast("."),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = icon.packageName,
                fontSize = 8.sp,
                color = colors.onSurface.copy(alpha=0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

@Composable
fun AlternativeRow(
    item: IconItem,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            item.imageBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = item.packageName,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = colors.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        top.yukonga.miuix.kmp.basic.RadioButton(
            selected = isSelected,
            onClick = { onClick() }
        )
    }
}

@Composable
fun OverlayDialog(
    title: String,
    show: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    if (show) {
        Dialog(onDismissRequest = onDismissRequest) {
            top.yukonga.miuix.kmp.basic.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(28.dp)),
                color = MiuixTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
fun WindowBottomSheet(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    top.yukonga.miuix.kmp.overlay.OverlayBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
        cornerRadius = 28.dp,
        content = content
    )
}

@Composable
fun WindowDialog(
    title: String,
    summary: String? = null,
    show: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    if (show) {
        Dialog(onDismissRequest = onDismissRequest) {
            top.yukonga.miuix.kmp.basic.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(28.dp)),
                color = MiuixTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = if (summary.isNullOrEmpty()) 16.dp else 8.dp)
                    )
                    if (!summary.isNullOrEmpty()) {
                        Text(
                            text = summary,
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurface.copy(alpha=0.6f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    content()
                }
            }
        }
    }
}


@Composable
fun MediaPreviewCard(
    title: String,
    bytes: ByteArray?,
    onClick: () -> Unit
) {
    val colors = MiuixTheme.colorScheme
    androidx.compose.material3.Card(
        modifier = Modifier
            .width(100.dp)
            .height(140.dp)
            .clickable { onClick() },
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (bytes != null) {
                val bitmap = remember(bytes) {
                    try {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    androidx.compose.material3.Icon(Icons.Default.BrokenImage, contentDescription = "Error", tint = colors.onSurface.copy(alpha = 0.6f))
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.CloudUpload, contentDescription = "Upload", tint = colors.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(title, fontSize = 11.sp, color = colors.onSurface, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("默认样式", fontSize = 10.sp, color = colors.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ArrowPreference(
    title: String,
    summary: String? = null,
    onClick: () -> Unit
) {
    val colors = MiuixTheme.colorScheme
    val isDark = top.yukonga.miuix.kmp.theme.LocalIsDarkTheme.current
    val backgroundColor = if (isDark) Color(0xFF242424) else Color(0xFFFFFFFF)
    top.yukonga.miuix.kmp.basic.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = backgroundColor
    ) {
        top.yukonga.miuix.kmp.basic.BasicComponent(
            title = title,
            summary = summary,
            endActions = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "详情",
                    tint = colors.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp).padding(end = 4.dp)
                )
            },
            onClick = onClick
        )
    }
}

@Composable
fun MyBottomBar(
    currentTab: Int,
    themeLoaded: Boolean,
    onTabSelected: (Int) -> Unit
) {
    val items = if (themeLoaded) {
        listOf("项目", "设置")
    } else {
        listOf("主页", "设置")
    }
    val bottomIcons = if (themeLoaded) {
        listOf(Icons.Default.Apps, Icons.Default.Settings)
    } else {
        listOf(Icons.Default.Home, Icons.Default.Settings)
    }
    NavigationBar {
        items.forEachIndexed { index, label ->
            NavigationBarItem(
                selected = currentTab == index,
                onClick = { onTabSelected(index) },
                icon = bottomIcons[index],
                label = label
            )
        }
    }
}

@Composable
fun HomeScreen(
    nativeApps: List<IconItem>,
    showSystemApps: Boolean,
    hasAppListPermission: Boolean,
    onShowPermissionRequest: () -> Unit,
    onImportRequested: () -> Unit,
    onBottomBar: @Composable () -> Unit,
    viewModel: ThemeViewModel
) {
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior()
    var homeSearchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    val hasDeniedPermission by viewModel.hasDeniedPermission.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "IconEdit",
                largeTitle = "IconEdit",
                scrollBehavior = scrollBehavior,
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            IconButton(
                                onClick = { showNewProjectDialog = true },
                                holdDownState = showNewProjectDialog
                            ) {
                                top.yukonga.miuix.kmp.basic.Icon(
                                    imageVector = Icons.Default.NoteAdd,
                                    contentDescription = "新建项目",
                                    tint = colors.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            OverlayListPopup(
                                show = showNewProjectDialog,
                                alignment = PopupPositionProvider.Align.Start,
                                onDismissRequest = { showNewProjectDialog = false }
                            ) {
                                ListPopupColumn {
                                    DropdownImpl(
                                        text = "新建小米 MTZ 项目",
                                        optionSize = 1,
                                        isSelected = false,
                                        index = 0,
                                        onSelectedIndexChange = {
                                            showNewProjectDialog = false
                                            viewModel.createEmptyProject(ProjectType.MTZ)
                                        }
                                    )
                                    DropdownImpl(
                                        text = "新建 Magisk 模块项目",
                                        optionSize = 1,
                                        isSelected = false,
                                        index = 1,
                                        onSelectedIndexChange = {
                                            showNewProjectDialog = false
                                            viewModel.createEmptyProject(ProjectType.MAGISK_ZIP)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box {
                            IconButton(
                                onClick = { showMenuDialog = true },
                                holdDownState = showMenuDialog
                            ) {
                                top.yukonga.miuix.kmp.basic.Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多设置",
                                    tint = colors.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            OverlayListPopup(
                                show = showMenuDialog,
                                alignment = PopupPositionProvider.Align.Start,
                                onDismissRequest = { showMenuDialog = false }
                            ) {
                                ListPopupColumn {
                                    DropdownImpl(
                                        text = "显示系统应用",
                                        optionSize = 1,
                                        isSelected = showSystemApps,
                                        index = 0,
                                        onSelectedIndexChange = {
                                            viewModel.toggleSystemApps(!showSystemApps)
                                            showMenuDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = onBottomBar,
        floatingActionButton = {
            top.yukonga.miuix.kmp.basic.FloatingActionButton(
                onClick = onImportRequested,
                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
            ) {
                top.yukonga.miuix.kmp.basic.Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { androidx.compose.ui.unit.IntOffset(0, (16.dp.toPx() * (1f - scrollBehavior.state.collapsedFraction)).toInt()) }
            ) {
                SearchBar(
                    modifier = Modifier.padding(horizontal = 0.dp),
                    inputField = {
                        InputField(
                            query = homeSearchQuery,
                            onQueryChange = { homeSearchQuery = it },
                            onSearch = { searchExpanded = false },
                            expanded = searchExpanded,
                            onExpandedChange = { searchExpanded = it },
                            label = "搜索"
                        )
                    },
                    expanded = searchExpanded,
                    onExpandedChange = { searchExpanded = it },
                    outsideEndAction = {
                        Text(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    searchExpanded = false
                                    homeSearchQuery = ""
                                },
                            text = "取消",
                            color = colors.primary
                        )
                    }
                ) {
                }

                Spacer(modifier = Modifier.height(10.dp))

                val filteredApps = if (hasAppListPermission) {
                    nativeApps.filter {
                        showSystemApps || it.suffix != "system"
                    }.filter {
                        it.packageName.contains(homeSearchQuery, ignoreCase = true) ||
                        (it.matchedAppName?.contains(homeSearchQuery, ignoreCase = true) ?: false)
                    }.sortedWith(compareByDescending<IconItem> { it.imageBytes?.isNotEmpty() == true }.thenBy { it.matchedAppName ?: it.packageName })
                } else {
                    emptyList()
                }

                if (filteredApps.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!hasAppListPermission) {
                            if (hasDeniedPermission) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "未获得权限",
                                    tint = colors.primary,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "未开启”获取应用列表”权限",
                                    color = colors.onSurface.copy(alpha=0.6f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                top.yukonga.miuix.kmp.basic.Button(
                                    onClick = onShowPermissionRequest
                                ) {
                                    top.yukonga.miuix.kmp.basic.Text("申请权限授权", color = Color.White)
                                }
                            } else {
                                CircularProgressIndicator(color = colors.primary)
                            }
                        } else {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "未找到应用",
                                tint = colors.onSurface.copy(alpha=0.6f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无本地应用",
                                color = colors.onSurface.copy(alpha=0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { icon ->
                            val isDark = top.yukonga.miuix.kmp.theme.LocalIsDarkTheme.current
                            top.yukonga.miuix.kmp.basic.Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                color = if (isDark) Color(0xFF242424) else Color(0xFFFFFFFF)
                            ) {
                                top.yukonga.miuix.kmp.basic.BasicComponent(
                                    title = icon.matchedAppName ?: icon.packageName.substringAfterLast("."),
                                    summary = icon.packageName,
                                    insideMargin = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
                                    startAction = {
                                        val iconModifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(40.dp)
                                        if (icon.imageBitmap != null) {
                                            Image(
                                                bitmap = icon.imageBitmap!!,
                                                contentDescription = icon.packageName,
                                                modifier = iconModifier,
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Apps,
                                                contentDescription = "默认",
                                                tint = colors.onSurface.copy(alpha=0.6f),
                                                modifier = iconModifier
                                            )
                                        }
                                    }
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
fun IconPageScreen(
    icons: List<IconItem>,
    nativeApps: List<IconItem>,
    hasAppListPermission: Boolean,
    showSystemApps: Boolean,
    onIconSelected: (IconItem) -> Unit,
    onCustomIconRequested: () -> Unit,
    onCloseProjectRequested: () -> Unit,
    onBottomBar: @Composable () -> Unit,
    onEnterVisualConfigRequested: () -> Unit,
    viewModel: ThemeViewModel
) {
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior()
    var iconSearchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var showMenuDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "图标管理",
                largeTitle = "图标管理",
                scrollBehavior = scrollBehavior,
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            IconButton(
                                onClick = { showMenuDialog = true },
                                holdDownState = showMenuDialog
                            ) {
                                top.yukonga.miuix.kmp.basic.Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多设置",
                                    tint = colors.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            OverlayListPopup(
                                show = showMenuDialog,
                                alignment = PopupPositionProvider.Align.Start,
                                onDismissRequest = { showMenuDialog = false }
                            ) {
                                ListPopupColumn {
                                    DropdownImpl(
                                        text = "显示系统应用",
                                        optionSize = 1,
                                        isSelected = showSystemApps,
                                        index = 0,
                                        onSelectedIndexChange = {
                                            viewModel.toggleSystemApps(!showSystemApps)
                                            showMenuDialog = false
                                        }
                                    )
                                    DropdownImpl(
                                        text = "关闭当前项目",
                                        optionSize = 1,
                                        isSelected = false,
                                        index = 0,
                                        onSelectedIndexChange = {
                                            showMenuDialog = false
                                            onCloseProjectRequested()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = onBottomBar,
        floatingActionButton = {
            top.yukonga.miuix.kmp.basic.FloatingActionButton(
                onClick = onCustomIconRequested,
                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
            ) {
                top.yukonga.miuix.kmp.basic.Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新增图标",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { androidx.compose.ui.unit.IntOffset(0, (16.dp.toPx() * (1f - scrollBehavior.state.collapsedFraction)).toInt()) }
            ) {
                SearchBar(
                    modifier = Modifier.padding(horizontal = 0.dp),
                    inputField = {
                        InputField(
                            query = iconSearchQuery,
                            onQueryChange = { iconSearchQuery = it },
                            onSearch = { searchExpanded = false },
                            expanded = searchExpanded,
                            onExpandedChange = { searchExpanded = it },
                            label = "搜索"
                        )
                    },
                    expanded = searchExpanded,
                    onExpandedChange = { searchExpanded = it },
                    outsideEndAction = {
                        Text(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    searchExpanded = false
                                    iconSearchQuery = ""
                                },
                            text = "取消",
                            color = colors.primary
                        )
                    }
                ) {
                }

                Spacer(modifier = Modifier.height(12.dp))

                ArrowPreference(
                    title = "信息编辑",
                    summary = "自定义主题的名称、作者、壁纸等配置",
                    onClick = onEnterVisualConfigRequested
                )

                Spacer(modifier = Modifier.height(10.dp))

                val packPackageNames = icons.map { it.packageName }.toSet()
                val fromPack = icons.filter { it.suffix.isEmpty() }.map { icon ->
                    val matchedAppName = if (hasAppListPermission) {
                        nativeApps.find { it.packageName == icon.packageName }?.matchedAppName
                    } else {
                        null
                    }
                    icon.copy(matchedAppName = matchedAppName)
                }
                val unadapted = if (hasAppListPermission) {
                    nativeApps.filter { it.packageName !in packPackageNames && (showSystemApps || it.suffix != "system") }
                } else emptyList()

                val combinedIcons = (fromPack + unadapted).filter {
                    it.packageName.contains(iconSearchQuery, ignoreCase = true) ||
                    (it.matchedAppName?.contains(iconSearchQuery, ignoreCase = true) ?: false)
                }.sortedWith(compareByDescending<IconItem> { packPackageNames.contains(it.packageName) }.thenBy { it.matchedAppName ?: it.packageName })

                val filteredIcons = combinedIcons

                if (filteredIcons.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "无搜索结果",
                            tint = colors.onSurface.copy(alpha=0.6f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "无对应包名图标",
                            color = colors.onSurface.copy(alpha=0.6f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredIcons, key = { it.packageName }) { icon ->
                            val isUnadapted = !packPackageNames.contains(icon.packageName)
                            val showAppName = !isUnadapted || icon.matchedAppName != null
                            val isDark = top.yukonga.miuix.kmp.theme.LocalIsDarkTheme.current
                            top.yukonga.miuix.kmp.basic.Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                color = if (isDark) Color(0xFF242424) else Color(0xFFFFFFFF)
                            ) {
                                top.yukonga.miuix.kmp.basic.BasicComponent(
                                    title = if (showAppName) (icon.matchedAppName ?: icon.packageName.substringAfterLast(".")) else icon.packageName,
                                    summary = if (showAppName) icon.packageName else null,
                                    insideMargin = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
                                    startAction = {
                                        val iconModifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(40.dp)
                                        
                                        var bmp by remember(icon.packageName) { mutableStateOf(icon.imageBitmap) }
                                        LaunchedEffect(icon.packageName) {
                                            if (bmp == null && isUnadapted) {
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    val loaded = viewModel.loadAppIconBitmap(icon.packageName)
                                                    if (loaded != null) {
                                                        bmp = loaded
                                                    }
                                                }
                                            }
                                        }

                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp!!,
                                                contentDescription = icon.packageName,
                                                modifier = iconModifier,
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Apps,
                                                contentDescription = "默认",
                                                tint = colors.onSurface.copy(alpha=0.6f),
                                                modifier = iconModifier
                                            )
                                        }
                                    },
                                    endActions = {
                                        if (isUnadapted) {
                                            Text(
                                                text = "未适配",
                                                fontSize = 14.sp,
                                                color = colors.primary,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                        }
                                    },
                                    onClick = { onIconSelected(icon) }
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
fun VisualConfigScreen(
    metadata: ThemeMetadata,
    projectType: ProjectType,
    viewModel: ThemeViewModel,
    onBottomBar: @Composable () -> Unit,
    onPickWallpaper: () -> Unit,
    onPickPreviewL: () -> Unit,
    onPickPreviewS: () -> Unit,
    onExport: (String) -> Unit,
    context: android.content.Context,
    onBack: (() -> Unit)? = null
) {
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior()
    val isDark = top.yukonga.miuix.kmp.theme.LocalIsDarkTheme.current
    val basicBgColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF242424) else androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    
    var titleText by remember(metadata) { mutableStateOf(metadata.title) }
    var authorText by remember(metadata) { mutableStateOf(metadata.author) }
    var designerText by remember(metadata) { mutableStateOf(metadata.designer) }
    var versionText by remember(metadata) { mutableStateOf(metadata.version) }
    var descText by remember(metadata) { mutableStateOf(metadata.description) }

    val wallBytes by viewModel.wallpaperBytes.collectAsState()
    val lBytes by viewModel.previewLauncherBytes.collectAsState()
    val sBytes by viewModel.previewLockscreenBytes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "信息编辑",
                largeTitle = "信息编辑",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = colors.onSurface
                            )
                        }
                    }
                }
            )
        },
        bottomBar = onBottomBar
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Topic card 1: Package Type (MTZ vs Magisk Module)
                Text(
                    text = "打包类型",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                TabRow(
                    selectedTabIndex = if (projectType == ProjectType.MTZ) 0 else 1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = projectType == ProjectType.MTZ,
                        onClick = { viewModel.loadTemplate(ProjectType.MTZ) },
                        text = { Text("小米 MTZ 主题包") }
                    )
                    Tab(
                        selected = projectType == ProjectType.MAGISK_ZIP,
                        onClick = { viewModel.loadTemplate(ProjectType.MAGISK_ZIP) },
                        text = { Text("Magisk 图标模组") }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Topic card 2: Metadata configurator Form
                Text("编辑信息", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.onSurface, modifier = Modifier.padding(bottom = 8.dp))

                top.yukonga.miuix.kmp.basic.Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = basicBgColor
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        TextField(
                            value = titleText,
                            onValueChange = {
                                titleText = it
                                viewModel.updateMetadata { meta -> meta.copy(title = it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = "主题名"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TextField(
                            value = authorText,
                            onValueChange = {
                                authorText = it
                                viewModel.updateMetadata { meta -> meta.copy(author = it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = "作者"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TextField(
                            value = designerText,
                            onValueChange = {
                                designerText = it
                                viewModel.updateMetadata { meta -> meta.copy(designer = it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = "设计师"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TextField(
                            value = versionText,
                            onValueChange = {
                                versionText = it
                                viewModel.updateMetadata { meta -> meta.copy(version = it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = "版本号"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TextField(
                            value = descText,
                            onValueChange = {
                                descText = it
                                viewModel.updateMetadata { meta -> meta.copy(description = it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = "版本介绍与描述"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Topic card 3: Wallpaper / Previews custom selector card row
                Text("壁纸与预览图", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.onSurface, modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MediaPreviewCard(title = "桌面壁纸", bytes = wallBytes, onClick = onPickWallpaper)
                    MediaPreviewCard(title = "桌面预览", bytes = lBytes, onClick = onPickPreviewL)
                    MediaPreviewCard(title = "锁屏预览", bytes = sBytes, onClick = onPickPreviewS)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action buttons: Compile Export
                top.yukonga.miuix.kmp.basic.Button(
                    onClick = {
                        val finalId = titleText.lowercase().replace(" ", "_").ifEmpty { "miuix_theme" }
                        val endSuffix = if (projectType == ProjectType.MTZ) ".mtz" else ".zip"
                        onExport(finalId + endSuffix)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        top.yukonga.miuix.kmp.basic.Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = "Save and export",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        top.yukonga.miuix.kmp.basic.Text(
                            text = "保存并导出主题包 (${if (projectType == ProjectType.MTZ) ".mtz" else ".zip"})",
                            fontSize = 13.sp,
                            color = colors.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Composable
fun SettingsScreen(
    themeMode: Int,
    onThemeModeChange: (Int) -> Unit,
    onBottomBar: @Composable () -> Unit
) {
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = "设置",
                largeTitle = "设置",
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = onBottomBar
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val options = listOf("跟随系统", "浅色模式", "深色模式")
                OverlayDropdownPreference(
                    title = "主题模式",
                    items = options.mapIndexed { index, name -> DropdownEntry(name, index) },
                    selectedIndex = themeMode,
                    onSelectedIndexChange = onThemeModeChange
                )
            }
        }
    }
}
