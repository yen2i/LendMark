package com.example.lendmark.data.sources.announcement

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class AcademicCrawler {

    suspend fun fetchAcademicSchedule(): AnnouncementItem.AcademicSchedule? =
        withContext(Dispatchers.IO) {

            return@withContext try {
                val doc = Jsoup.connect("https://www.seoultech.ac.kr/life/sch/common").get()

                val firstRow = doc.select("table tbody tr").firstOrNull()
                    ?: return@withContext null

                val date = firstRow.select("td").getOrNull(0)?.text().orEmpty()
                val title = firstRow.select("td").getOrNull(1)?.text().orEmpty()

                AnnouncementItem.AcademicSchedule(date, title)

            } catch (e: Exception) {
                null
            }
        }
}
