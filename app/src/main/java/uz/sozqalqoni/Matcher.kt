package uz.sozqalqoni

import java.text.Normalizer

/** So'zlarni solishtirish: kichik harf, tinish belgilarsiz, butun so'z bo'yicha. */
object Matcher {
    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

    fun norm(s: String?): String =
        NON_WORD.replace(Normalizer.normalize((s ?: "").lowercase(), Normalizer.Form.NFKC), " ").trim()

    /** Mos kelgan bloklangan so'zni qaytaradi, topilmasa null. */
    fun find(text: String?, list: List<String>, strict: Boolean): String? {
        val q = " " + norm(text) + " "
        if (q.isBlank()) return null
        for (w in list) {
            val n = norm(w)
            if (n.isEmpty()) continue
            val hit = if (strict) q.contains(n) else q.contains(" $n ")
            if (hit) return w
        }
        return null
    }
}
