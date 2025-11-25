package com.example.lendmark.ui.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.lendmark.R
import com.example.lendmark.data.model.Building
import com.example.lendmark.databinding.FragmentMyReservationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class MyReservationFragment : Fragment() {

    private var _binding: FragmentMyReservationBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    // Firestore에서 받아온 예약들 저장
    private var reservationList: List<ReservationFS> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyReservationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 필터 초기값
        binding.filterGroup.check(R.id.filterAll)
        binding.filterGroup.setOnCheckedChangeListener { _, _ ->
            displayReservations()
        }

        loadReservations()
    }

    /** Firestore에서 예약 정보 불러오기 */
    private fun loadReservations() {
        if (uid == null) return

        db.collection("reservations")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                reservationList = snapshot.documents.map { doc ->
                    ReservationFS(
                        id = doc.id,
                        buildingId = doc.getString("buildingId") ?: "",
                        roomId = doc.getString("roomId") ?: "",
                        date = doc.getString("date") ?: "",
                        day = doc.getString("day") ?: "",
                        periodStart = doc.getLong("periodStart")?.toInt() ?: 0,
                        periodEnd = doc.getLong("periodEnd")?.toInt() ?: 0,
                        attendees = doc.getLong("people")?.toInt() ?: 0,
                        purpose = doc.getString("purpose") ?: "",
                        status = doc.getString("status") ?: "approved"
                    )
                }

                displayReservations()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load reservations", Toast.LENGTH_SHORT).show()
            }
    }

    /** 상태 필터 후 카드 그리기 */
    private fun displayReservations() {
        val container = binding.reservationContainer
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        val filterId = binding.filterGroup.checkedChipId

        val filtered = when (filterId) {
            R.id.filterApproved -> reservationList.filter { it.status == "approved" }
            R.id.filterFinished -> reservationList.filter { it.status == "finished" || it.status == "canceled" }
            else -> reservationList
        }

        if (filtered.isEmpty()) {
            addEmptyMessage(container)
            return
        }

        filtered.forEach { reservation ->
            val card = inflater.inflate(R.layout.item_reservation, container, false)

            val tvBuildingRoom = card.findViewById<TextView>(R.id.tvBuildingRoom)
            val tvStatus = card.findViewById<TextView>(R.id.tvStatus)
            val tvDateTime = card.findViewById<TextView>(R.id.tvDateTime)
            val tvAttendees = card.findViewById<TextView>(R.id.tvAttendees)
            val tvPurpose = card.findViewById<TextView>(R.id.tvPurpose)

            val btnCancel = card.findViewById<MaterialButton>(R.id.btnCancel)
            val btnRegisterInfo = card.findViewById<MaterialButton>(R.id.btnRegisterInfo)

            // UI 매핑
            tvBuildingRoom.text = "${reservation.buildingId} ${reservation.roomId}"
            tvDateTime.text = "${reservation.date} • ${periodToTime(reservation.periodStart)} - ${periodToTime(reservation.periodEnd)}"
            tvAttendees.text = "Attendees: ${reservation.attendees}"
            tvPurpose.text = "Purpose: ${reservation.purpose}"
            tvStatus.text = reservation.status.replaceFirstChar { it.uppercase() }

            // 상태별 버튼 처리
            when (reservation.status) {
                "approved" -> {
                    btnRegisterInfo.visibility = View.GONE
                    btnCancel.visibility = View.VISIBLE
                }
                "finished" -> {
                    btnCancel.visibility = View.GONE
                    btnRegisterInfo.visibility = View.VISIBLE
                }
                "canceled" -> {
                    btnCancel.visibility = View.VISIBLE
                    btnCancel.isEnabled = false
                    btnCancel.text = "Cancelled"
                }
            }

            // 상세보기 다이얼로그 연결
            card.setOnClickListener {
                ReservationDetailDialogFS(
                    reservation = reservation,
                    onCancelClick = { updateStatus(reservation.id, "canceled") },
                    onRegisterClick = { updateStatus(reservation.id, "finished") }
                ).show(childFragmentManager, "ReservationDetailDialogFS")
            }

            // 취소 버튼
            btnCancel.setOnClickListener {
                updateStatus(reservation.id, "canceled")
            }

            // 정보 등록 버튼
            btnRegisterInfo.setOnClickListener {
                updateStatus(reservation.id, "finished")
            }

            container.addView(card)
        }
    }

    /** Firestore status 업데이트 */
    private fun updateStatus(id: String, newStatus: String) {
        db.collection("reservations").document(id)
            .update("status", newStatus)
            .addOnSuccessListener {
                loadReservations()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addEmptyMessage(container: LinearLayout) {
        val tv = TextView(requireContext()).apply {
            text = "No reservations found."
            setPadding(16, 32, 16, 32)
            setTextColor(requireContext().getColor(R.color.gray_dark))
        }
        container.addView(tv)
    }

    private fun periodToTime(period: Int): String {
        val hour = 8 + period
        return String.format("%02d:00", hour)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** Firestore에서 불러오는 Reservation 모델 */
data class ReservationFS(
    val id: String = "",
    val buildingId: String = "",
    val roomId: String = "",
    val date: String = "",
    val day: String = "",
    val periodStart: Int = 0,
    val periodEnd: Int = 0,
    val attendees: Int = 0,
    val purpose: String = "",
    val status: String = ""
)
