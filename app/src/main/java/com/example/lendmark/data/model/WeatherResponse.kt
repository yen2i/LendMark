package com.example.lendmark.data.sources.announcement.models

data class WeatherResponse(
    val weather: List<WeatherInfo>,
    val main: MainInfo
)

data class WeatherInfo(
    val description: String,
    val icon: String
)

data class MainInfo(
    val temp: Double
)
