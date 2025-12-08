package com.example.lendmark.data.sources.announcement

import android.util.Log
import com.example.lendmark.R
import com.example.lendmark.data.sources.announcement.models.WeatherResponse
import com.example.lendmark.data.sources.announcement.AnnouncementItem.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {

    companion object {
        private const val DEFAULT_LAT = 37.6315      // 서울과기대 기본값
        private const val DEFAULT_LON = 127.0770
        private const val API_KEY = "3a622ee8a3a87bef4fcc32763095d683"   // ← 여기에 너의 API KEY 넣어야 함!
    }

    // 현재 좌표 (사용자 위치 사용 시 업데이트 가능)
    private var lat: Double = DEFAULT_LAT
    private var lon: Double = DEFAULT_LON

    fun setLocation(newLat: Double, newLon: Double) {
        lat = newLat
        lon = newLon
    }

    // Retrofit API
    private val api = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)

    /**
     * ⭐ OpenWeatherMap 데이터 불러오기
     * 날씨 설명 → 한국어 변환
     * 아이콘 코드 → 우리 앱 아이콘 리소스 변환
     */
    suspend fun getWeather(): Weather? {
        return try {
            val response: WeatherResponse = api.getWeather(
                lat = lat,
                lon = lon,
                apiKey = API_KEY
            )

            val weatherInfo = response.weather.firstOrNull()
            val temp = response.main.temp
            val descEng = weatherInfo?.description ?: ""
            val iconCode = weatherInfo?.icon ?: ""

            Weather(
                temperature = "${temp.toInt()}°C",
                description = mapDescription(descEng),
                iconRes = mapIcon(iconCode)
            )

        } catch (e: Exception) {
            Log.e("WeatherRepository", "날씨 로드 실패: ${e.message}")
            null
        }
    }

    /**
     * ⭐ 영어 → 한국어 날씨 설명 매핑
     */
    private fun mapDescription(desc: String): String {
        return when (desc.lowercase()) {
            "clear sky" -> "맑음"
            "few clouds" -> "구름 조금"
            "scattered clouds" -> "흩어진 구름"
            "broken clouds" -> "대체로 흐림"
            "overcast clouds" -> "흐림"
            "light rain" -> "약한 비"
            "moderate rain" -> "보통 비"
            "heavy intensity rain" -> "강한 비"
            "shower rain" -> "소나기"
            "rain" -> "비"
            "thunderstorm" -> "천둥번개"
            "snow" -> "눈"
            "mist" -> "안개"
            else -> desc     // 모르는 내용은 원문 표시
        }
    }

    /**
     * ⭐ 아이콘 코드 → 앱의 이미지 리소스 매핑
     */
    private fun mapIcon(code: String): Int {
        return when (code) {
            "01d", "01n" -> R.drawable.ic_weather_sunny
            "02d", "02n" -> R.drawable.ic_weather_partly
            "03d", "03n" -> R.drawable.ic_weather_cloud
            "04d", "04n" -> R.drawable.ic_weather_cloud
            "09d", "09n" -> R.drawable.ic_weather_rain
            "10d", "10n" -> R.drawable.ic_weather_shower
            "11d", "11n" -> R.drawable.ic_weather_thunder
            "13d", "13n" -> R.drawable.ic_weather_snow
            "50d", "50n" -> R.drawable.ic_weather_fog
            else -> R.drawable.ic_weather_unknown
        }
    }
}
