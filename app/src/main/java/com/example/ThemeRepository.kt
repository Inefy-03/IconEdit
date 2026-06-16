package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.io.InputStream

class ThemeRepository(private val context: Context) {

    private var installedAppsMap = mapOf<String, String>()
    private var allInstalledApps = mutableListOf<IconItem>()
    private var isAppsLoaded = false

    /**
     * Load installed apps asynchronously to avoid ANR.
     */
    suspend fun loadInstalledApps(): List<IconItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!isAppsLoaded) {
            try {
                val pm = context.packageManager
                val packages = pm.getInstalledPackages(0)
                
                val apps = mutableListOf<IconItem>()
                for (pack in packages) {
                    try {
                        val pName = pack.packageName
                        val appInfo = pack.applicationInfo ?: continue
                        val path = appInfo.sourceDir ?: ""
                        val isSystem = if (path.isNotEmpty()) {
                            if (path.startsWith("/data/app/")) {
                                false
                            } else {
                                path.startsWith("/system/") ||
                                        path.startsWith("/vendor/") ||
                                        path.startsWith("/product/") ||
                                        path.startsWith("/apex/") ||
                                        path.startsWith("/oem/") ||
                                        path.startsWith("/odm/") ||
                                        ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)
                            }
                        } else {
                            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                        }
                        val label = appInfo.loadLabel(pm).toString()
                        
                        val iconDrawable = appInfo.loadIcon(pm)
                        val w = iconDrawable.intrinsicWidth.coerceAtLeast(100)
                        val h = iconDrawable.intrinsicHeight.coerceAtLeast(100)
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        iconDrawable.setBounds(0, 0, canvas.width, canvas.height)
                        iconDrawable.draw(canvas)
                        
                        val bos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
                        
                        apps.add(
                            IconItem(
                                packageName = pName,
                                suffix = if (isSystem) "system" else "user",
                                imageBytes = bos.toByteArray(),
                                matchedAppName = label
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                allInstalledApps = apps
                installedAppsMap = apps.associate { it.packageName to (it.matchedAppName ?: it.packageName) }
                isAppsLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext allInstalledApps
    }

    fun getInstalledAppName(packageName: String): String? {
        return installedAppsMap[packageName]
    }

    /**
     * Parse XML details safely to metadata token
     */
    fun parseMetadataXml(xml: String): ThemeMetadata {
        val metadata = ThemeMetadata()
        metadata.title = extractXmlTag(xml, "title") ?: "My Styled Theme"
        metadata.author = extractXmlTag(xml, "author") ?: "Miuix Designer"
        metadata.designer = extractXmlTag(xml, "designer") ?: "Miuix Designer"
        metadata.version = extractXmlTag(xml, "version") ?: "1.0.0"
        metadata.uiVersion = extractXmlTag(xml, "uiVersion") ?: "4"
        metadata.description = extractXmlTag(xml, "description") ?: "Custom MIUI Theme icon pack"
        return metadata
    }

    private fun extractXmlTag(xml: String, tag: String): String? {
        val regex = "<$tag>(.*?)</$tag>".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(xml)
        val value = match?.groupValues?.get(1)?.trim()
        return value?.removePrefix("<![CDATA[")?.removeSuffix("]]>")?.trim()
    }

    /**
     * Parse Prop details safely to metadata token
     */
    fun parseMetadataProp(prop: String): ThemeMetadata {
        val metadata = ThemeMetadata()
        val lines = prop.lines()
        for (line in lines) {
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                when (key) {
                    "name" -> metadata.title = value
                    "author" -> metadata.author = value
                    "version" -> metadata.version = value
                    "description" -> metadata.description = value
                }
            }
        }
        if (metadata.title.isEmpty()) metadata.title = "Custom Magisk Icons"
        if (metadata.author.isEmpty()) metadata.author = "Magisk Developer"
        if (metadata.version.isEmpty()) metadata.version = "1.0.0"
        if (metadata.description.isEmpty()) metadata.description = "Magisk system module for customizing icons"
        return metadata
    }

    /**
     * Load MTZ file stream and output metadata map & package icons list.
     */
    fun loadMtz(inputStream: InputStream): LoadedThemeResult {
        var metadata = ThemeMetadata()
        val iconsList = mutableListOf<IconItem>()
        var wallpaperBytes: ByteArray? = null
        var previewLauncherBytes: ByteArray? = null
        var previewLockscreenBytes: ByteArray? = null
        
        val zipIn = ZipInputStream(inputStream)
        var entry: ZipEntry? = zipIn.getNextEntry()

        while (entry != null) {
            val name = entry.name
            if (name == "description.xml") {
                val xmlContent = zipIn.readBytes().toString(Charsets.UTF_8)
                metadata = parseMetadataXml(xmlContent)
            } else if (name == "wallpaper/default_wallpaper.jpg" || name.endsWith("default_wallpaper.jpg")) {
                wallpaperBytes = zipIn.readBytes()
            } else if (name == "preview/preview_launcher_0.jpg" || name.endsWith("preview_launcher_0.jpg")) {
                previewLauncherBytes = zipIn.readBytes()
            } else if (name == "preview/preview_lockscreen_0.jpg" || name.endsWith("preview_lockscreen_0.jpg")) {
                previewLockscreenBytes = zipIn.readBytes()
            } else if (name == "icons" || name.endsWith("/icons")) {
                val innerZipBytes = zipIn.readBytes()
                val innerZipIn = ZipInputStream(ByteArrayInputStream(innerZipBytes))
                var innerEntry = innerZipIn.getNextEntry()
                while (innerEntry != null) {
                    val innerName = innerEntry.name
                    if (innerName.endsWith(".png") || innerName.endsWith(".webp")) {
                        val extension = if (innerName.endsWith(".png")) ".png" else ".webp"
                        val filename = innerName.substringAfterLast("/")
                        val fullName = filename.substringBeforeLast(extension)
                        // Split packageName and its suffix (like _1, _2)
                        val packageName = fullName.substringBeforeLast("_")
                        val suffix = if (fullName.contains("_")) "_" + fullName.substringAfterLast("_") else ""
                        val cleanedPackageName = if (suffix.isNotEmpty()) packageName else fullName

                        val imageBytes = innerZipIn.readBytes()
                        iconsList.add(
                            IconItem(
                                packageName = cleanedPackageName,
                                suffix = suffix,
                                imageBytes = imageBytes,
                                matchedAppName = getInstalledAppName(cleanedPackageName)
                            )
                        )
                    }
                    innerEntry = innerZipIn.getNextEntry()
                }
            }
            entry = zipIn.getNextEntry()
        }
        zipIn.close()
        return LoadedThemeResult(
            metadata = metadata,
            icons = iconsList,
            wallpaper = wallpaperBytes,
            previewLauncher = previewLauncherBytes,
            previewLockscreen = previewLockscreenBytes
        )
    }

    /**
     * Load Magisk Module ZIP sequence
     */
    fun loadMagiskZip(inputStream: InputStream): LoadedThemeResult {
        var metadata = ThemeMetadata()
        val iconsList = mutableListOf<IconItem>()
        var customizePrintLines: String? = null
        
        val zipIn = ZipInputStream(inputStream)
        var entry: ZipEntry? = zipIn.getNextEntry()

        while (entry != null) {
            val name = entry.name
            if (name == "module.prop" || name.endsWith("/module.prop")) {
                val propContent = zipIn.readBytes().toString(Charsets.UTF_8)
                metadata = parseMetadataProp(propContent)
            } else if (name == "customize.sh" || name.endsWith("/customize.sh")) {
                val scriptContent = zipIn.readBytes().toString(Charsets.UTF_8)
                val printLines = scriptContent.lines()
                    .filter { it.trim().startsWith("ui_print ") }
                    .joinToString("\n")
                if (printLines.isNotEmpty()) {
                    customizePrintLines = printLines
                }
            } else if (name == "system/media/theme/default/icons" || name.endsWith("/system/media/theme/default/icons") || name == "icons" || name.endsWith("/icons")) {
                val innerZipBytes = zipIn.readBytes()
                val innerZipIn = ZipInputStream(ByteArrayInputStream(innerZipBytes))
                var innerEntry = innerZipIn.getNextEntry()
                while (innerEntry != null) {
                    val innerName = innerEntry.name
                    if (innerName.endsWith(".png") || innerName.endsWith(".webp")) {
                        val extension = if (innerName.endsWith(".png")) ".png" else ".webp"
                        val filename = innerName.substringAfterLast("/")
                        val fullName = filename.substringBeforeLast(extension)
                        val packageName = fullName.substringBeforeLast("_")
                        val suffix = if (fullName.contains("_")) "_" + fullName.substringAfterLast("_") else ""
                        val cleanedPackageName = if (suffix.isNotEmpty()) packageName else fullName

                        val imageBytes = innerZipIn.readBytes()
                        iconsList.add(
                            IconItem(
                                packageName = cleanedPackageName,
                                suffix = suffix,
                                imageBytes = imageBytes,
                                matchedAppName = getInstalledAppName(cleanedPackageName)
                            )
                        )
                    }
                    innerEntry = innerZipIn.getNextEntry()
                }
            }
            entry = zipIn.getNextEntry()
        }
        zipIn.close()
        return LoadedThemeResult(
            metadata = metadata,
            icons = iconsList,
            customizeUiPrint = customizePrintLines
        )
    }

    private fun createInnerIconsZip(icons: List<IconItem>): ByteArray {
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)
        
        // 1. Write transform_config.xml
        val configXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <IconTransform>
                <PointsMapping>
                    <Point fromX="0" fromY="0" toX="-4" toY="-4"/>
                    <Point fromX="0" fromY="90" toX="-4" toY="94"/>
                    <Point fromX="90" fromY="90" toX="94" toY="94"/>
                    <Point fromX="90" fromY="0" toX="94" toY="-4"/>
                </PointsMapping>
                <Config name="ConfigIconMask" value="M 0 42.7 C 0 27.7 0 20.3 2.9 14.6 C 5.5 9.5 9.5 5.5 14.6 2.9 C 20.3 0 27.7 0 42.7 0 L 57.3 0 C 72.3 0 79.7 0 85.4 2.9 C 90.5 5.5 94.5 9.5 97.1 14.6 C 100 20.3 100 27.7 100 42.7 V 57.3 C 100 72.3 100 79.7 97.1 85.4 C 94.5 90.5 90.5 94.5 85.4 97.1 C 79.7 100 72.3 100 57.3 100 H 42.7 C 27.7 100 20.3 100 14.6 97.1 C 9.5 94.5 5.5 90.5 2.9 85.4 C 0 79.7 0 72.3 0 57.3 L 0 42.7 Z" />
            </IconTransform>
        """.trimIndent()
        zos.putNextEntry(ZipEntry("transform_config.xml"))
        zos.write(configXml.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        
        // 2. Write package icons inside res/drawable-xxhdpi/ folder
        for (icon in icons) {
            val entryName = "res/drawable-xxhdpi/${icon.packageName}${icon.suffix}.png"
            zos.putNextEntry(ZipEntry(entryName))
            zos.write(icon.imageBytes)
            zos.closeEntry()
        }
        
        zos.finish()
        zos.close()
        return bos.toByteArray()
    }

    private fun createDefaultWallpaper(): ByteArray {
        val bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#1C1E24")) // Dark Slate
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
        return bos.toByteArray()
    }

    private fun createDefaultPreview(): ByteArray {
        val bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#262930")) // Dark grey
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = 50f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Miuix Theme Preview", 540f, 1200f, paint)
        
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
        return bos.toByteArray()
    }

    /**
     * Build modified workspace back into MTZ file
     */
    fun exportMtz(
        metadata: ThemeMetadata,
        icons: List<IconItem>,
        outputFile: java.io.File,
        wallpaper: ByteArray? = null,
        previewLauncher: ByteArray? = null,
        previewLockscreen: ByteArray? = null
    ): java.io.File {
        val fileOut = java.io.FileOutputStream(outputFile)
        val parentZos = ZipOutputStream(fileOut)

        // 1. Write description.xml
        parentZos.putNextEntry(ZipEntry("description.xml"))
        parentZos.write(metadata.toXmlString().toByteArray(Charsets.UTF_8))
        parentZos.closeEntry()

        // 2. Write wallpaper/default_wallpaper.jpg
        parentZos.putNextEntry(ZipEntry("wallpaper/default_wallpaper.jpg"))
        parentZos.write(wallpaper ?: createDefaultWallpaper())
        parentZos.closeEntry()

        // 3. Write previews
        parentZos.putNextEntry(ZipEntry("preview/preview_launcher_0.jpg"))
        parentZos.write(previewLauncher ?: createDefaultPreview())
        parentZos.closeEntry()

        parentZos.putNextEntry(ZipEntry("preview/preview_lockscreen_0.jpg"))
        parentZos.write(previewLockscreen ?: createDefaultPreview())
        parentZos.closeEntry()

        // 4. Icons Inner ZIP
        val innerZipBytes = createInnerIconsZip(icons)
        parentZos.putNextEntry(ZipEntry("icons"))
        parentZos.write(innerZipBytes)
        parentZos.closeEntry()

        parentZos.finish()
        parentZos.close()
        return outputFile
    }

    /**
     * Build modified workspace back into Magisk module bundle file
     */
    fun exportMagiskZip(
        metadata: ThemeMetadata,
        icons: List<IconItem>,
        moduleId: String = "custom_icon_module",
        outputFile: java.io.File,
        customizePrintLines: String = ""
    ): java.io.File {
        val fileOut = java.io.FileOutputStream(outputFile)
        val parentZos = ZipOutputStream(fileOut)

        // 1. Write module.prop
        parentZos.putNextEntry(ZipEntry("module.prop"))
        parentZos.write(metadata.toPropString(moduleId).toByteArray(Charsets.UTF_8))
        parentZos.closeEntry()

        // 2. Write customize.sh
        val customizeContent = buildString {
            if (customizePrintLines.isNotEmpty()) {
                append(customizePrintLines)
                append("\n")
            } else {
                append("ui_print \"- 正在安装自定义图标主题...\"\n")
                append("ui_print \"- Installing custom icon theme...\"\n")
            }
            append("""
                
                SKIPUNZIP=1
                var_version="`getprop ro.build.version.release`"
                var_miui_version="`getprop ro.miui.ui.version.code`"
                
                
                if [ ${'$'}var_version -lt 10 ]; then 
                  abort "- 您的 Android 版本不符合要求，即将退出安装。"
                  abort "- Your Android version does not meet the requirements and the installation will be exited."
                fi
                if [ ${'$'}var_miui_version -lt 10 ]; then 
                  abort "- 您的 HyperOS/MIUI 版本不符合要求，即将退出安装。"
                  abort "- Your HyperOS/MIUI version does not meet the requirements and will exit the installation."
                fi
                
                if [ -L "/system/media" ] ;then
                  MEDIAPATH=system${'}'}(realpath /system/media)
                else
                  if [ -d "/system/media" ]; then 
                    MEDIAPATH=system/media
                  else
                    abort "- ROM似乎有问题，无法安装。"
                    abort "- There seems to be a problem with the ROM and it cannot be installed."
                  fi
                fi
                
                REPLACE="/${'$'}MEDIAPATH/theme/miui_mod_icons/dynamic"
                
                echo "- 安装中..."
                echo "- installing..."
                
                mkdir -p ${'$'}{MODPATH}/${'$'}{MEDIAPATH}/theme/default/
                unzip -oj "${'$'}ZIPFILE" icons -d ${'$'}MODPATH/${'$'}MEDIAPATH/theme/default/ >&2
                unzip -oj "${'$'}ZIPFILE" miui_mod_icons/* -d ${'$'}MODPATH/${'$'}MEDIAPATH/theme/miui_mod_icons >&2
                unzip -oj "${'$'}ZIPFILE" addons/* -d ${'$'}MODPATH/${'$'}MEDIAPATH/theme/default/ >&2
                unzip -oj "${'$'}ZIPFILE" module.prop -d ${'$'}MODPATH/ >&2
                unzip -oj "${'$'}ZIPFILE" post-fs-data.sh -d ${'$'}MODPATH/ >&2 
                echo -ne '\x50\x4b\x05\x06\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00' > ${'$'}MODPATH/${'$'}MEDIAPATH/theme/default/dynamicicons
                set_perm_recursive ${'$'}MODPATH 0 0 0755 0644
                
                rm -rf /data/system/package_cache/*
                echo "√ 安装成功，请重启设备"
                echo "√ Installation successful, please restart the device"
                echo "---------------------------------------------"
            """.trimIndent())
        }
        parentZos.putNextEntry(ZipEntry("customize.sh"))
        parentZos.write(customizeContent.toByteArray(Charsets.UTF_8))
        parentZos.closeEntry()

        // 3. Write post-fs-data.sh
        val postFsDataContent = """
            MODDIR="${'$'}{0%/*}"
            for i in /data/user/0/com.xiaomi.market/files/download_icon /data/user/0/com.xiaomi.market/cache/icons; do
              if [ -d ${'$'}i ]; then
                rm -rf ${'$'}i/*
                chattr +i ${'$'}i
              fi
            done
        """.trimIndent()
        parentZos.putNextEntry(ZipEntry("post-fs-data.sh"))
        parentZos.write(postFsDataContent.toByteArray(Charsets.UTF_8))
        parentZos.closeEntry()

        // 4. Write icons nested ZIP
        val innerZipBytes = createInnerIconsZip(icons)
        parentZos.putNextEntry(ZipEntry("icons"))
        parentZos.write(innerZipBytes)
        parentZos.closeEntry()

        parentZos.finish()
        parentZos.close()
        return outputFile
    }

    /**
     * Generate in-memory default procedural template when no file is chosen.
     */
    fun generateDefaultTemplate(type: ProjectType): Pair<ThemeMetadata, List<IconItem>> {
        val metadata = if (type == ProjectType.MTZ) {
            ThemeMetadata(
                title = "HyperOS Slate Theme",
                author = "Miuix Author",
                designer = "HyperDesign",
                version = "1.0.0",
                uiVersion = "4",
                description = "Procedural Slate HyperOS theme template with pristine curves"
            )
        } else {
            ThemeMetadata(
                title = "Magisk Deep Blue Pack",
                author = "SysMag Developer",
                designer = "Android Custom",
                version = "1.0",
                uiVersion = "4",
                description = "Magisk system media modules loaded procedurally"
            )
        }

        val templatePackages = listOf(
            Triple("com.android.settings", "S", 0xFF2A2E3D.toInt()),
            Triple("com.android.camera", "C", 0xFFE05A3E.toInt()),
            Triple("com.android.phone", "P", 0xFF2B88E6.toInt()),
            Triple("com.android.contacts", "D", 0xFF3BA854.toInt()),
            Triple("com.android.providers.downloads", "D", 0xFFDF9E1F.toInt()),
            Triple("com.android.calendar", "C", 0xFF6549E0.toInt()),
            Triple("com.android.browser", "B", 0xFF3EAFE0.toInt()),
            Triple("com.android.gallery3d", "G", 0xFFD83BA2.toInt()),
            Triple("com.android.deskclock", "A", 0xFF2979FF.toInt()),
            Triple("com.miui.player", "M", 0xFFE91E63.toInt())
        )

        val icons = mutableListOf<IconItem>()
        for (item in templatePackages) {
            val pName = item.first
            // Generate primary icon
            val primaryBytes = createProceduralIcon(item.second, item.third)
            icons.add(IconItem(pName, "", primaryBytes, getInstalledAppName(pName)))

            // Generate alternatives for some icons to showcase suffix switching!
            if (pName == "com.android.settings" || pName == "com.android.camera") {
                val altBytes1 = createProceduralIcon(item.second, 0xFF424242.toInt()) // dark theme version
                icons.add(IconItem(pName, "_1", altBytes1, getInstalledAppName(pName)))

                val altBytes2 = createProceduralIcon(item.second, 0xFF7E57C2.toInt()) // purple theme version
                icons.add(IconItem(pName, "_2", altBytes2, getInstalledAppName(pName)))
            }
        }

        return Pair(metadata, icons)
    }

    /**
     * Helper to render highly visual tiles procedurally in-memory
     */
    private fun createProceduralIcon(char: String, backgroundColor: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw shadow or modern base circle
        paint.color = Color.parseColor("#15000000")
        canvas.drawRoundRect(10f, 10f, 186f, 186f, 48f, 48f, paint)

        // Draw the rounded main canvas tile
        paint.color = backgroundColor
        canvas.drawRoundRect(6f, 6f, 182f, 182f, 46f, 46f, paint)

        // Elegant geometric inner shape (accent circle in top-right)
        paint.color = Color.parseColor("#33FFFFFF")
        canvas.drawCircle(144f, 48f, 24f, paint)

        // Draw text symbol inside with nice anti-aliasing
        paint.color = Color.WHITE
        paint.textSize = 96f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        val fontMetrics = paint.fontMetrics
        val y = 96f - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(char, 96f, y, paint)

        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
        return bos.toByteArray()
    }
}
