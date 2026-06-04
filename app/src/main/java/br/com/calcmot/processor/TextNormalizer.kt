package br.com.calcmot.processor

import java.text.Normalizer
import java.util.Locale

object TextNormalizer {

    fun clean(value: String): String {
        return value
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace("┬á", " ")
            .replace("├º", "c")
            .replace("├ó", "a")
            .replace("├ú", "u")
            .replace("├⌐", "e")
            .replace("├í", "a")
            .replace("├¡", "i")
            .replace("Ã¢", "a")
            .replace("Ã£", "a")
            .replace("Ã¡", "a")
            .replace("Ã©", "e")
            .replace("Ã­", "i")
            .replace("Ã³", "o")
            .replace("Ãº", "u")
            .replace("Ã§", "c")
            .replace(Regex("""[ \t\r\f]+"""), " ")
            .trim()
    }

    fun searchKey(value: String): String {
        val decomposed = Normalizer.normalize(clean(value), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("""(?<=\d\.)[li](?=\s*km\b)"""), "1")
            .replace(Regex("""(?<=\d)[li](?=\s*km\b)"""), "1")
            .replace(Regex("""(?<=\d)[li](?=\s*(?:min|minuto))"""), "1")
            .replace(Regex("""\b[li]{2}(?=\s*(?:min|minuto))"""), "11")
    }
}
