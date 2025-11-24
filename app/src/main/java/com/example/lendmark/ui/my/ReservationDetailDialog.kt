package com.example.lendmark.ui.my

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.lendmark.R
import com.example.lendmark.databinding.DialogReservationDetailBinding

class ReservationDetailDialog(
    private val reservation: Reservation,
    private val onCancelClick: (Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogReservationDetailBinding.inflate(LayoutInflater.from(context))

        // Set reservation info
        binding.tvBuildingName.text = reservation.building
        binding.tvRoomName.text = reservation.room
        binding.tvDate.text = reservation.date
        binding.tvTime.text = reservation.time
        binding.tvAttendees.text = "${reservation.attendees} people"
        binding.tvPurpose.text = reservation.purpose

        // Control button visibility and state based on status and cancellation flag
        when {
            // Case 1: Reservation was cancelled
            reservation.isCancelled -> {
                binding.btnRegisterInfo.visibility = View.GONE
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnCancel.text = "Cancelled"
                binding.btnCancel.isEnabled = false
                binding.btnCancel.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
                binding.btnCancel.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.gray)
            }
            // Case 2: Reservation is finished (and not cancelled)
            reservation.status == "Finished" -> {
                binding.btnCancel.visibility = View.GONE
                binding.btnRegisterInfo.visibility = View.VISIBLE
            }
            // Case 3: Reservation is active (Approved)
            else -> {
                binding.btnRegisterInfo.visibility = View.GONE
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnCancel.text = "Cancel Reservation"
                binding.btnCancel.isEnabled = true
                binding.btnCancel.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                binding.btnCancel.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.red)
            }
        }

        // Set click listener for the active cancel button
        binding.btnCancel.setOnClickListener {
            if (it.isEnabled) { // Only trigger if the button is enabled
                onCancelClick(reservation.id)
                dismiss()
            }
        }

        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        val dialog = AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
}
