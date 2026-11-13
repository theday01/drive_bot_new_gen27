package com.indriver.bot.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.indriver.bot.utils.FilterDecision
import com.indriver.bot.utils.OrderFilter
import com.indriver.bot.utils.OrderParser
import com.indriver.bot.utils.PreferenceManager

class InDriverAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "InDriverBot"
        const val PACKAGE_INDRIVER = "sinet.startup.inDriver"

        private const val DEBOUNCE_COOLDOWN_MS = 800L

        // Configurable list of accept button resource IDs (can be adjusted after testing on real devices)
        val ACCEPT_BUTTON_VIEW_IDS = listOf(
            "sinet.startup.inDriver:id/accept_button",
            "sinet.startup.inDriver:id/btn_accept",
            "sinet.startup.inDriver:id/order_accept_btn",
            "sinet.startup.inDriver:id/acceptOrderButton"
        )

        // Configurable list of exact accept button texts (exact match, case-insensitive, trimmed)
        val ACCEPT_BUTTON_TEXTS = listOf(
            "قبول",
            "Accept",
            "Принять",
            "Aceptar",
            "ACCEPT",
            "ACEPTAR"
        )

        @Volatile
        private var instance: InDriverAccessibilityService? = null

        fun getInstance(): InDriverAccessibilityService? = instance
    }

    private lateinit var prefs: PreferenceManager

    @Volatile
    private var isActionInFlight = false

    @Volatile
    private var lastActionTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefs = PreferenceManager(this)

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
            packageNames = arrayOf(PACKAGE_INDRIVER)
        }
        serviceInfo = info

        Log.d(TAG, "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 1. Single source of truth: if bot is turned off, do nothing
        if (!PreferenceManager.isBotEnabled) {
            return
        }

        // Only handle events from inDriver
        val packageName = event.packageName?.toString() ?: return
        if (packageName != PACKAGE_INDRIVER) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                processActiveWindow()
            }
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                processActiveWindow()
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Inspects the active window for an order screen containing an accept button,
     * parses price and distance, checks filters, and clicks accept if conditions are met.
     */
    private fun processActiveWindow() {
        val root = rootInActiveWindow ?: return

        try {
            // Find accept button (detects an order screen only when an accept button is present)
            val acceptButton = findAcceptButton(root) ?: return

            try {
                // Debounce / Cooldown check
                val now = SystemClock.uptimeMillis()
                if (now - lastActionTime < DEBOUNCE_COOLDOWN_MS) {
                    return
                }

                // In-flight guard
                if (isActionInFlight) {
                    return
                }

                // Extract all text nodes to parse price and distance
                val allTexts = extractAllTexts(root)
                val parsed = OrderParser.parse(allTexts)

                val decision = OrderFilter.evaluate(
                    order = parsed,
                    minPrice = prefs.getMinPrice(),
                    maxDistance = prefs.getMaxDistance(),
                    isAutoBidEnabled = prefs.isAutoBidEnabled()
                )

                when (decision) {
                    FilterDecision.SKIP_AUTO_BID_DISABLED -> {
                        Log.d(TAG, "Auto-bid disabled in settings, skipping order")
                        return
                    }
                    FilterDecision.SKIP_UNPARSEABLE -> {
                        Log.d(TAG, "Order detected but could not parse price (${parsed.price}) or distance (${parsed.distanceKm}). Skipping.")
                        return
                    }
                    FilterDecision.REJECT_PRICE_TOO_LOW -> {
                        Log.d(TAG, "Order rejected by min price filter: price=${parsed.price} < min=${prefs.getMinPrice()}")
                        prefs.incrementMissed()
                        lastActionTime = now
                        return
                    }
                    FilterDecision.REJECT_DISTANCE_TOO_HIGH -> {
                        Log.d(TAG, "Order rejected by max distance filter: distance=${parsed.distanceKm} km > max=${prefs.getMaxDistance()} km")
                        prefs.incrementMissed()
                        lastActionTime = now
                        return
                    }
                    FilterDecision.ACCEPT -> {
                        // All criteria met -> proceed to click accept button
                    }
                }

                val price = parsed.price!!
                val distance = parsed.distanceKm!!

                // Find clickable target: button itself or nearest clickable parent
                val clickableTarget = findClickableTarget(acceptButton)
                if (clickableTarget == null) {
                    Log.w(TAG, "Accept button or parent is not clickable")
                    return
                }

                try {
                    isActionInFlight = true
                    val success = clickableTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (success) {
                        lastActionTime = SystemClock.uptimeMillis()
                        prefs.incrementAccepted()
                        prefs.incrementTodayAccepted()
                        prefs.addEarnings(price)

                        if (prefs.areNotificationsEnabled()) {
                            showToast("✅ Accepted: $price")
                        }
                        Log.d(TAG, "Order accepted successfully: price=$price, distance=$distance km")
                    }
                } finally {
                    isActionInFlight = false
                    if (clickableTarget != acceptButton) {
                        clickableTarget.recycle()
                    }
                }
            } finally {
                acceptButton.recycle()
            }
        } finally {
            root.recycle()
        }
    }

    /**
     * Detects accept button: prefers matching by viewIdResourceName,
     * falls back to exact text match (not contains).
     */
    private fun findAcceptButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 1. Try matching by view ID
        for (viewId in ACCEPT_BUTTON_VIEW_IDS) {
            val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
            if (!nodes.isNullOrEmpty()) {
                val match = nodes[0]
                // Recycle remaining nodes to avoid leaks
                for (i in 1 until nodes.size) {
                    nodes[i].recycle()
                }
                return match
            }
        }

        // 2. Fall back to exact text match
        for (exactText in ACCEPT_BUTTON_TEXTS) {
            val candidates = root.findAccessibilityNodeInfosByText(exactText)
            if (!candidates.isNullOrEmpty()) {
                var found: AccessibilityNodeInfo? = null
                for (node in candidates) {
                    val text = node.text?.toString()?.trim()
                    if (found == null && text.equals(exactText, ignoreCase = true)) {
                        found = node
                    } else {
                        node.recycle()
                    }
                }
                if (found != null) {
                    return found
                }
            }
        }

        return null
    }

    /**
     * Walks up the hierarchy to find the nearest clickable node.
     */
    private fun findClickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isClickable) {
            return node
        }
        var current = node.parent
        while (current != null) {
            if (current.isClickable) {
                return current
            }
            val parent = current.parent
            current.recycle()
            current = parent
        }
        return null
    }

    /**
     * Extracts all visible text from the hierarchy to pass to OrderParser.
     */
    private fun extractAllTexts(root: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                texts.add(text)
            }
            val desc = node.contentDescription?.toString()?.trim()
            if (!desc.isNullOrBlank()) {
                texts.add(desc)
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }

            if (node != root) {
                node.recycle()
            }
        }
        return texts
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
