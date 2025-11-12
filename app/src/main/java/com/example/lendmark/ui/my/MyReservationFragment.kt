package com.example.lendmark.ui.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lendmark.R
import com.example.lendmark.databinding.FragmentMyReservationBinding

// 예약 데이터 클래스
data class Reservation(
    val id: Int,
    val building: String,
    val room: String,
    val date: String,
    val time: String,
    val attendees: Int,
    val purpose: String,
    var status: String, // Approved, Pending, Finished, Cancelled
    var isCancelled: Boolean = false
)

class MyReservationFragment : Fragment() {

    private var _binding: FragmentMyReservationBinding? = null
    private val binding get() = _binding!!

    //  더미 예약 데이터
    private val reservations = mutableListOf(
        Reservation(1, "Frontier Hall", "Room 107", "2025-10-15", "13:00 - 15:00", 5, "Team project", "Approved"),
        Reservation(2, "Eoui Hall", "Room 201", "2025-10-20", "16:00 - 18:00", 8, "Club meeting", "Pending"),
        Reservation(3, "Dasan Hall", "Room 301", "2025-10-10", "10:00 - 12:00", 3, "Study", "Finished")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyReservationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 기본 필터: All
        binding.filterGroup.check(R.id.filterAll)
        updateUI()

        // 필터 변경 시
        binding.filterGroup.setOnCheckedChangeListener { _, _ -> updateUI() }

        // 카드 클릭 시 상세 다이얼로그 열기
        binding.cardApproved.setOnClickListener { reservations.find { it.id == 1 }?.let { openDetailDialog(it) } }
        binding.cardPending.setOnClickListener { reservations.find { it.id == 2 }?.let { openDetailDialog(it) } }
        binding.cardFinished.setOnClickListener { reservations.find { it.id == 3 }?.let { openDetailDialog(it) } }

        // 취소 버튼 클릭 이벤트
        binding.btnCancelApproved.setOnClickListener { showCancelConfirmationDialog(1) }
        binding.btnCancelPending.setOnClickListener { showCancelConfirmationDialog(2) }
    }

    // 상세 다이얼로그 열기
    private fun openDetailDialog(reservation: Reservation) {
        val dialog = ReservationDetailDialog(reservation) { reservationId ->
            showCancelConfirmationDialog(reservationId)
        }
        dialog.show(childFragmentManager, "ReservationDetailDialog")
    }

    // 취소 확인 다이얼로그
    private fun showCancelConfirmationDialog(reservationId: Int) {
        val onConfirm = {
            reservations.find { it.id == reservationId }?.apply {
                status = "Cancelled"   // 취소 시 Finished가 아닌 Cancelled로 설정
                isCancelled = true
            }
            updateUI()
        }
        ConfirmCancelDialog(onConfirm).show(parentFragmentManager, "ConfirmCancelDialog")
    }

    //  UI 업데이트 (필터 및 상태별 표시)
    private fun updateUI() {
        val checkedId = binding.filterGroup.checkedChipId
        val showAll = checkedId == R.id.filterAll || checkedId == View.NO_ID

        reservations.forEach { res ->
            val cardBinding = when (res.id) {
                1 -> Triple(binding.cardApproved, binding.tvStatusApproved, binding.btnCancelApproved)
                2 -> Triple(binding.cardPending, binding.tvStatusPending, binding.btnCancelPending)
                3 -> Triple(binding.cardFinished, binding.tvStatusFinished, binding.btnRegisterInfo)
                else -> null
            }

            cardBinding?.let { (card, statusView, button) ->
                val filterStatus = getFilterStatus(checkedId)

                // “Cancelled” 예약도 Finished 필터에 포함
                val isMatched =
                    showAll ||
                            res.status.equals(filterStatus, true) ||
                            (filterStatus == "Finished" && res.status == "Cancelled")

                card.visibility = if (isMatched) View.VISIBLE else View.GONE

                updateCardStatus(res, statusView as TextView)

                //  버튼 상태 갱신 (Finished 카드 제외)
                if (button is com.google.android.material.button.MaterialButton && res.id != 3) {
                    updateButtonState(res, button)
                }
            }
        }
    }

    // 상태 뱃지 스타일 변경
    private fun updateCardStatus(reservation: Reservation, statusView: TextView) {
        statusView.text = reservation.status
        when (reservation.status) {
            "Approved" -> {
                statusView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_green)
                statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
            "Pending" -> {
                statusView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_yellow)
                statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
            "Finished" -> {
                statusView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_gray)
                statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
            }
            "Cancelled" -> {
                statusView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_gray)
                statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            }
        }
    }

    // 취소 버튼 상태 제어
    private fun updateButtonState(reservation: Reservation, button: com.google.android.material.button.MaterialButton) {
        if (reservation.isCancelled || reservation.status == "Cancelled") {
            button.text = "Cancelled"
            button.isEnabled = false
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.gray)
        } else {
            button.text = "Cancel Reservation"
            button.isEnabled = true
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.red)
        }
    }

    //  필터 상태 반환
    private fun getFilterStatus(checkedId: Int): String {
        return when (checkedId) {
            R.id.filterPending -> "Pending"
            R.id.filterApproved -> "Approved"
            R.id.filterFinished -> "Finished"
            else -> "" // All
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
