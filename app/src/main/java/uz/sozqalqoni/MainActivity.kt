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
import android.provider.Settings
import android.text.InputType
import android.util.Patterns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private val BG = Color.parseColor("#0F1424")
    private val CARD = Color.parseColor("#171E38")
    private val TEXT = Color.parseColor("#E8ECF6")
    private val MUTED = Color.parseColor("#9FB0E0")
    private val BLUE = Color.parseColor("#2C6BFF")
    private val RED = Color.parseColor("#FF8A8A")
    private val GREEN = Color.parseColor("#7BE0A0")

    private val PASS = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    private val EMAIL = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    private val PLAIN = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

    private lateinit var root: FrameLayout
    private var loggedIn = false

    private lateinit var listBox: LinearLayout
    private lateinit var countTv: TextView
    private lateinit var bar: ProgressBar
    private lateinit var addBtn: Button
    private lateinit var msgTv: TextView
    private var words = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        root = FrameLayout(this).apply { setBackgroundColor(BG) }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        when {
            !Prefs.hasUser(this) -> showRegister()
            !loggedIn -> showLogin()
            else -> showMain()
        }
    }

    // Ilova orqa fonga o'tganda qayta parol so'raladi.
    override fun onStop() {
        super.onStop()
        loggedIn = false
    }

    /* ---------------- Yordamchi funksiyalar ---------------- */

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun lp(top: Int = 0) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(top) }

    private fun round(c: Int, r: Int) = GradientDrawable().apply {
        setColor(c)
        cornerRadius = dp(r).toFloat()
    }

    private fun tv(t: String, size: Float = 14f, color: Int = TEXT, bold: Boolean = false, top: Int = 0) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            layoutParams = lp(top)
        }

    private fun edit(h: String, kind: Int, top: Int = 8) = EditText(this).apply {
        hint = h
        setHintTextColor(MUTED)
        setTextColor(TEXT)
        textSize = 15f
        background = round(CARD, 10)
        setPadding(dp(14), dp(12), dp(14), dp(12))
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
            background = round(color, 12)
            layoutParams = lp(top)
            setOnClickListener { onClick() }
        }

    private fun screen(build: LinearLayout.() -> Unit) {
        root.removeAllViews()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }
        col.build()
        val sv = ScrollView(this).apply {
            isFillViewport = true
            addView(col)
        }
        root.addView(sv)
    }

    private fun LinearLayout.header() {
        addView(tv("🛡️ So'z Qalqoni", 24f, TEXT, true))
        addView(tv("Parol bilan himoyalangan panel", 13f, MUTED, top = 2))
    }

    /* ---------------- Ro'yxatdan o'tish ---------------- */

    private fun showRegister() = screen {
        header()
        addView(tv("Ro'yxatdan o'tish", 18f, TEXT, true, 28))
        val email = edit("Elektron pochta", EMAIL, 14)
        val p1 = edit("Parol (kamida 6 ta belgi)", PASS)
        val p2 = edit("Parolni takrorlang", PASS)
        val msg = tv("", 13f, RED, top = 10)
        addView(email)
        addView(p1)
        addView(p2)
        addView(btn("Hisob yaratish") {
            val e = email.text.toString().trim().lowercase()
            val a = p1.text.toString()
            when {
                !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> msg.text = "Elektron pochtani to'g'ri yozing."
                a.length < 6 -> msg.text = "Parol kamida 6 ta belgidan iborat bo'lsin."
                a != p2.text.toString() -> msg.text = "Parollar bir xil emas."
                else -> {
                    Prefs.register(this@MainActivity, e, a)
                    loggedIn = true
                    showMain()
                }
            }
        })
        addView(msg)
        addView(tv("Parolni eslab qoling: uni tiklab bo'lmaydi.", 12f, MUTED, top = 8))
    }

    /* ---------------- Kirish ---------------- */

    private fun showLogin() = screen {
        header()
        addView(tv("Kirish", 18f, TEXT, true, 28))
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
        addView(email)
        addView(pass)
        addView(btn("Kirish") { go() })
        addView(msg)
    }

    /* ---------------- Asosiy panel ---------------- */

    private fun serviceEnabled(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return s.split(':').any {
            val cn = ComponentName.unflattenFromString(it)
            cn != null && cn.packageName == packageName && cn.className == BlockerService::class.java.name
        }
    }

    private fun showMain() {
        words = Prefs.words(this).toMutableList()
        screen {
            val header = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = lp(0)
            }
            header.addView(tv("🛡️ So'z Qalqoni", 20f, TEXT, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            header.addView(tv("Chiqish", 14f, Color.parseColor("#8FB0FF")).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(dp(10), dp(6), dp(0), dp(6))
                setOnClickListener {
                    loggedIn = false
                    showLogin()
                }
            })
            addView(header)
            addView(tv(Prefs.email(this@MainActivity), 12f, MUTED, top = 2))

            // Himoya holati
            val on = serviceEnabled()
            val card = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                background = round(CARD, 14)
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = lp(16)
            }
            card.addView(
                tv(
                    if (on) "✅ Himoya yoqilgan" else "⚠️ Himoya o'chiq",
                    15f, if (on) GREEN else RED, true
                )
            )
            if (on) {
                card.addView(tv("Bloklangan so'z qidirilsa, ogohlantirish chiqadi.", 13f, MUTED, top = 6))
                card.addView(
                    tv(
                        "Bugun to'xtatilgan urinishlar: ${Prefs.todayCount(this@MainActivity)} (jami: ${Prefs.totalCount(this@MainActivity)})",
                        13f, TEXT, top = 8
                    )
                )
                card.addView(
                    tv("Sinab ko'rish: brauzer yoki YouTube qidiruviga ro'yxatdagi so'zni yozing.", 12f, MUTED, top = 8)
                )
            } else {
                card.addView(tv("Qidiruvni kuzatish uchun \"So'z Qalqoni\" xizmatini yoqing.", 13f, MUTED, top = 6))
                card.addView(btn("Himoyani yoqish", top = 10) {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                })
                card.addView(
                    tv(
                        "Yoqib bo'lmasa: Sozlamalar → Ilovalar → So'z Qalqoni → ⋮ (yuqori o'ng burchak) → \"Cheklangan sozlamalarga ruxsat berish\". Keyin yana urinib ko'ring.",
                        12f, MUTED, top = 8
                    )
                )
            }
            addView(card)

            // So'z qo'shish
            addView(tv("Bloklanadigan so'z yoki ism", 13f, MUTED, top = 20))
            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = lp(6)
            }
            val input = edit("masalan: jenny", PLAIN, 0).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            addBtn = btn("Qo'shish", top = 0) { addWords(input) }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(8) }
            }
            row.addView(input)
            row.addView(addBtn)
            addView(row)
            input.setOnEditorActionListener { _, _, _ -> addWords(input); true }

            addView(tv("Bir nechtasini vergul bilan ajratib yozish mumkin.", 12f, MUTED, top = 6))
            msgTv = tv("", 13f, RED, top = 6)
            addView(msgTv)

            countTv = tv("", 12f, MUTED, top = 14)
            addView(countTv)
            bar = ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = Prefs.MAX_WORDS
                progressTintList = ColorStateList.valueOf(BLUE)
                layoutParams = lp(6)
            }
            addView(bar)

            listBox = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = lp(10)
            }
            addView(listBox)

            val sw = Switch(this@MainActivity).apply {
                text = "Qattiq rejim: so'z boshqa so'z ichida kelganda ham topadi (sex → sexy)"
                setTextColor(MUTED)
                textSize = 13f
                isChecked = Prefs.strict(this@MainActivity)
                layoutParams = lp(18)
                setOnCheckedChangeListener { _, c -> Prefs.setStrict(this@MainActivity, c) }
            }
            addView(sw)

            render()
        }
    }

    private fun render() {
        countTv.text = "Ro'yxat: ${words.size} / ${Prefs.MAX_WORDS}"
        bar.progress = words.size
        addBtn.isEnabled = words.size < Prefs.MAX_WORDS
        listBox.removeAllViews()
        if (words.isEmpty()) {
            listBox.addView(tv("Hozircha so'z yo'q. Yuqoridan qo'shing.", 13f, MUTED))
            return
        }
        words.forEachIndexed { i, w ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = round(CARD, 10)
                setPadding(dp(14), dp(10), dp(8), dp(10))
                layoutParams = lp(6)
            }
            row.addView(tv(w, 14f).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = "✕"
                textSize = 16f
                setTextColor(RED)
                setPadding(dp(12), dp(4), dp(12), dp(4))
                setOnClickListener { confirmDelete(i) }
            })
            listBox.addView(row)
        }
    }

    private fun confirmDelete(i: Int) {
        if (i !in words.indices) return
        AlertDialog.Builder(this)
            .setMessage("\"${words[i]}\" ro'yxatdan o'chirilsinmi?")
            .setPositiveButton("Ha") { _, _ ->
                if (i in words.indices) {
                    words.removeAt(i)
                    Prefs.saveWords(this, words)
                    render()
                    msgTv.text = ""
                }
            }
            .setNegativeButton("Yo'q", null)
            .show()
    }

    private fun addWords(input: EditText) {
        val raw = input.text.toString().split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (raw.isEmpty()) {
            msgTv.setTextColor(RED)
            msgTv.text = "So'z yoki ism yozing."
            return
        }
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
        input.setText("")
        render()
        when {
            full -> {
                msgTv.setTextColor(RED)
                msgTv.text = "Limit: ${Prefs.MAX_WORDS} tadan ortiq so'z yozib bo'lmaydi. Qo'shildi: $added."
            }
            added == 0 && dup > 0 -> {
                msgTv.setTextColor(RED)
                msgTv.text = "Bu so'z ro'yxatda allaqachon bor."
            }
            else -> {
                msgTv.setTextColor(GREEN)
                msgTv.text = "Qo'shildi: $added."
            }
        }
    }
}
