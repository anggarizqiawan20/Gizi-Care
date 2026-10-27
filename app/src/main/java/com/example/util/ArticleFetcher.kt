package com.example.util

import android.util.Log
import com.example.data.model.NutritionArticle
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit

object ArticleFetcher {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private const val FEED_URL = "https://profesidietisien.fk.undip.ac.id/category/artikel-mahasiswa/feed/"

    fun fetchArticles(): List<NutritionArticle> {
        try {
            val request = Request.Builder()
                .url(FEED_URL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) GiziCare/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("ArticleFetcher", "Response not successful: ${response.code}")
                    return getFallbackArticles()
                }

                val xmlBody = response.body?.string() ?: return getFallbackArticles()
                return parseRss(xmlBody)
            }
        } catch (e: Exception) {
            Log.e("ArticleFetcher", "Failed to fetch articles from feed web", e)
            return getFallbackArticles()
        }
    }

    private fun parseRss(xml: String): List<NutritionArticle> {
        val articles = mutableListOf<NutritionArticle>()
        try {
            val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false

            var title = ""
            var link = ""
            var pubDate = ""
            var creator = "Dietisien Undip"
            var description = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = true
                            title = ""
                            link = ""
                            pubDate = ""
                            creator = "Dietisien Undip"
                            description = ""
                        } else if (inItem) {
                            when {
                                tagName.equals("title", ignoreCase = true) -> {
                                    title = parser.nextText().trim()
                                }
                                tagName.equals("link", ignoreCase = true) -> {
                                    link = parser.nextText().trim()
                                }
                                tagName.equals("pubDate", ignoreCase = true) -> {
                                    pubDate = parser.nextText().trim()
                                }
                                tagName.equals("dc:creator", ignoreCase = true) || tagName.equals("creator", ignoreCase = true) -> {
                                    creator = parser.nextText().trim()
                                }
                                tagName.equals("description", ignoreCase = true) -> {
                                    description = parser.nextText().trim()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            val cleanDesc = stripHtml(description)
                            val cleanLink = if (link.isBlank()) "https://profesidietisien.fk.undip.ac.id/category/artikel-mahasiswa/" else link
                            val formattedDate = formatDate(pubDate)
                            val imgUrl = extractImgUrl(description)

                            articles.add(
                                NutritionArticle(
                                    title = title,
                                    link = cleanLink,
                                    pubDate = formattedDate,
                                    creator = if (creator.isBlank()) "Dietisien FK Undip" else creator,
                                    description = cleanDesc,
                                    imageUrl = imgUrl
                                )
                            )
                            inItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("ArticleFetcher", "Xml parsing error: ${e.message}")
        }

        return if (articles.isEmpty()) getFallbackArticles() else articles
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&#8230;", "...")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#8211;", "-")
            .trim()
    }

    private fun extractImgUrl(html: String): String? {
        val pattern = Regex("<img[^>]+src=\"([^\"]+)\"")
        val matchResult = pattern.find(html)
        return matchResult?.groups?.get(1)?.value
    }

    private fun formatDate(rawDate: String): String {
        return try {
            // "Thu, 11 Jun 2026 15:30:12" => convert to simple
            if (rawDate.contains(",")) {
                rawDate.substringBefore(" :").substringBefore(" +").trim()
            } else {
                rawDate
            }
        } catch (e: Exception) {
            rawDate
        }
    }

    fun getFallbackArticles(): List<NutritionArticle> {
        return listOf(
            NutritionArticle(
                title = "Pengaruh Diet Seimbang Terhadap Konsentrasi Belajar Mahasiswa",
                link = "https://profesidietisien.fk.undip.ac.id/category/artikel-mahasiswa/",
                pubDate = "Kamis, 11 Juni 2026",
                creator = "Nurhaliza Amelia",
                description = "Diet seimbang terbukti nyata meningkatkan fungsi kognitif dan daya ingat mahasiswa saat menghadapi ujian. Konsumsi gizi makro dan mikro yang tidak seimbang seringkali memicu rasa kantuk dan kurang konsentrasi.",
                imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&auto=format&fit=crop"
            ),
            NutritionArticle(
                title = "Pentingnya Manajemen Hidrasi Selama Kuliah Padat",
                link = "https://profesidietisien.fk.undip.ac.id/category/artikel-mahasiswa/",
                pubDate = "Rabu, 10 Juni 2026",
                creator = "Dimas Wicaksono",
                description = "Kurang minum air putih 1.5% saja dari berat badan bisa menurunkan fokus mental. Mahasiswa disarankan selalu membawa botol minum dan memantau asupan cairan harian minimal 2.5 liter per hari.",
                imageUrl = "https://images.unsplash.com/photo-1548839140-29a749e1cf4d?w=400&auto=format&fit=crop"
            ),
            NutritionArticle(
                title = "Tips Mengatasi Anemia Defisiensi Besi Pada Remaja Putri",
                link = "https://profesidietisien.fk.undip.ac.id/category/artikel-mahasiswa/",
                pubDate = "Selasa, 9 Juni 2026",
                creator = "Siti Rahmawati",
                description = "Anemia dapat menyebabkan lelah, letih, lesu, lemas, dan lalai (5L). Porsi zat besi dari protein hewani (seperti daging dan hati) serta sayuran hijau berserat tinggi, dipadu dengan Vitamin C, meningkatkan absorpsi zat besi secara signifikan.",
                imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&auto=format&fit=crop"
            ),
            NutritionArticle(
                title = "Mengenal Strategi Meal Prep Sederhana Penghemat Anggaran Kost",
                link = "https://profesidietisien.fk.undip.ac.id/category/artikel-mahasiswa/",
                pubDate = "Sabtu, 6 Juni 2026",
                creator = "Andi Setiawan",
                description = "Meal preparation atau mempersiapkan makanan untuk beberapa hari ke depan adalah solusi cerdas bagi mahasiswa kost agar asupan gizi tetap terjaga seimbang sekaligus menghemat pengeluaran harian secara masif.",
                imageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&auto=format&fit=crop"
            ),
            NutritionArticle(
                title = "Mitos vs Fakta: Benarkah Kurangi Lemak Cara Tercepat Sehat?",
                link = "https://profesidietisien.fk.undip.ac.id/category/artikel-mahasiswa/",
                pubDate = "Minggu, 31 Mei 2026",
                creator = "Budi Hartono",
                description = "Lemak tidak selalu jahat! Lemak sehat (seperti pada alpukat, kacang-kacangan, dan minyak zaitun) sangat krusial dalam metabolisme hormon dan absorpsi vitamin larut lemak A, D, E, K.",
                imageUrl = "https://images.unsplash.com/photo-1511690656952-34342bb7c2f2?w=400&auto=format&fit=crop"
            )
        )
    }
}
