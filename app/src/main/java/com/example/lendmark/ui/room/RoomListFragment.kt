package com.example.lendmark.ui.room

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lendmark.data.model.Room
import com.example.lendmark.databinding.FragmentRoomListBinding
import com.google.firebase.firestore.FirebaseFirestore

class RoomListFragment : Fragment() {

    private var _binding: FragmentRoomListBinding? = null
    private val binding get() = _binding!!

    private lateinit var buildingId: String   // 🔥 Firestore 문서 ID (예: "38")
    private lateinit var buildingName: String // UI 표시용

    private val db = FirebaseFirestore.getInstance()
    private val roomList = mutableListOf<Room>()
    private lateinit var adapter: RoomListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // MainActivity에서 전달한 값 받기
        buildingId = arguments?.getString("buildingId") ?: ""
        buildingName = arguments?.getString("buildingName") ?: ""

        //Log.d("ROOM_DEBUG", "BuildingId: $buildingId")

        // 상단 제목 설정
        binding.tvBuildingTitle.text = buildingName

        // RecyclerView 설정
        adapter = RoomListAdapter(
            rooms = roomList,
            onRoomClick = { room ->
                Toast.makeText(requireContext(),
                    "${room.name} 선택됨 (시간표 이동 예정)", Toast.LENGTH_SHORT).show()
            },
            onMoreInfoClick = { room ->
                Toast.makeText(requireContext(),
                    "${room.name} 정보 보기 클릭됨", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvRoomList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoomList.adapter = adapter

        loadRooms()
    }

    private fun loadRooms() {

        db.collection("buildings")
            .document(buildingId)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {
                    Log.d("ROOM_DEBUG", "Document not found: $buildingId")
                    return@addOnSuccessListener
                }

                // timetable 필드 Map 가져오기
                val timetableMap = doc.get("timetable") as? Map<String, Any>
                Log.d("ROOM_DEBUG", "Raw timetable: $timetableMap")

                if (timetableMap == null) {
                    Log.d("ROOM_DEBUG", "Timetable is NULL")
                    return@addOnSuccessListener
                }

                roomList.clear()

                for ((roomId, _) in timetableMap) {

                    val floor =
                        if (roomId.isNotEmpty()) roomId.first().toString() + "F" else "-"

                    val capacity = 30 // 기본값

                    roomList.add(
                        Room(
                            name = "$buildingName ${roomId}호",
                            capacity = capacity,
                            floor = floor
                        )
                    )
                }

                Log.d("ROOM_DEBUG", "Loaded rooms: ${roomList.size}")

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "강의실 정보를 불러올 수 없습니다: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


