package com.example.lendmark.ui.notification

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.lendmark.databinding.DialogNotificationDetailBinding
import com.example.lendmark.ui.my.ReservationDetailDialogFS
import com.example.lendmark.ui.my.ReservationFS
import com.google.firebase.firestore.FirebaseFirestore

class NotificationDetailDialog(
    private val item: NotificationItem
) : DialogFragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogNotificationDetailBinding.inflate(LayoutInflater.from(context))

        // 제목과 간단 정보 표시
        binding.tvTitle.text = item.title
        binding.tvDetail.text = "Reservation at: ${item.location} (${item.startTime} - ${item.endTime})"

        // "Go to Reservation Details" 버튼 클릭 시
        binding.btnGoReservation.setOnClickListener {
            dismiss()
            loadReservationDetails(reservationId = item.reservationId)
        }

        // 닫기 버튼
        binding.btnConfirm.setOnClickListener {
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    /** Firestore에서 실제 예약 정보 가져오기 */
    private fun loadReservationDetails(reservationId: String) {
        db.collection("reservations").document(reservationId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Reservation no longer exists.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Firestore → ReservationFS 매핑
                val reservation = ReservationFS(
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

                // Firestore 기반 상세 다이얼로그 열기
                val dialog = ReservationDetailDialogFS(
                    reservation = reservation,
                    onCancelClick = { updateStatus(reservation.id, "canceled") },
                    onRegisterClick = { updateStatus(reservation.id, "finished") }
                )

                dialog.show(parentFragmentManager, "ReservationDetailDialogFS")
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load reservation.", Toast.LENGTH_SHORT).show()
            }
    }

    /** Firestore 상태 업데이트 */
    private fun updateStatus(id: String, newStatus: String) {
        db.collection("reservations").document(id)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Status updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }
}
