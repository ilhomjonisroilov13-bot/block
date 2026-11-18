package uz.sozqalqoni

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import java.util.Calendar

class MainActivity : Activity() {

    private val BG = Color.parseColor("#0B1020")
    private val CARD = Color.parseColor("#151B33")
    private val FIELD = Color.parseColor("#1D2545")
    private val TEXT = Color.parseColor("#E8ECF6")
    private val MUTED = Color.parseColor("#97A3C7")
    private val BLUE = Color.parseColor("#3B7BFF")
    private val LINK = Color.parseColor("#8FB0FF")
    private val RED = Color.parseColor("#FF7A7A")
    private val GREEN = Color.parseColor("#5BE49B")

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private val PASS = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    private val EMAIL = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    private val PLAIN = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

    private val QUOTES = listOf(
        "Har bir \"yo'q\" deyilgan lahza sizni kuchliroq qiladi.",
        "Istak to'lqin kabi: ko'tariladi va o'tib ketadi. Uni kuting.",
        "Bugun o'zingizga bergan va'daga sodiq qoling.",
        "Kichik g'alabalar katta o'zgarishga aylanadi.",
        "Vaqtingizni foydali ishga sarflang: kitob, sport, yangi ko'nikma.",
        "Siz o'zingizni nazorat qilyapsiz. Bu allaqachon g'alaba."
    )

    private val SUGGESTIONS = listOf("sex", "seks", "porn", "porno", "xxx")

    private lateinit var root: FrameLayout
    private lateinit var body: FrameLayout
    private val navItems = mutableListOf<TextView>()

