package uz.sozqalqoni

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Boshqa ilovalarda (brauzer, YouTube, Google va h.k.) qidiruv maydoniga yozilgan
 * matnni kuzatadi. Bloklangan so'z topilsa, maydonni tozalaydi va ogohlantirish chiqaradi.
 */
class BlockerService : AccessibilityService() {

    companion object {
        private val BROWSERS = setOf(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
            "org.mozilla.firefox", "org.mozilla.fenix", "org.mozilla.firefox_beta",
            "com.sec.android.app.sbrowser", "com.opera.browser", "com.opera.mini.native",
            "com.microsoft.emmx", "com.brave.browser", "com.duckduckgo.mobile.android",
            "com.yandex.browser", "com.kiwibrowser.browser", "com.vivaldi.browser",
            "com.UCMobile.intl", "mark.via.gp",
            "com.google.android.googlequicksearchbox", "com.google.android.youtube"
        )
        private val URL_IDS = listOf("url_bar", "mozac_browser_toolbar_url_view", "url_bar_title")
        private val HINT_WORDS = listOf("search", "qidir", "izla", "поиск", "найти", "query", "omnibox", "url")

        private val BG = Color.parseColor("#0F1424")
        private val CARD = Color.parseColor("#151B2E")
        private val TEXT = Color.parseColor("#E8ECF6")
        private val MUTED = Color.parseColor("#B9C2D9")
        private val BLUE = Color.parseColor("#2C6BFF")
        private val RED = Color.parseColor("#FF6B6B")

        private const val PAUSE_SECONDS = 3
    }

    private var words: List<String> = emptyList()
    private var strict = false
    private var overlay: View? = null
    private var lastShown = 0L
    private var lastUrlCheck = 0L
    private val handler = Handler(Looper.getMainLooper())

    // Kuchli havola saqlanishi shart (SharedPreferences uni zaif saqlaydi).
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "words" || key == "strict") reload()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        reload()
        getSharedPreferences(Prefs.FILE, MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onDestroy() {
        try {
            getSharedPreferences(Prefs.FILE, MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefListener)
        } catch (e: Exception) {
        }
        dismiss()
        super.onDestroy()
    }

    override fun onInterrupt() {}

    private fun reload() {
        words = Prefs.words(this)
        strict = Prefs.strict(this)
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        e ?: return
        if (words.isEmpty() || overlay != null) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui") return

        when (e.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val src = e.source
                if (src != null) {
                    if (src.isPassword || !src.isEditable) return
                    if (!isSearchLike(src, pkg)) return
                } else if (pkg !in BROWSERS) {
                    return
                }
                val fromEvent = e.text?.joinToString(" ")
                val text = if (fromEvent.isNullOrBlank()) (src?.text?.toString() ?: "") else fromEvent
                val hit = Matcher.find(text, words, strict) ?: return
                clearField(src)
                show(hit)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (pkg in BROWSERS) checkUrlBar(pkg)
            }
        }
    }

    private fun isSearchLike(n: AccessibilityNodeInfo, pkg: String): Boolean {
        if (pkg in BROWSERS) return true
        val id = (n.viewIdResourceName ?: "").lowercase()
        val hint = (n.hintText?.toString() ?: "").lowercase()
        val desc = (n.contentDescription?.toString() ?: "").lowercase()
        return HINT_WORDS.any { id.contains(it) || hint.contains(it) || desc.contains(it) }
    }

    private fun checkUrlBar(pkg: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastUrlCheck < 700) return
        lastUrlCheck = now
        val root = rootInActiveWindow ?: return
        for (id in URL_IDS) {
            val nodes = root.findAccessibilityNodeInfosByViewId("$pkg:id/$id") ?: continue
            for (n in nodes) {
                val t = n.text?.toString() ?: continue
                val hit = Matcher.find(t, words, strict)
                if (hit != null) {
                    show(hit)
                    return
                }
            }
        }
    }

    private fun clearField(node: AccessibilityNodeInfo?) {
        try {
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            node?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } catch (e: Exception) {
        }
    }

    /* ---------------- Ogohlantirish oynasi ---------------- */

    private fun show(word: String) {
        val now = SystemClock.uptimeMillis()
        if (overlay != null || now - lastShown < 800) return
        lastShown = now
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val days = Prefs.cleanDays(this)
            val v = buildOverlay(word, days)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE
            )
            wm.addView(v, params)
            overlay = v
            Prefs.recordBlock(this)
        } catch (e: Exception) {
            overlay = null
        }
    }

    private fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        val v = overlay ?: return
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (e: Exception) {
        }
        overlay = null
    }

    /** Tugma bir necha soniyadan keyin bosiladigan bo'ladi: shoshilmasdan o'ylab ko'rish uchun. */
    private fun countdown(b: Button, label: String, secs: Int) {
        if (secs <= 0) {
            b.isEnabled = true
            b.alpha = 1f
            b.text = label
            return
        }
        b.isEnabled = false
        b.alpha = 0.5f
        b.text = "$label ($secs)"
        handler.postDelayed({
            if (overlay != null) countdown(b, label, secs - 1)
        }, 1000)
    }

    private fun buildOverlay(word: String, days: Int): View {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        fun round(c: Int, r: Int) = GradientDrawable().apply {
            setColor(c)
            cornerRadius = dp(r).toFloat()
        }
        fun label(t: String, size: Float, color: Int, bold: Boolean = false, top: Int = 0) =
            TextView(this).apply {
                text = t
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                if (bold) setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(top) }
            }
        fun button(t: String, color: Int, onClick: () -> Unit) = Button(this).apply {
            text = t
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 15f
            background = round(color, 12)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            setOnClickListener { onClick() }
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = round(CARD, 20)
            setPadding(dp(24), dp(32), dp(24), dp(24))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        card.addView(label("⚠️", 48f, TEXT))
        card.addView(label("Ogohlantirish!", 26f, RED, true, 8))
        card.addView(label("Siz o'zingiz bloklagan so'zni qidirdingiz:", 15f, MUTED, false, 10))
        card.addView(label(word, 18f, Color.parseColor("#FF9A9A"), true, 12).apply {
            background = round(Color.parseColor("#3A1620"), 20)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        })
        val tips = listOf(
            "To'xtang va chuqur nafas oling. Bu istak bir necha daqiqada o'tib ketadi.",
            "O'zingizga bergan va'dani eslang. Siz bundan kuchliroqsiz.",
            "Hozir boshqa ishga o'ting: bir stakan suv iching yoki sayr qiling.",
            "Telefonni qo'ying va 10 ta cho'kka tushing. Keyin o'zingizni yaxshi his qilasiz."
        )
        card.addView(label(tips.random(), 15f, Color.parseColor("#D7DEEF"), false, 14))
        card.addView(
            label(
                if (days > 0) "🔥 $days kun toza yurgan edingiz. Yangi boshlash uchun hozir ajoyib payt!"
                else "Har bir to'xtatilgan urinish sizni g'alabaga yaqinlashtiradi.",
                13f, Color.parseColor("#8FB0FF"), false, 12
            )
        )

        val backLabel = "Orqaga qaytish"
        val homeLabel = "Bosh ekranga chiqish"
        val backBtn = button(backLabel, BLUE) {
            dismiss()
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        val homeBtn = button(homeLabel, Color.parseColor("#2A3150")) {
            dismiss()
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
        card.addView(backBtn)
        card.addView(homeBtn)
        countdown(backBtn, backLabel, PAUSE_SECONDS)
        countdown(homeBtn, homeLabel, PAUSE_SECONDS)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(BG)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            isClickable = true
            addView(card)
        }
    }
}
