package com.example.lendmark.ui.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.lendmark.R
import com.example.lendmark.data.model.Building
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ManageFavoritesFragment : Fragment() {

    private var isEditMode = false
    private val selectedToDelete = mutableSetOf<String>()

    private lateinit var favoriteContainer: LinearLayout
    private lateinit var allContainer: LinearLayout
    private lateinit var btnAddBuilding: MaterialButton
    private lateinit var editModeButtons: LinearLayout
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var tvEdit: TextView

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val allBuildings = mutableListOf<Building>()
    private val favoriteBuildings = mutableListOf<Building>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_manage_favorites, container, false)

        favoriteContainer = view.findViewById(R.id.layoutFavoriteBuildings)
        allContainer = view.findViewById(R.id.layoutAllBuildings)
        btnAddBuilding = view.findViewById(R.id.btnAddBuilding)
        editModeButtons = view.findViewById(R.id.editModeButtons)
        btnCancel = view.findViewById(R.id.btnCancelEdit)
        btnDelete = view.findViewById(R.id.btnDeleteFavorites)
        tvEdit = view.findViewById(R.id.tvEditFavorites)

        loadDataFromFirestore()

        tvEdit.setOnClickListener { toggleEditMode() }
        btnCancel.setOnClickListener { toggleEditMode() } // Also cancels
        btnDelete.setOnClickListener { deleteSelectedFavorites() }

        btnAddBuilding.setOnClickListener {
            showAddBuildingDialog()
        }

        return view
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        selectedToDelete.clear()
        updateUiForEditMode()
        renderFavorites() // Re-render to show/hide checkboxes
    }

    private fun updateUiForEditMode() {
        if (isEditMode) {
            editModeButtons.visibility = View.VISIBLE
            btnAddBuilding.visibility = View.GONE
            tvEdit.text = "Done"
        } else {
            editModeButtons.visibility = View.GONE
            btnAddBuilding.visibility = View.VISIBLE
            tvEdit.text = "Edit"
        }
        updateDeleteButtonState()
    }

    private fun updateDeleteButtonState() {
        btnDelete.text = "Delete (${selectedToDelete.size})"
        btnDelete.isEnabled = selectedToDelete.isNotEmpty()
    }

    private fun deleteSelectedFavorites() {
        if (userId == null || selectedToDelete.isEmpty()) return

        db.collection("users").document(userId).update("favorites", FieldValue.arrayRemove(*selectedToDelete.toTypedArray()))
            .addOnSuccessListener {
                Toast.makeText(context, "Favorites removed", Toast.LENGTH_SHORT).show()
                toggleEditMode() // Exit edit mode
                loadDataFromFirestore() // Refresh the data
            }
            .addOnFailureListener { 
                Toast.makeText(context, "Error removing favorites", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadDataFromFirestore() {
        if (userId == null) return

        val userRef = db.collection("users").document(userId)
        val buildingsRef = db.collection("buildings")

        buildingsRef.get().addOnSuccessListener { buildingsSnapshot ->
            allBuildings.clear()
            val buildings = buildingsSnapshot.toObjects(Building::class.java)
            allBuildings.addAll(buildings.mapIndexed { index, building -> building.apply { id = buildingsSnapshot.documents[index].id } })
            allBuildings.sortBy { it.name }

            userRef.get().addOnSuccessListener { userDoc ->
                val favoriteBuildingIds = userDoc.get("favorites") as? List<String> ?: emptyList()
                
                favoriteBuildings.clear()
                favoriteBuildings.addAll(allBuildings.filter { favoriteBuildingIds.contains(it.id) })

                renderFavorites()
                renderAllBuildings()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to load buildings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderFavorites() {
        favoriteContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        if (favoriteBuildings.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "Please add favorite buildings."
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
            }
            favoriteContainer.addView(tv)
            tvEdit.visibility = View.GONE // Hide edit if no favorites
            return
        }
        tvEdit.visibility = View.VISIBLE

        favoriteBuildings.forEach { building ->
            val itemView = inflater.inflate(R.layout.item_favorite_building, favoriteContainer, false)
            val checkBox = itemView.findViewById<CheckBox>(R.id.cbFavorite)
            itemView.findViewById<TextView>(R.id.tvFavoriteName).text = building.name
            itemView.findViewById<TextView>(R.id.tvFavoriteRooms).text = "${building.roomCount} classrooms"

            if (isEditMode) {
                checkBox.visibility = View.VISIBLE
                checkBox.isChecked = selectedToDelete.contains(building.id)
                itemView.setOnClickListener {
                    checkBox.isChecked = !checkBox.isChecked
                }
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedToDelete.add(building.id)
                    } else {
                        selectedToDelete.remove(building.id)
                    }
                    updateDeleteButtonState()
                }
            } else {
                checkBox.visibility = View.GONE
            }

            favoriteContainer.addView(itemView)
        }
    }

    private fun renderAllBuildings() {
        allContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        allBuildings.forEach { building ->
            val itemView = inflater.inflate(R.layout.item_all_building, allContainer, false)

            itemView.findViewById<TextView>(R.id.tvAllName).text = building.name
            itemView.findViewById<TextView>(R.id.tvAllRooms).text = "${building.roomCount} classrooms"

            val tvStar = itemView.findViewById<TextView>(R.id.tvAllStar)
            tvStar.visibility = if (favoriteBuildings.any { it.id == building.id }) View.VISIBLE else View.INVISIBLE

            allContainer.addView(itemView)
        }
    }

    private fun showAddBuildingDialog() {
        val dialog = AddBuildingDialogFragment()

        val candidates = allBuildings.filter { b -> favoriteBuildings.none { it.id == b.id } }

        dialog.setCandidateBuildings(candidates)
        dialog.onBuildingSelected = { selected ->
            if (userId != null) {
                db.collection("users").document(userId).update("favorites", FieldValue.arrayUnion(selected.id))
                    .addOnSuccessListener { loadDataFromFirestore() } // Reload on success
            }
        }

        dialog.show(parentFragmentManager, "AddBuildingDialog")
    }
}
