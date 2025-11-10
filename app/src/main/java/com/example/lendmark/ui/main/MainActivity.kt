package com.example.lendmark.ui.main

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.lendmark.R
import com.example.lendmark.ui.home.HomeFragment
import com.example.lendmark.ui.my.MyPageFragment
import com.example.lendmark.ui.reservation.BuildingListFragment
import com.example.lendmark.ui.reservation.ReservationMapFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // initial DB//uploadInitialData()
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // 첫 화면: HOME
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_book -> {
                    // BOOK 탭에서 어떤 화면 보여줄지: 일단 BuildingListFragment 사용
                    replaceFragment(BuildingListFragment())
                    true
                }
                R.id.nav_map -> {
                    replaceFragment(ReservationMapFragment())
                    true
                }
                R.id.nav_my -> {
                    replaceFragment(MyPageFragment())
                    true
                }
                else -> false
            }
        }
        // initial DB // val db = FirebaseFirestore.getInstance()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .commit()
    }

    // initial DB

//    fun uploadInitialData() {
//        val db = FirebaseFirestore.getInstance()
//
//        // 1. Buildings & Rooms
//        val dasan = hashMapOf(
//            "name" to "Dasan Hall",
//            "code" to -1,
//            "naverMapLat" to 37.631,
//            "naverMapLng" to 127.078
//        )
//        db.collection("buildings").document("dasan").set(dasan)
//
//        val rooms = listOf(
//            mapOf("name" to "101", "capacity" to 40,
//                "facilities" to listOf("projector", "whiteboard", "power-outlets")),
//            mapOf("name" to "107", "capacity" to 120,
//                "facilities" to listOf("projector", "whiteboard", "mic", "hdmi", "aircon"))
//        )
//        for (room in rooms) {
//            db.collection("buildings").document("dasan")
//                .collection("rooms").document("dasan-${room["name"]}")
//                .set(room)
//        }
//
//        // 2. Tag catalog
//        val tags = listOf(
//            mapOf("id" to "projector", "group" to "facility", "labelEn" to "Projector", "icon" to "projector"),
//            mapOf("id" to "whiteboard", "group" to "facility", "labelEn" to "Whiteboard", "icon" to "whiteboard"),
//            mapOf("id" to "power-outlets", "group" to "facility", "labelEn" to "Power outlets", "icon" to "power"),
//            mapOf("id" to "hdmi", "group" to "facility","labelEn" to "HDMI", "icon" to "hdmi"),
//            mapOf("id" to "mic", "group" to "facility", "labelEn" to "Microphone", "icon" to "mic"),
//            mapOf("id" to "aircon", "group" to "facility", "labelEn" to "Air conditioning", "icon" to "aircon"),
//            mapOf("id" to "large-hall", "group" to "roomType", "labelEn" to "Large lecture hall", "icon" to "hall")
//        )
//        for (tag in tags) {
//            db.collection("tag_catalog").document(tag["id"] as String).set(tag)
//        }
//
//        // 3. Semester / roomSchedules
//        val weekly = listOf(
//            mapOf("dow" to 1, "startMin" to 540, "endMin" to 630, "courseId" to "CS101", "title" to "Intro to CS", "dept" to "CSE"),
//            mapOf("dow" to 3, "startMin" to 780, "endMin" to 870, "courseId" to "EE210", "title" to "Circuits", "dept" to "EE")
//        )
//        val schedule = mapOf("weekly" to weekly)
//        db.collection("semesters").document("2025-fall")
//            .collection("roomSchedules").document("dasan(01)-107")
//            .set(schedule)
//
//
//        // 4. Users (sample)
//        val users = listOf(
//            mapOf(
//                "id" to "phy9558",
//                "email" to "phy9558@gmail.com",
//                "nickname" to "유영",
//                "createdAt" to FieldValue.serverTimestamp()
//            ),
//            mapOf(
//                "id" to "testuser",
//                "email" to "testuser@example.com",
//                "nickname" to "테스터",
//                "createdAt" to FieldValue.serverTimestamp()
//            )
//        )
//
//        for (user in users) {
//            val userId = user["id"] as String
//            db.collection("users").document(userId).set(user)
//
//            // 기본 즐겨찾기 샘플
//            val favorite = mapOf(
//                "buildingId" to "dasan",
//                "roomId" to "dasan-107",
//                "createdAt" to FieldValue.serverTimestamp()
//            )
//            db.collection("users").document(userId)
//                .collection("favorites").add(favorite)
//        }
//
//        Log.d("FirebaseSeed", "Initial Data Upload successfully")
//
//
//
//
//
//    }




}
