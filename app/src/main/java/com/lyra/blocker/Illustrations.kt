// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import android.widget.LinearLayout
import java.util.concurrent.Executors

/** Optional packaged artwork: decode off-thread, retain a bounded cache, never fetch from the network. */
object Illustrations {
    private val worker = Executors.newSingleThreadExecutor()
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val names = setOf("attention-garden", "personal-controls", "learning-detour", "one-thing", "take-a-breath",
        "short-video-choice", "daily-allowance", "routine-rhythm", "browsing-boundaries", "permission-window",
        "local-history", "privacy-drawer", "commitment-ribbon", "small-goal", "ready-for-your-day", "first-page")
    fun addTo(parent: LinearLayout, name: String, height: Int = 200): Boolean {
        require(name in names)
        val app = parent.context.applicationContext
        val path = "illustrations/$name.webp"
        if (runCatching { app.assets.list("illustrations")?.contains("$name.webp") }.getOrDefault(false) != true) return false
        parent.addView(SceneImage(parent.context, path), LinearLayout.LayoutParams(-1, LyraUi.dp(parent.context, height)).apply {
            topMargin = LyraUi.dp(parent.context, 6); bottomMargin = LyraUi.dp(parent.context, 12)
        })
        return true
    }
    private class SceneImage(context: Context, private val path: String) : ImageView(context) {
        private var generation = 0
        private var ambient: AmbientArtwork? = null
        init { scaleType = ScaleType.FIT_CENTER; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO; ambient = AmbientArtwork(this) }
        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            val ticket = ++generation
            cache.get(path)?.let { setImageBitmap(it); ambient?.refresh(); return }
            val app = context.applicationContext
            worker.execute {
                val bitmap = runCatching { app.assets.open(path).use { BitmapFactory.decodeStream(it) } }.getOrNull()
                if (bitmap != null) {
                    cache.put(path, bitmap)
                    post { if (isAttachedToWindow && generation == ticket) {
                        setImageBitmap(bitmap)
                        if (Motion.enabled(context)) { alpha = 0f; animate().alpha(1f).setDuration(240).start() }
                        ambient?.refresh()
                    } }
                }
            }
        }
        override fun onDetachedFromWindow() { generation++; animate().cancel(); super.onDetachedFromWindow() }
        override fun onWindowFocusChanged(hasWindowFocus: Boolean) { super.onWindowFocusChanged(hasWindowFocus); ambient?.refresh() }
        override fun onWindowVisibilityChanged(visibility: Int) { super.onWindowVisibilityChanged(visibility); ambient?.refresh() }
        override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) { super.onVisibilityChanged(changedView, visibility); ambient?.refresh() }
    }
}
