package com.example.rednotebook.ui.editor

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.load
import coil.request.CachePolicy
import com.example.rednotebook.data.network.ApiClient

object EntryRenderer {

    // Matches [""URL_OR_PATH"".ext]
    private val IMAGE_REGEX = Regex("""\[""\s*([^""]+)""\s*\.([a-zA-Z0-9]+)(?:\?\d+)?\s*\]""")

    // Desktop attachments path prefix to strip
    private val FILE_PATH_PREFIXES = listOf(
        "file:///",
        "file://"
    )

    fun render(context: Context, text: String, container: LinearLayout) {
        container.removeAllViews()
        container.orientation = LinearLayout.VERTICAL
        if (text.isBlank()) return

        val baseUrl = ApiClient.getSavedUrl(context).trimEnd('/')

        var lastEnd = 0
        var foundAny = false

        for (match in IMAGE_REGEX.findAll(text)) {
            foundAny = true
            val before = text.substring(lastEnd, match.range.first).trim()
            if (before.isNotEmpty()) container.addView(makeTextView(context, before))

            val rawUrl = match.groupValues[1].trim()
            val ext    = match.groupValues[2].substringBefore("?")

            val imageUrl = resolveUrl(rawUrl, ext, baseUrl)
            if (imageUrl != null) {
                container.addView(makeImageView(context, imageUrl))
            } else {
                // Can't resolve — show as text
                container.addView(makeTextView(context, match.value))
            }

            lastEnd = match.range.last + 1
        }

        val remaining = text.substring(lastEnd).trim()
        if (remaining.isNotEmpty()) container.addView(makeTextView(context, remaining))

        if (!foundAny || container.childCount == 0) {
            container.removeAllViews()
            container.addView(makeTextView(context, text))
        }
    }

    /**
     * Resolves a URL or file path to a loadable URL:
     * - http/https URLs: used as-is (strip duplicate ext if needed)
     * - file:/// paths: extract filename, build server URL via /attachments/file/
     */
    private fun resolveUrl(raw: String, ext: String, baseUrl: String): String? {
        if (baseUrl.isEmpty()) return null

        return when {
            raw.startsWith("http://") || raw.startsWith("https://") -> {
                // Already a full URL — strip duplicate extension if present
                if (raw.endsWith(".$ext", ignoreCase = true)) raw else "$raw.$ext"
            }
            raw.startsWith("file:///") || raw.startsWith("file://") -> {
                // Desktop local file — extract just the filename
                val path = raw.removePrefix("file:///").removePrefix("file://")
                val filename = path.substringAfterLast("/")
                // Remove duplicate extension from filename if present
                val cleanName = if (filename.endsWith(".$ext", ignoreCase = true))
                    filename else "$filename.$ext"
                "$baseUrl/attachments/file/$cleanName"
            }
            else -> null
        }
    }

    private fun makeTextView(context: Context, text: String): TextView {
        val tv = TextView(context)
        tv.text = text
        tv.textSize = 16f
        tv.setTextIsSelectable(true)
        tv.setLineSpacing(0f, 1.4f)
        tv.setPadding(0, 8, 0, 8)
        tv.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        return tv
    }

    private fun makeImageView(context: Context, url: String): ImageView {
        val iv = ImageView(context)
        iv.adjustViewBounds = true
        iv.scaleType = ImageView.ScaleType.FIT_START
        iv.setPadding(0, 8, 0, 8)
        iv.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iv.load(url) {
            crossfade(true)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
        }
        return iv
    }
}
