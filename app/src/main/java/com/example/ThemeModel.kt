package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

enum class ProjectType {
    MTZ,
    MAGISK_ZIP
}

data class ThemeMetadata(
    var id: String = "custom_icons",
    var title: String = "",
    var author: String = "",
    var designer: String = "",
    var version: String = "",
    var uiVersion: String = "4",
    var description: String = ""
) {
    fun toXmlString(): String {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <theme>
                <title>$title</title>
                <author>$author</author>
                <designer>$designer</designer>
                <version>$version</version>
                <uiVersion>$uiVersion</uiVersion>
                <description>$description</description>
            </theme>
        """.trimIndent()
    }

    fun toPropString(idValue: String): String {
        return """
            id=${idValue.ifEmpty { id }}
            name=$title
            version=$version
            versionCode=1
            author=$author
            description=$description
        """.trimIndent()
    }
}

data class LoadedThemeResult(
    val metadata: ThemeMetadata,
    val icons: List<IconItem>,
    val wallpaper: ByteArray? = null,
    val previewLauncher: ByteArray? = null,
    val previewLockscreen: ByteArray? = null,
    val customizeUiPrint: String? = null
)

data class IconItem(
    val packageName: String,
    val suffix: String = "", // e.g., "_1", "_2"
    var imageBytes: ByteArray? = null,
    val matchedAppName: String? = null,
    var imageBitmap: ImageBitmap? = null
) {
    init {
        if (imageBitmap == null && imageBytes != null) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes!!.size)
                imageBitmap = bitmap?.asImageBitmap()
            } catch (e: Exception) {
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IconItem
        if (packageName != other.packageName) return false
        if (suffix != other.suffix) return false
        return imageBytes.contentEquals(other.imageBytes)
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + suffix.hashCode()
        result = 31 * result + imageBytes.contentHashCode()
        return result
    }
}
