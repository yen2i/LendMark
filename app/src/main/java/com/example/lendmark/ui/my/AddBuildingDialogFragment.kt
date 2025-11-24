package com.example.lendmark.ui.my

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.lendmark.R
import com.example.lendmark.data.model.Building

class AddBuildingDialogFragment : DialogFragment() {

    var onBuildingSelected: ((Building) -> Unit)? = null

    private val candidateBuildings = mutableListOf<Building>()

    fun setCandidateBuildings(buildings: List<Building>) {
        candidateBuildings.clear()
        candidateBuildings.addAll(buildings)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_add_building, null)

        val listContainer = view.findViewById<LinearLayout>(R.id.layoutAddBuildingList)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)

        btnClose.setOnClickListener { dismiss() }

        candidateBuildings.forEach { building ->
            val itemView = inflater.inflate(
                R.layout.item_add_building,
                listContainer,
                false
            )

            val tvName = itemView.findViewById<TextView>(R.id.tvBuildingName)
            val tvRooms = itemView.findViewById<TextView>(R.id.tvBuildingRooms)
            val btnAdd = itemView.findViewById<ImageButton>(R.id.btnAdd)

            tvName.text = building.name
            tvRooms.text = "${building.roomCount} rooms"

            btnAdd.setOnClickListener {
                onBuildingSelected?.invoke(building)
                dismiss()
            }

            listContainer.addView(itemView)
        }

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(), // Set width to 90% of screen width
            (resources.displayMetrics.heightPixels * 0.8).toInt()  // Set height to 80% of screen height
        )
    }
}
