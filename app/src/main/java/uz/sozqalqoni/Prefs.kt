package uz.sozqalqoni

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONArray
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Hisob, so'zlar ro'yxati va statistika faqat shu telefonda saqlanadi. */
object Prefs {
    const val MAX_WORDS = 100
    const val MAX_LEN = 60
    const val FILE = "sozqalqoni"
    private const val MAX_FAILS = 5
    private const val LOCK_MS = 60_000L

    private fun sp(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun hash(pass: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pass.toCharArray(), salt, 100_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    fun hasUser(c: Context) = sp(c).contains("hash")

    fun email(c: Context): String = sp(c).getString("email", "") ?: ""

    fun register(c: Context, email: String, pass: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sp(c).edit()
            .putString("email", email)
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash(pass, salt), Base64.NO_WRAP))
            .putString("words", "[]")
            .putBoolean("strict", false)
            .putInt("fails", 0)
            .putLong("lockUntil", 0)
            .apply()
    }

    /** null = muvaffaqiyatli, aks holda xato matni. */
    fun login(c: Context, email: String, pass: String): String? {
        val s = sp(c)
        val now = System.currentTimeMillis()
        val lock = s.getLong("lockUntil", 0)
        if (lock > now) {
            return "Juda ko'p urinish. ${(lock - now + 999) / 1000} soniyadan keyin urinib ko'ring."
        }
        val salt = Base64.decode(s.getString("salt", "") ?: "", Base64.NO_WRAP)
        val saved = Base64.decode(s.getString("hash", "") ?: "", Base64.NO_WRAP)
        val ok = email == (s.getString("email", "") ?: "") && MessageDigest.isEqual(hash(pass, salt), saved)
        if (ok) {
            s.edit().putInt("fails", 0).putLong("lockUntil", 0).apply()
            return null
        }
        val nf = s.getInt("fails", 0) + 1
        return if (nf >= MAX_FAILS) {
            s.edit().putInt("fails", 0).putLong("lockUntil", now + LOCK_MS).apply()
            "Parol 5 marta xato kiritildi. 1 daqiqa kuting."
        } else {
            s.edit().putInt("fails", nf).apply()
            "Elektron pochta yoki parol xato. Qolgan urinishlar: ${MAX_FAILS - nf}"
        }
    }

    fun words(c: Context): List<String> {
        val arr = JSONArray(sp(c).getString("words", "[]") ?: "[]")
        return List(arr.length()) { arr.getString(it) }
    }

    fun saveWords(c: Context, list: List<String>) {
        sp(c).edit().putString("words", JSONArray(list).toString()).apply()
    }

    fun strict(c: Context) = sp(c).getBoolean("strict", false)

    fun setStrict(c: Context, v: Boolean) {
        sp(c).edit().putBoolean("strict", v).apply()
    }

    /* ---------- Statistika: nechta urinish to'xtatildi ---------- */

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun recordBlock(c: Context) {
        val s = sp(c)
        val t = today()
        val todayCount = if (s.getString("statDay", "") == t) s.getInt("statToday", 0) else 0
        s.edit()
            .putString("statDay", t)
            .putInt("statToday", todayCount + 1)
            .putInt("statTotal", s.getInt("statTotal", 0) + 1)
            .apply()
    }

    fun todayCount(c: Context): Int {
        val s = sp(c)
        return if (s.getString("statDay", "") == today()) s.getInt("statToday", 0) else 0
    }

    fun totalCount(c: Context): Int = sp(c).getInt("statTotal", 0)
}
