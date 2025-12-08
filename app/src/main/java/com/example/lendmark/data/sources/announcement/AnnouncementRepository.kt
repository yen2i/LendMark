package com.example.lendmark.data.sources.announcement

class AnnouncementRepository(
    private val weatherRepo: WeatherRepository,
    private val academicCrawler: AcademicCrawler
) {

    suspend fun loadAnnouncements(): List<AnnouncementItem> {
        val slides = mutableListOf<AnnouncementItem>()

        // 1️⃣ 리뷰 슬라이드 가장 앞으로!
        slides.add(AnnouncementItem.ReviewEvent)

        // 2️⃣ 그 다음: 날씨 (있을 때만)
        weatherRepo.getWeather()?.let { slides.add(it) }

        // 3️⃣ 그 다음: 학사 일정
        academicCrawler.fetchAcademicSchedule()?.let { slides.add(it) }

        // 4️⃣ 마지막: A+
        slides.add(AnnouncementItem.PleaseGiveAPlus)

        return slides
    }
}

