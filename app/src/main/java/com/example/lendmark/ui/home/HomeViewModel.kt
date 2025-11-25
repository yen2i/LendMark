package com.example.lendmark.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.lendmark.ui.home.adapter.Announcement
import com.example.lendmark.ui.home.adapter.Room

// Firestore 기반 Upcoming 예약
data class UpcomingReservationInfo(
    val reservationId: String,
    val roomName: String,
    val time: String
)

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private val _announcements = MutableLiveData<List<Announcement>>()
    val announcements: LiveData<List<Announcement>> = _announcements

    private val _frequentlyUsedRooms = MutableLiveData<List<Room>>()
    val frequentlyUsedRooms: LiveData<List<Room>> = _frequentlyUsedRooms

    private val _upcomingReservation = MutableLiveData<UpcomingReservationInfo?>()
    val upcomingReservation: LiveData<UpcomingReservationInfo?> = _upcomingReservation

    fun loadHomeData() {

        // (1) 홈 공지
        _announcements.value = listOf(
            Announcement("Announcement", "Mon - Fri 09:00 - 18:00\nHolidays and vacations are closed"),
            Announcement("Review Event", "If you leave a review for your classroom, we will give you a voucher.")
        )

        // (2) 자주 사용하는 강의실 더미 (홈 UI 유지용)
        _frequentlyUsedRooms.value = listOf(
            Room("Frontier Hall 107", ""),
            Room("Dasan Hall 301", ""),
            Room("Mirae Hall 205", "")
        )

        // (3) Firestore에서 곧 있을 예약 불러오기
        loadUpcomingReservation()
    }

    private fun loadUpcomingReservation() {
        if (uid == null) {
            _upcomingReservation.value = null
            return
        }

        db.collection("reservations")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", "approved")
            .orderBy("timestamp") // 가장 가까운 예약 순으로 정렬
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
                val periodStart = doc.getLong("periodStart")?.toInt() ?: 0
                val periodEnd = doc.getLong("periodEnd")?.toInt() ?: 0
                val date = doc.getString("date") ?: ""

                val time = "${periodToTime(periodStart)} - ${periodToTime(periodEnd)}"

                _upcomingReservation.value = UpcomingReservationInfo(
                    reservationId = doc.id,
                    roomName = "$buildingId $roomId",
                    time = "$date • $time"
                )
            }
            .addOnFailureListener {
                _upcomingReservation.value = null
            }
    }

    private fun periodToTime(period: Int): String {
        val hour = 8 + period
        return String.format("%02d:00", hour)
    }
}
