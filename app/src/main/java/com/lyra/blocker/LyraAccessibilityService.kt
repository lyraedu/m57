// SPDX-License-Identifier: (GPL-3.0-only AND AGPL-3.0-only)
// Curbox's event-driven app/keyword blocking, reduced to the Lyra redirect policy.
package com.lyra.blocker

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.util.ArrayDeque
import java.util.concurrent.Executors
import com.lyra.blocker.family.BrowserDomains
import com.lyra.blocker.family.FamilyStore
import com.lyra.blocker.family.FamilyRepository
import com.lyra.blocker.family.FamilyBlockedOverlay
import com.lyra.blocker.family.HostRules

class LyraAccessibilityService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        @Volatile var connected = false
            private set
    }
    private lateinit var store: ProtectionStore
    private lateinit var overlay: RedirectOverlay
    private lateinit var shortsOverlay: ShortsReminderOverlay
    private lateinit var familyOverlay: FamilyBlockedOverlay
    private lateinit var familyStore: FamilyStore
    private lateinit var familyRepository: FamilyRepository
    private lateinit var focusStore: FocusStore
    private lateinit var focusOverlay: FocusBlockedOverlay
    private lateinit var limitStore: AppLimitStore
    private val usageWorker = Executors.newSingleThreadExecutor()
    @Volatile private var usageSnapshot = UsageSnapshot(false, emptyMap(), 0)
    private var usageReading = false
    private var serviceAlive = false
    private var lastUsageRead = -60_000L
    private var lastWidgetMinute = -1L
    private val periodic = object : Runnable {
        override fun run() {
            if (!::store.isInitialized) return
            if (getSystemService(PowerManager::class.java).isInteractive) {
                refreshUsage()
                focusStore.session?.let { if (focusStore.state(it).finished()) focusStore.finish() }
                checkCurrentWindow()
                val minute = System.currentTimeMillis() / 60_000
                if (minute != lastWidgetMinute) { lastWidgetMinute = minute; FocusWidget.update(this@LyraAccessibilityService) }
            }
            handler.postDelayed(this, 1000)
        }
    }
    private var shortsVisit = ShortsVisit()
    private var lastVideoKey:String?=null
    private data class Target(val packageName: String, val label: String, val reminderOnly: Boolean = false, val appInfo: Boolean = false, val familyCategory: String? = null, val videoKey:String?=null, val sourceHint:String="other", val focusReason: String? = null)
    private val handler = Handler(Looper.getMainLooper())
    private val recheck = Runnable { checkCurrentWindow() }
    private val pauseExpiry = Runnable { checkCurrentWindow(); schedulePauseExpiry() }
    private val appInfoExpiry = Runnable { checkCurrentWindow() }
    private val appInfoRetry = Runnable { checkCurrentWindow() }
    private var lastAppInfoRedirect = -1_000L
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handler.removeCallbacks(recheck)
            handler.removeCallbacks(appInfoRetry)
            overlay.hide()
            shortsOverlay.hide()
            familyOverlay.hide()
            if (::focusOverlay.isInitialized) focusOverlay.hide()
            shortsVisit.shouldRemind(false, SystemClock.elapsedRealtime())
        }
    }
    private var receiverRegistered = false
    override fun onServiceConnected() {
        serviceAlive = true
        connected = true
        handler.removeCallbacksAndMessages(null)
        if (::overlay.isInitialized) overlay.hide()
        if (::shortsOverlay.isInitialized) shortsOverlay.hide()
        if (::familyOverlay.isInitialized) familyOverlay.hide()
        if (::focusOverlay.isInitialized) focusOverlay.hide()
        if (::focusStore.isInitialized) focusStore.prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (::limitStore.isInitialized) limitStore.prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (::familyStore.isInitialized) familyStore.prefs.unregisterOnSharedPreferenceChangeListener(this)
        shortsVisit = ShortsVisit()
        if (::store.isInitialized) store.prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (receiverRegistered) unregisterReceiver(screenOff)
        store = ProtectionStore(this)
        focusStore = FocusStore(this)
        limitStore = AppLimitStore(this)
        focusOverlay = FocusBlockedOverlay(this)
        familyStore = FamilyStore(this)
        familyRepository = FamilyRepository.get(this)
        familyOverlay = FamilyBlockedOverlay(this)
        overlay = RedirectOverlay(this, store) { expected ->
            currentTarget()?.let { !it.reminderOnly && !it.appInfo && it.familyCategory == null && it.focusReason == null && it.packageName == expected && it.sourceHint == overlay.sourceHint } == true
        }
        shortsOverlay = ShortsReminderOverlay(this) { currentTarget()?.reminderOnly == true }
        store.prefs.registerOnSharedPreferenceChangeListener(this)
        familyStore.prefs.registerOnSharedPreferenceChangeListener(this)
        focusStore.prefs.registerOnSharedPreferenceChangeListener(this)
        limitStore.prefs.registerOnSharedPreferenceChangeListener(this)
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
        schedulePauseExpiry()
        scheduleAppInfoExpiry()
        handler.post(recheck)
        handler.post(periodic)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::overlay.isInitialized) return
        // Window events from every app let us remove a shield as soon as the user
        // leaves the blocked app. Trees are read only for the supported packages.
        if (event.packageName?.toString() == packageName && event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return
        // Coalesce bursts without postponing the check indefinitely on busy screens.
        if (!handler.hasCallbacks(recheck)) handler.postDelayed(recheck, 80)
    }

    private fun checkCurrentWindow() {
        if (!::overlay.isInitialized) return
        val target = currentTarget()
        if(target?.videoKey!=null&&target.videoKey!=lastVideoKey){shortsVisit=ShortsVisit();lastVideoKey=target.videoKey}
        handler.removeCallbacks(appInfoRetry)
        val remind = shortsVisit.shouldRemind(target?.reminderOnly == true, SystemClock.elapsedRealtime())
        if (target == null) { overlay.hide(); shortsOverlay.hide(); familyOverlay.hide(); focusOverlay.hide(); return }
        if (target.focusReason == null) focusOverlay.hide()
        if (target.focusReason != null) {
            overlay.hide(); shortsOverlay.hide(); familyOverlay.hide()
            focusOverlay.show(target.packageName, target.focusReason)
            return
        }
        if (target.familyCategory == null) familyOverlay.hide()
        if (target.familyCategory != null) {
            overlay.hide(); shortsOverlay.hide()
            familyOverlay.show(target.label, target.familyCategory)
            return
        }
        if (target.appInfo) {
            overlay.hide()
            shortsOverlay.hide()
            val now = SystemClock.elapsedRealtime()
            if (now - lastAppInfoRedirect >= 1_000) {
                lastAppInfoRedirect = now
                performGlobalAction(GLOBAL_ACTION_BACK)
                val opened = runCatching {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(MainActivity.SETTINGS_GUARD_EXTRA, true)
                    })
                    true
                }.getOrDefault(false)
                if (!opened) performGlobalAction(GLOBAL_ACTION_HOME)
            }
            // Recheck the actual foreground before every retry, never redirect on a stale event.
            handler.postDelayed(appInfoRetry, 1_000)
        } else if (target.reminderOnly) {
            overlay.hide()
            if ((store.videoMode=="block"||remind) && !shortsOverlay.show(target.label,store.videoMode=="block")) shortsVisit.retry()
        } else {
            shortsOverlay.hide()
            if (overlay.visible && (overlay.blockedPackage != target.packageName || overlay.sourceHint != target.sourceHint)) overlay.hide()
            overlay.show(target.packageName, target.label, target.sourceHint)
        }
    }

    private fun currentTarget(): Target? {
        if (!store.consented || (store.familyManaged && !store.hasPassword) || store.paused ||
            !getSystemService(PowerManager::class.java).isInteractive ||
            getSystemService(KeyguardManager::class.java).isKeyguardLocked) return null
        // Select an application window so our non-focusable shield and keyboards
        // cannot become the inspected root. API 34 no longer needs node recycling.
        val root = windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && (it.isActive || it.isFocused)
        }?.root ?: rootInActiveWindow ?: return null
        val activePackage = root.packageName?.toString() ?: return null
        if (activePackage == packageName || activePackage == "com.android.systemui") return null
        if (activePackage == AppInfoPolicy.settingsPackage) {
            if (!store.familyManaged || !store.settingsGuard) return null
            if (store.appInfoAccessRemaining > 0) return null
            val labels = collectLabels(root, false, true)
            val appInfo = AppInfoPolicy.isOwnAppInfo(activePackage, labels, getString(R.string.app_name), packageName)
            val accessibility = AccessibilitySettingsPolicy.matches(activePackage, labels.map {
                AccessibilitySettingsPolicy.Node(it.id, it.text, it.checkable)
            }, getString(R.string.app_name), getString(R.string.accessibility_description))
            return if (appInfo || accessibility) Target(activePackage, "M57 settings", appInfo = true) else null
        }
        val focusReason = focusStore.blockingReason()
        if ((activePackage in focusStore.blockedApps || activePackage in limitStore.limits) &&
            activePackage in AppCatalog.protectedPackages(this)) return null
        if (focusReason != null && activePackage in focusStore.blockedApps)
            return Target(activePackage, "Focus app", focusReason = focusReason)
        val allowance = limitStore.limits[activePackage]
        val sample = usageSnapshot
        if (allowance != null && focusStore.usageEnabled && UsageAccess.granted(this) && sample.available &&
            System.currentTimeMillis() - sample.sampledAt in 0..35_000 &&
            java.time.Instant.ofEpochMilli(sample.sampledAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now() &&
            (sample.totals[activePackage] ?: 0) >= allowance * 60_000L)
            return Target(activePackage, "Daily limit", focusReason = "Your $allowance-minute daily allowance is complete. It resets at local midnight.")
        val mediaIds=MediaPolicy.inspectedNames(activePackage).flatMap { id -> root.findAccessibilityNodeInfosByViewId("$activePackage:id/$id") }
            .filter { node ->
                val bounds=Rect();node.getBoundsInScreen(bounds)
                (node.isVisibleToUser&&Rect.intersects(bounds,Rect(0,0,resources.displayMetrics.widthPixels,resources.displayMetrics.heightPixels))) ||
                    (activePackage in MediaPolicy.TIKTOK&&node.viewIdResourceName=="$activePackage:id/long_press_layout")
            }.mapNotNull {it.viewIdResourceName}.toSet()
        val facebookViewer=activePackage=="com.facebook.katana"&&root.findAccessibilityNodeInfosByText("Reels tab details").any {it.isVisibleToUser&&it.contentDescription?.toString()=="Reels tab details"}
        val nativeVideo=MediaPolicy.nativePlatform(activePackage,mediaIds,facebookViewer)
        if(nativeVideo!=null&&videoEnabled(nativeVideo))return Target(activePackage,MediaPolicy.LABELS[nativeVideo]?:"Short videos",reminderOnly=true,videoKey=nativeVideo)
        val browserLabels = if (activePackage in BrowserDomains.BROWSERS) collectLabels(root, true).filterNot { it.editing } else emptyList()
        if (store.familyManaged && store.repoGuard && store.appInfoAccessRemaining == 0L) for (item in browserLabels) {
            if (BrowserDomains.host(activePackage,item.id,item.text) != null && HandoffPolicy.repositoryAddress(item.text))
                return Target(activePackage,"M57 source repository",familyCategory="source")
        }
        if (familyStore.browserEnabled) for (item in browserLabels) {
            if (!BrowserDomains.trustedField(activePackage, item.id)) continue
            val address = com.lyra.blocker.family.BrowserUrlPolicy.parse(item.text) ?: continue
            val category = familyRepository.index.match(address.host())
            if (category != null) return Target(activePackage, address.host(), familyCategory = category)
            val allowed = familyStore.allow.any { address.host() == it || address.host().endsWith(".$it") }
            if (!allowed && familyStore.blockedUrls.any { com.lyra.blocker.family.BrowserUrlPolicy.matches(it, item.text) })
                return Target(activePackage, address.host(), familyCategory = "page")
        }
        if (focusReason != null) for (item in browserLabels) {
            if (!BrowserDomains.trustedField(activePackage, item.id)) continue
            val host = com.lyra.blocker.family.BrowserUrlPolicy.parse(item.text)?.host() ?: continue
            if (FocusPolicy.blocks(host, focusStore.blockedSites, focusStore.allowedSites, focusStore.studyBrowser) ||
                (focusStore.studyBrowser && (host.contains(':') || host.matches(Regex("[0-9.]+")))))
                return Target(activePackage, "Focus website", focusReason = if (focusStore.studyBrowser) "$focusReason · only your study websites are available" else focusReason)
        }
        val webVideo=browserLabels.firstNotNullOfOrNull {MediaPolicy.addressPlatform(it.text)}
        if(webVideo!=null&&videoEnabled(webVideo))return Target(activePackage,MediaPolicy.LABELS[webVideo]?:"Short videos",reminderOnly=true,videoKey=webVideo)
        if (!store.aiEnabled) return null
        if (store.googleAiMode && browserLabels.any { BrowserDomains.host(activePackage, it.id, it.text) != null && GoogleAiPolicy.address(it.text) })
            return Target(activePackage, "Google AI Mode")
        val metaService = MetaPolicy.SERVICES[activePackage]
        val metaAllowed = metaService!=null && store.metaEnabled && metaService in store.metaServices
        val label = BlockPolicy.apps[activePackage]?.takeIf {
            activePackage !in store.disabledApps && (metaService != "meta" || metaAllowed)
        } ?: activePackage.takeIf { it in store.extraPackages && it !in BlockPolicy.apps }?.let { "This AI app" }
        if (label != null) return Target(activePackage, label, sourceHint=HandoffPolicy.source(label))
        if (activePackage !in MetaPolicy.SERVICES && activePackage != BlockPolicy.google && activePackage !in BrowserDomains.BROWSERS) {
            return null
        }
        val labels = if (activePackage in BrowserDomains.BROWSERS) browserLabels else collectLabels(root, false)
        val embedded = if (metaAllowed && labels.any { MetaPolicy.titleMatches(activePackage,it.id,it.text) || MetaPolicy.titleMatches(activePackage,it.id,it.description) }) "Meta AI"
            else if(activePackage==BlockPolicy.google) {
                if (store.googleAiMode && BlockPolicy.googleAiMode(labels)) "Google AI Mode" else BlockPolicy.embeddedAi(activePackage,labels)
            } else null
        val ownHost = HostRules.address(store.destination)
        val website = if (activePackage in BrowserDomains.BROWSERS) labels.firstOrNull {
            val host=BrowserDomains.host(activePackage,it.id,it.text)
            host!=null && host!=ownHost &&
                (BlockPolicy.domains.any { domain -> domain !in store.disabledDomains && (host==domain || host.endsWith(".$domain")) } ||
                    store.extraAiDomains.any { domain -> host==domain || host.endsWith(".$domain") })
        } else null
        val reason = embedded ?: website?.let { "This AI website" }
        return reason?.let { Target(activePackage, it, sourceHint=HandoffPolicy.source(website?.let { address -> BrowserDomains.host(activePackage,address.id,address.text) } ?: it)) }
    }

    private fun videoEnabled(key:String):Boolean=when(key){
        "youtube"->store.youtubeEnabled
        "instagram"->store.instagramEnabled
        else->store.extraVideosEnabled&&key in store.extraVideoApps
    }

    private fun collectLabels(root: AccessibilityNodeInfo, browser: Boolean, settings: Boolean = false): List<BlockPolicy.Label> {
        val result = mutableListOf<BlockPolicy.Label>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        // Bound traversal. Only title/address-bar fields are read, never message bodies.
        val ids = if (settings) AppInfoPolicy.inspectedIds + AccessibilitySettingsPolicy.INSPECTED_IDS else if (browser) BrowserDomains.INSPECTED_IDS else setOf(
            "conversation_contact_name", "conversation_title", "toolbar_title", "action_bar_title", "assistant_title", "thread_title", "thread_header_title", "thread_name",
        )
        while (queue.isNotEmpty() && visited++ < 200) {
            val node = queue.removeFirst()
            val id = if (browser && node.uniqueId == "ADDRESSBAR_URL_BOX") "${root.packageName}:id/ADDRESSBAR_URL_BOX" else node.viewIdResourceName.orEmpty()
            if (node.isVisibleToUser && id.substringAfterLast('/') in ids) {
                result.add(BlockPolicy.Label(id, node.text?.toString().orEmpty(), node.contentDescription?.toString().orEmpty(), node.isCheckable, node.isSelected, node.isEditable && node.isFocused, node.isHeading))
            }
            for (index in 0 until node.childCount.coerceAtMost(80)) {
                if (queue.size >= 200) break
                node.getChild(index)?.let(queue::addLast)
            }
        }
        return result
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        handler.removeCallbacks(recheck)
        handler.post(recheck)
        // A pause expires even if the blocked app emits no new accessibility event.
        if (key == "pause_until" || key == "pause_boot" || key?.startsWith("protection_pause_") == true) schedulePauseExpiry()
        if (key == "app_info_until" || key == "app_info_boot") scheduleAppInfoExpiry()
        if (preferences === focusStore.prefs && key == "usage") {
            usageSnapshot = UsageSnapshot(false, emptyMap(), 0)
            lastUsageRead = -60_000L
        }
        if (preferences === limitStore.prefs) lastUsageRead = -60_000L
    }
    private fun refreshUsage() {
        if (usageReading || SystemClock.elapsedRealtime() - lastUsageRead < 15_000) return
        if (!focusStore.usageEnabled || limitStore.limits.isEmpty()) { usageSnapshot = UsageSnapshot(false, emptyMap(), 0); return }
        usageReading = true
        lastUsageRead = SystemClock.elapsedRealtime()
        usageWorker.execute {
            val sample = UsageAccess.today(this)
            handler.post { if (serviceAlive) { usageReading = false; usageSnapshot = sample; checkCurrentWindow() } }
        }
    }
    private fun schedulePauseExpiry() {
        handler.removeCallbacks(pauseExpiry)
        if (store.paused && !store.pauseIndefinite) handler.postDelayed(pauseExpiry, store.pauseRemaining.coerceAtMost(86_400_000) + 100)
    }
    private fun scheduleAppInfoExpiry() {
        handler.removeCallbacks(appInfoExpiry)
        if (store.appInfoAccessRemaining > 0) handler.postDelayed(appInfoExpiry, store.appInfoAccessRemaining + 100)
    }
    override fun onInterrupt() {
        if (::overlay.isInitialized) overlay.hide()
        if (::shortsOverlay.isInitialized) shortsOverlay.hide()
        if (::familyOverlay.isInitialized) familyOverlay.hide()
        if (::focusOverlay.isInitialized) focusOverlay.hide()
    }
    override fun onDestroy() {
        serviceAlive = false
        connected = false
        handler.removeCallbacksAndMessages(null)
        if (::overlay.isInitialized) overlay.hide()
        if (::shortsOverlay.isInitialized) shortsOverlay.hide()
        if (::familyOverlay.isInitialized) familyOverlay.hide()
        if (::focusOverlay.isInitialized) focusOverlay.hide()
        if (::focusStore.isInitialized) focusStore.prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (::limitStore.isInitialized) limitStore.prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (::familyStore.isInitialized) familyStore.prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (::store.isInitialized) store.prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (receiverRegistered) unregisterReceiver(screenOff)
        usageWorker.shutdown()
        super.onDestroy()
    }
}
