package com.example.lendmark.data.sources.announcement

sealed class AnnouncementItem {

    data class Weather(
        val temperature: String,
        val description: String,
        val iconRes: Int  // ← 추가!!
    ) : AnnouncementItem()

    data class AcademicSchedule(
        val date: String,
        val title: String
    ) : AnnouncementItem()

    object ReviewEvent : AnnouncementItem()
    object PleaseGiveAPlus : AnnouncementItem()
}