    private var loggedIn = false
    private var leftAt = 0L
    private var tab = 0
    private var words = mutableListOf<String>()
    private var pendingMsg: String? = null
    private var pendingOk = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = Color.parseColor("#0B1020")
        root = FrameLayout(this).apply { setBackgroundColor(BG) }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        if (!Prefs.hasUser(this)) {
            showRegister()
            return
        }
        // 3 daqiqadan ko'p chiqib ketilgan bo'lsa, parol qayta so'raladi.
        if (loggedIn && leftAt != 0L && SystemClock.elapsedRealtime() - leftAt > 180_000L) {
            loggedIn = false
        }
        leftAt = 0L
        if (loggedIn) showMain() else showLogin()
    }

    override fun onStop() {
        super.onStop()
        leftAt = SystemClock.elapsedRealtime()
    }

    /* ---------------- Yordamchi funksiyalar ---------------- */

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun lp(top: Int = 0) =
        LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(top) }

    private fun round(c: Int, r: Int) = GradientDrawable().apply {
        setColor(c)
        cornerRadius = dp(r).toFloat()
    }

    private fun oval(c: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(c)
    }

    private fun tv(
        t: String, size: Float = 14f, color: Int = TEXT, bold: Boolean = false,
        top: Int = 0, center: Boolean = false
    ) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        if (center) gravity = Gravity.CENTER
        layoutParams = lp(top)
    }

    private fun edit(h: String, kind: Int, top: Int = 10) = EditText(this).apply {
        hint = h
        setHintTextColor(Color.parseColor("#6B7699"))
        setTextColor(TEXT)
        textSize = 15f
        background = round(FIELD, 12)
        setPadding(dp(14), dp(13), dp(14), dp(13))
        inputType = kind
        isSingleLine = true
        layoutParams = lp(top)
    }

    private fun btn(t: String, color: Int = BLUE, top: Int = 14, onClick: () -> Unit) =
        Button(this).apply {
            text = t
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 15f
            background = round(color, 14)
            layoutParams = lp(top)
            setOnClickListener { onClick() }
        }

    private fun card(top: Int = 14, color: Int = CARD, pad: Int = 16) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = round(color, 18)
        setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
        layoutParams = lp(top)
    }

    private fun page(build: LinearLayout.() -> Unit): ScrollView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(22))
        }
        col.build()
        return ScrollView(this).apply {
            isFillViewport = true
            addView(col)
        }
    }

    private fun LinearLayout.brand(sub: String) {
        addView(tv("🛡️", 56f, TEXT, top = 18, center = true))
        addView(tv("So'z Qalqoni", 28f, TEXT, true, 4, true))
        addView(tv(sub, 14f, MUTED, top = 6, center = true))
    }

    private fun LinearLayout.passwordToggle(vararg fields: EditText) {
        val t = tv("👁 Parolni ko'rsatish", 13f, LINK, top = 12)
        var shown = false
        t.setOnClickListener {
            shown = !shown
            fields.forEach {
                it.transformationMethod =
                    if (shown) HideReturnsTransformationMethod.getInstance()
                    else PasswordTransformationMethod.getInstance()
                it.setSelection(it.text.length)
            }
            t.text = if (shown) "🙈 Parolni yashirish" else "👁 Parolni ko'rsatish"
        }
        addView(t)
    }

    private fun setScreen(v: ScrollView) {
        root.removeAllViews()
        root.addView(v)
    }

    /* ---------------- Ro'yxatdan o'tish ---------------- */

    private fun showRegister() = setScreen(page {
        brand("Birinchi qadam: hisob yarating.\nKeyin himoyani yoqing va so'zlaringizni qo'shing.")
        val c = card(26, pad = 18)
        c.addView(tv("Ro'yxatdan o'tish", 18f, TEXT, true))
        val email = edit("Elektron pochta", EMAIL, 14)
        val p1 = edit("Parol (kamida 6 ta belgi)", PASS)
        val p2 = edit("Parolni takrorlang", PASS)
        val msg = tv("", 13f, RED, top = 10)
        c.addView(email)
        c.addView(p1)
        c.addView(p2)
        c.passwordToggle(p1, p2)
        c.addView(btn("Hisob yaratish", top = 16) {
            val e = email.text.toString().trim().lowercase()
            val a = p1.text.toString()
            when {
                !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> msg.text = "Elektron pochtani to'g'ri yozing."
                a.length < 6 -> msg.text = "Parol kamida 6 ta belgidan iborat bo'lsin."
                a != p2.text.toString() -> msg.text = "Parollar bir xil emas."
                else -> {
                    Prefs.register(this@MainActivity, e, a)
                    loggedIn = true
                    tab = 0
                    showMain()
                }
            }
        })
        c.addView(msg)
        addView(c)
        addView(tv("⚠️ Parolni eslab qoling: uni tiklab bo'lmaydi.", 12f, MUTED, top = 14, center = true))
    })

    /* ---------------- Kirish ---------------- */

    private fun showLogin() = setScreen(page {
        brand("Qaytganingiz bilan!\nDavom etish uchun parolni kiriting.")
        val c = card(26, pad = 18)
        c.addView(tv("Kirish", 18f, TEXT, true))
        val email = edit("Elektron pochta", EMAIL, 14)
        email.setText(Prefs.email(this@MainActivity))
        val pass = edit("Parol", PASS)
        val msg = tv("", 13f, RED, top = 10)
        val go: () -> Unit = {
            val err = Prefs.login(
                this@MainActivity,
                email.text.toString().trim().lowercase(),
                pass.text.toString()
            )
            if (err == null) {
                loggedIn = true
                showMain()
            } else {
                msg.text = err
            }
        }
        pass.setOnEditorActionListener { _, _, _ -> go(); true }
        c.addView(email)
        c.addView(pass)
        c.passwordToggle(pass)
        c.addView(btn("Kirish", top = 16) { go() })
        c.addView(msg)
        addView(c)
    })

    /* ---------------- Ochiq ogohlantirish (rozilik) ---------------- */

    private fun showConsent(openSettings: Boolean) = setScreen(page {
        addView(tv("🔒", 48f, TEXT, top = 10, center = true))
        addView(tv("Muhim: ruxsat haqida", 22f, TEXT, true, 6, true))
        addView(tv("Davom etishdan oldin shuni diqqat bilan o'qing.", 13f, MUTED, top = 4, center = true))

        val c = card(18, pad = 18)
        fun block(title: String, text: String, top: Int) {
            c.addView(tv(title, 14f, LINK, true, top))
            c.addView(tv(text, 14f, TEXT, top = 4))
        }
        block(
            "Qaysi ruxsat kerak?",
            "Himoya ishlashi uchun \"So'z Qalqoni\" telefoningizning Qulaylik xizmati (Accessibility) ruxsatidan foydalanadi.",
            0
        )
        block(
            "Nima o'qiladi?",
            "Faqat brauzerlar, Google va YouTube kabi qidiruv ilovalarida qidiruv maydoniga yozgan matningiz va brauzer manzil qatori. Parol maydonlari o'qilmaydi.",
            14
        )
        block(
            "Nima uchun?",
            "Yozilgan matn siz ro'yxatga kiritgan so'zlardan biriga mos kelsa, ogohlantirish ko'rsatish va yozilgan matnni tozalash uchun.",
            14
        )
        block(
            "Ma'lumot bilan nima qilinadi?",
            "Hech qayerga yuborilmaydi, saqlanmaydi va boshqalar bilan ulashilmaydi. Tekshiruv butunlay telefoningizning o'zida bajariladi. Faqat to'xtatilgan urinishlar soni saqlanadi.",
            14
        )
        block(
            "Nazorat sizda",
            "Rozilikni Sozlamalar bo'limidan qaytarib olishingiz yoki xizmatni telefon sozlamalaridan (Qulaylik) istalgan vaqt o'chirishingiz mumkin.",
            14
        )
        addView(c)

        addView(btn("Qabul qilaman", top = 18) {
            Prefs.setConsent(this@MainActivity, true)
            if (openSettings) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            showMain()
        })
        addView(btn("Rad etaman", Color.parseColor("#2A3150"), top = 10) {
            Prefs.setConsent(this@MainActivity, false)
            showMain()
        })
        addView(
            tv(
                "Rad etsangiz ham ilova ochiladi, lekin himoya ishlamaydi. Keyinroq Bosh sahifadan yoqishingiz mumkin.",
                12f, MUTED, top = 10, center = true
            )
        )
    })

    /* ---------------- Asosiy oyna ---------------- */

    private fun serviceEnabled(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return s.split(':').any {
            val cn = ComponentName.unflattenFromString(it)
            cn != null && cn.packageName == packageName && cn.className == BlockerService::class.java.name
        }
    }

    private fun showMain() {
        if (!Prefs.consentAsked(this)) {
            showConsent(false)
            return
        }
        root.removeAllViews()
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f)
        }
        col.addView(body)
        col.addView(buildNav())
        root.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))
        renderTab()
    }

    private fun buildNav(): LinearLayout {
        navItems.clear()
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0E142B"))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
            setPadding(0, dp(4), 0, dp(4))
        }
        val items = listOf(
            "🏠" to "Bosh sahifa",
            "📝" to "So'zlar",
            "⚙️" to "Sozlamalar"
        )
        items.forEachIndexed { i, item ->
            val t = TextView(this).apply {
                text = item.first + "\n" + item.second
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
                layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
                setOnClickListener { setTab(i) }
            }
            navItems.add(t)
            bar.addView(t)
        }
        return bar
    }

    private fun setTab(i: Int) {
        tab = i
        renderTab()
    }

    private fun renderTab() {
        words = Prefs.words(this).toMutableList()
        navItems.forEachIndexed { i, t -> t.setTextColor(if (i == tab) LINK else MUTED) }
        body.removeAllViews()
        body.addView(
            when (tab) {
                1 -> wordsTab()
                2 -> settingsTab()
                else -> homeTab()
            }
        )
    }

    /* ---------------- 1. Bosh sahifa ---------------- */

    private fun homeTab(): ScrollView = page {
        val on = serviceEnabled() && Prefs.consent(this@MainActivity)
        val hasWords = words.isNotEmpty()

        addView(tv("Salom! 👋", 24f, TEXT, true))
        addView(tv(Prefs.email(this@MainActivity), 12f, MUTED, top = 2))

        // Katta holat kartochkasi
        val st = card(18, if (on) Color.parseColor("#10291F") else Color.parseColor("#2B1622"), 22)
        st.gravity = Gravity.CENTER_HORIZONTAL
        st.addView(TextView(this@MainActivity).apply {
            text = if (on) "🛡️" else "🔓"
            textSize = 44f
            gravity = Gravity.CENTER
            background = oval(if (on) Color.parseColor("#1B4D37") else Color.parseColor("#522233"))
            layoutParams = LinearLayout.LayoutParams(dp(104), dp(104))
        })
        st.addView(tv(if (on) "Himoya faol" else "Himoya o'chiq", 22f, if (on) GREEN else RED, true, 14, true))
        st.addView(
            tv(
                if (on) "Qidiruvlaringiz kuzatilmoqda. Bloklangan so'z yozilsa, darhol ogohlantirish chiqadi."
                else "Hozir himoya ishlamayapti. Quyidagi tugmani bosib, uni yoqing.",
                13f, MUTED, top = 6, center = true
            )
        )
        if (!on) {
            st.addView(btn("Himoyani yoqish", top = 16) {
                if (Prefs.consent(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } else {
                    showConsent(true)
                }
            })
            st.addView(
                tv(
                    "Ochilgan oynada \"So'z Qalqoni\" ni toping → uni yoqing → \"Ruxsat berish\" ni bosing.\n\n" +
                        "Yoqib bo'lmasa: Sozlamalar → Ilovalar → So'z Qalqoni → ⋮ → \"Cheklangan sozlamalarga ruxsat berish\", so'ng qayta urinib ko'ring.",
                    12f, MUTED, top = 12
                )
            )
        }
        addView(st)

        // Sozlash yo'riqnomasi
        val done = listOf(true, on, hasWords)
        if (!done.all { it }) {
            val c = card(14)
            c.addView(tv("Sozlash: ${done.count { it }}/3", 16f, TEXT, true))
            c.addView(tv("✅  Hisob yaratildi", 14f, GREEN, top = 10))
            c.addView(
                tv(
                    if (on) "✅  Himoya yoqildi" else "⭕  Himoyani yoqing (yuqoridagi tugma)",
                    14f, if (on) GREEN else TEXT, top = 8
                )
            )
            val third = tv(
                if (hasWords) "✅  So'zlar qo'shildi" else "⭕  So'z qo'shing  →",
                14f, if (hasWords) GREEN else LINK, top = 8
            )
            if (!hasWords) third.setOnClickListener { setTab(1) }
            c.addView(third)
            addView(c)
        }

        // Statistika
        val row = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = lp(14)
        }
        fun stat(value: String, label: String, first: Boolean) {
            val s = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                background = round(CARD, 16)
                setPadding(dp(8), dp(14), dp(8), dp(14))
                layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply {
                    if (!first) marginStart = dp(8)
                }
            }
            s.addView(tv(value, 24f, TEXT, true, center = true))
            s.addView(tv(label, 11f, MUTED, top = 2, center = true))
            row.addView(s)
        }
        stat("🔥 " + Prefs.cleanDays(this@MainActivity), "Toza kun", true)
        stat(Prefs.todayCount(this@MainActivity).toString(), "Bugun to'xtatildi", false)
        stat(Prefs.totalCount(this@MainActivity).toString(), "Jami to'xtatildi", false)
        addView(row)

        // Kun fikri
        val q = card(14, Color.parseColor("#17224A"))
        q.addView(tv("💡 Kun fikri", 12f, LINK, true))
        val idx = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % QUOTES.size
        q.addView(tv(QUOTES[idx], 14f, TEXT, top = 6))
        addView(q)
    }

    /* ---------------- 2. So'zlar ---------------- */

    private fun wordsTab(): ScrollView = page {
        addView(tv("So'zlar ro'yxati", 22f, TEXT, true))
        addView(tv("Shu so'zlar qidirilsa, ilova sizni ogohlantiradi.", 13f, MUTED, top = 2))

        val c = card(16)
        c.addView(tv("Yangi so'z yoki ism qo'shing", 13f, MUTED))
        val row = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = lp(8)
        }
        val input = edit("masalan: jenny", PLAIN, 0).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        }
        val add = btn("Qo'shish", top = 0) { addFromInput(input) }.apply {
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = dp(8) }
            isEnabled = words.size < Prefs.MAX_WORDS
        }
        row.addView(input)
        row.addView(add)
        input.setOnEditorActionListener { _, _, _ -> addFromInput(input); true }
        c.addView(row)
        c.addView(tv("Bir nechtasini vergul bilan ajrating: jenny, jordan", 12f, MUTED, top = 8))
        val msg = tv("", 13f, RED, top = 8)
        c.addView(msg)
        pendingMsg?.let {
            msg.setTextColor(if (pendingOk) GREEN else RED)
            msg.text = it
            pendingMsg = null
        }
        addView(c)

        // Tez qo'shish
        val have = words.map { it.lowercase() }.toSet()
        val sugg = SUGGESTIONS.filter { !have.contains(it) }
        if (sugg.isNotEmpty() && words.size < Prefs.MAX_WORDS) {
            addView(tv("Tez qo'shish (bosing):", 12f, MUTED, top = 16))
            val chips = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            sugg.forEach { w ->
                chips.addView(TextView(this@MainActivity).apply {
                    text = "+ $w"
                    textSize = 14f
                    setTextColor(LINK)
                    background = round(CARD, 20)
                    setPadding(dp(16), dp(9), dp(16), dp(9))
                    layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = dp(8) }
                    setOnClickListener { addRaw(listOf(w)) }
                })
            }
            addView(HorizontalScrollView(this@MainActivity).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = lp(8)
                addView(chips)
            })
        }

        // Hisoblagich
        addView(tv("Ro'yxat: ${words.size} / ${Prefs.MAX_WORDS}", 12f, MUTED, top = 18))
        addView(ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = Prefs.MAX_WORDS
            progress = words.size
            progressTintList = ColorStateList.valueOf(BLUE)
            layoutParams = lp(6)
        })

        if (words.isEmpty()) {
            val e = card(12)
            e.addView(tv("Hozircha so'z yo'q.\nYuqoridan qo'shing yoki tez qo'shish tugmalarini bosing.", 13f, MUTED, center = true))
            addView(e)
        } else {
            words.forEachIndexed { i, w ->
                val r = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = round(CARD, 12)
                    setPadding(dp(16), dp(11), dp(8), dp(11))
                    layoutParams = lp(8)
                }
                r.addView(tv(w, 15f).apply {
                    layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
                })
                r.addView(TextView(this@MainActivity).apply {
                    text = "✕"
                    textSize = 17f
                    setTextColor(RED)
                    setPadding(dp(14), dp(4), dp(14), dp(4))
                    setOnClickListener { confirmDelete(i) }
                })
                addView(r)
            }
        }

        addView(
            tv(
                "💡 Maslahat: oddiy ism (masalan, \"jordan\") boshqa sahifalarni ham to'sishi mumkin. " +
                    "Aniqroq bo'lishi uchun \"jenny porn\" kabi birikma yozing.",
                12f, MUTED, top = 18
            )
        )
    }

    private fun addFromInput(input: EditText) {
        val raw = input.text.toString().split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (raw.isEmpty()) {
            pendingMsg = "So'z yoki ism yozing."
            pendingOk = false
            renderTab()
            return
        }
        addRaw(raw)
    }

    private fun addRaw(raw: List<String>) {
        val have = words.map { it.lowercase() }.toMutableSet()
        var added = 0
        var dup = 0
        var full = false
        for (r in raw) {
            val w = r.take(Prefs.MAX_LEN)
            if (have.contains(w.lowercase())) {
                dup++
                continue
            }
            if (words.size >= Prefs.MAX_WORDS) {
                full = true
                break
            }
            words.add(w)
            have.add(w.lowercase())
            added++
        }
        Prefs.saveWords(this, words)
        when {
            full -> {
                pendingMsg = "Limit: ${Prefs.MAX_WORDS} tadan ortiq so'z yozib bo'lmaydi. Qo'shildi: $added."
                pendingOk = false
            }
            added == 0 && dup > 0 -> {
                pendingMsg = "Bu so'z ro'yxatda allaqachon bor."
                pendingOk = false
            }
            else -> {
                pendingMsg = "Qo'shildi: $added ✅"
                pendingOk = true
            }
        }
        renderTab()
    }

    private fun confirmDelete(i: Int) {
        if (i !in words.indices) return
        AlertDialog.Builder(this)
            .setMessage("\"${words[i]}\" ro'yxatdan o'chirilsinmi?")
            .setPositiveButton("Ha, o'chirish") { _, _ ->
                if (i in words.indices) {
                    words.removeAt(i)
                    Prefs.saveWords(this, words)
                    pendingMsg = null
                    renderTab()
                }
            }
            .setNegativeButton("Yo'q", null)
            .show()
    }

    /* ---------------- 3. Sozlamalar ---------------- */

    private fun settingsTab(): ScrollView = page {
        addView(tv("Sozlamalar", 22f, TEXT, true))

        // Qattiq rejim
        val c1 = card(16)
        c1.addView(Switch(this@MainActivity).apply {
            text = "Qattiq rejim"
            setTextColor(TEXT)
            textSize = 15f
            isChecked = Prefs.strict(this@MainActivity)
            setOnCheckedChangeListener { _, v -> Prefs.setStrict(this@MainActivity, v) }
        })
        c1.addView(
            tv(
                "Yoqilsa, so'z boshqa so'z ichida kelganda ham topiladi (masalan, \"sex\" so'zi \"sexy\" ichida ham). " +
                    "O'chiq bo'lsa, faqat butun so'z topiladi.",
                12f, MUTED, top = 8
            )
        )
        addView(c1)

        // Parolni o'zgartirish
        val c2 = card(14)
        c2.addView(tv("🔑 Parolni o'zgartirish", 15f, TEXT, true))
        val old = edit("Eski parol", PASS, 12)
        val n1 = edit("Yangi parol (kamida 6 ta belgi)", PASS)
        val n2 = edit("Yangi parolni takrorlang", PASS)
        val msg = tv("", 13f, RED, top = 10)
        c2.addView(old)
        c2.addView(n1)
        c2.addView(n2)
        c2.passwordToggle(old, n1, n2)
        c2.addView(btn("Parolni saqlash", top = 14) {
            val a = n1.text.toString()
            when {
                a.length < 6 -> {
                    msg.setTextColor(RED)
                    msg.text = "Yangi parol kamida 6 ta belgidan iborat bo'lsin."
                }
                a != n2.text.toString() -> {
                    msg.setTextColor(RED)
                    msg.text = "Yangi parollar bir xil emas."
                }
                else -> {
                    val err = Prefs.changePassword(this@MainActivity, old.text.toString(), a)
                    if (err == null) {
                        msg.setTextColor(GREEN)
                        msg.text = "Parol o'zgartirildi ✅"
                        old.setText("")
                        n1.setText("")
                        n2.setText("")
                    } else {
                        msg.setTextColor(RED)
                        msg.text = err
                    }
                }
            }
        })
        c2.addView(msg)
        addView(c2)

        // Qanday ishlaydi
        val c3 = card(14)
        c3.addView(tv("❓ Qanday ishlaydi?", 15f, TEXT, true))
        c3.addView(
            tv(
                "1. So'zlar tabida bloklanadigan so'z va ismlarni qo'shasiz.\n" +
                    "2. Brauzer yoki YouTube'da shu so'z yozilsa, ilova ekranni to'sib, ogohlantirish chiqaradi va yozilgan matnni tozalaydi.\n" +
                    "3. Ogohlantirishdagi tugmalar 3 soniyadan keyin bosiladi: shu vaqt ichida to'xtab o'ylab olasiz.\n" +
                    "4. Barcha ma'lumotlar faqat shu telefonda saqlanadi, hech qayerga yuborilmaydi.",
                13f, MUTED, top = 10
            )
        )
        addView(c3)

        // Maxfiylik va rozilik
        val c4 = card(14)
        c4.addView(tv("🔒 Maxfiylik", 15f, TEXT, true))
        c4.addView(
            tv(
                "Ilova qidiruv matnini faqat telefonning o'zida tekshiradi. Hech narsa yuborilmaydi va saqlanmaydi.",
                13f, MUTED, top = 8
            )
        )
        c4.addView(
            tv(
                if (Prefs.consent(this@MainActivity)) "Holat: roziligingiz berilgan ✅" else "Holat: rozilik berilmagan",
                13f, if (Prefs.consent(this@MainActivity)) GREEN else RED, top = 8
            )
        )
        if (Prefs.consent(this@MainActivity)) {
            c4.addView(btn("Roziligimni qaytarib olish", Color.parseColor("#2A3150"), top = 12) {
                Prefs.setConsent(this@MainActivity, false)
                renderTab()
            })
        }
        addView(c4)

        addView(btn("Chiqish", Color.parseColor("#2A3150"), top = 18) {
            loggedIn = false
            showLogin()
        })
        addView(tv("So'z Qalqoni 2.1", 11f, Color.parseColor("#5C6890"), top = 14, center = true))
    }
}
