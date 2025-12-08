package com.example.lendmark.ui.home

import android.util.Log
import androidx.lifecycle.*
import com.example.lendmark.data.local.RecentRoomEntity
import com.example.lendmark.data.sources.announcement.*
import com.example.lendmark.ui.home.adapter.Room
import com.example.lendmark.ui.main.MyApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

data class UpcomingReservationInfo(
    val reservationId: String,
    val roomName: String,
    val time: String
)

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    // ⭐ ANNOUNCEMENT 슬라이드 구성 요소들
    private val announcementRepo = AnnouncementRepository(
        weatherRepo = WeatherRepository(),
        academicCrawler = AcademicCrawler()
    )

    private val _announcementSlides = MutableLiveData<List<AnnouncementItem>>()
    val announcementSlides: LiveData<List<AnnouncementItem>> = _announcementSlides



    private val _frequentlyUsedRooms = MutableLiveData<List<Room>>()
    val frequentlyUsedRooms: LiveData<List<Room>> = _frequentlyUsedRooms

    private val _upcomingReservation = MutableLiveData<UpcomingReservationInfo?>()
    val upcomingReservation: LiveData<UpcomingReservationInfo?> = _upcomingReservation

    private val _recentViewedRooms = MutableLiveData<List<RecentRoomEntity>>()
    val recentViewedRooms: LiveData<List<RecentRoomEntity>> = _recentViewedRooms

    private val _searchResults = MutableLiveData<List<String>>()
    val searchResults: LiveData<List<String>> = _searchResults


    fun loadHomeData() {
        loadAnnouncementSlides()
        loadFrequentlyUsedRooms()
        loadUpcomingReservation()
        loadRecentViewedRooms()
    }

    private fun loadAnnouncementSlides() {
        viewModelScope.launch {
            val slides = announcementRepo.loadAnnouncements()
            _announcementSlides.postValue(slides)
        }
    }

    fun loadRecentViewedRooms() {
        viewModelScope.launch {
            val dao = MyApp.database.recentRoomDao()
            _recentViewedRooms.postValue(dao.getRecentRooms())
        }
    }

    fun addRecentViewedRoom(roomId: String, buildingId: String, roomName: String) {
        viewModelScope.launch {
            val dao = MyApp.database.recentRoomDao()
            dao.insertRecentRoom(
                RecentRoomEntity(
                    roomId = roomId,
                    buildingId = buildingId,
                    roomName = roomName,
                    viewedAt = System.currentTimeMillis()
                )
            )
            dao.trimRecentRooms()
            loadRecentViewedRooms()
        }
    }

    private fun loadFrequentlyUsedRooms() {
        if (uid == null) {
            _frequentlyUsedRooms.value = emptyList()
            return
        }

        db.collection("reservations")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    _frequentlyUsedRooms.value = emptyList()
                    return@addOnSuccessListener
                }

                val roomCounts = docs.mapNotNull { doc ->
                    val b = doc.getString("buildingId")
                    val r = doc.getString("roomId")
                    if (b != null && r != null) "$b $r" else null
                }.groupingBy { it }.eachCount()

                val top = roomCounts.entries.sortedByDescending { it.value }.take(3)
                val result = mutableListOf<Room>()

                top.forEach { entry ->
                    val (buildingId, roomId) = entry.key.split(" ")
                    db.collection("buildings").document(buildingId)
                        .get()
                        .addOnSuccessListener { doc ->
                            val img = doc.getString("imageUrl") ?: ""
                            result.add(Room("$buildingId Hall $roomId", img))

                            if (result.size == top.size) {
                                _frequentlyUsedRooms.value = result
                            }
                        }
                }
            }
    }

    private fun loadUpcomingReservation() {
        if (uid == null) {
            _upcomingReservation.value = null
            return
        }

        db.collection("reservations")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", "approved")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp.now())
            .orderBy("timestamp")
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    _upcomingReservation.value = null
                    return@addOnSuccessListener
                }

                val doc = snap.documents.first()

                val buildingId = doc.getString("buildingId") ?: ""
                val roomId = doc.getString("roomId") ?: ""
                val start = doc.getLong("periodStart")?.toInt() ?: 0
                val end = doc.getLong("periodEnd")?.toInt() ?: 0
                val date = doc.getString("date") ?: ""

                _upcomingReservation.value = UpcomingReservationInfo(
                    reservationId = doc.id,
                    roomName = "$buildingId $roomId",
                    time = "$date • ${periodToTime(start)} - ${periodToTime(end)}"
                )
            }
    }

    private fun periodToTime(period: Int): String {
        val hour = 8 + period
        return String.format("%02d:00", hour)
    }

    fun searchBuilding(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        db.collection("buildings")
            .get()
            .addOnSuccessListener { result ->
                val matched = result.documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    if (name != null && name.contains(clean, ignoreCase = true)) name else null
                }
                _searchResults.value = matched
            }
            .addOnFailureListener {
                _searchResults.value = emptyList()
            }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}
