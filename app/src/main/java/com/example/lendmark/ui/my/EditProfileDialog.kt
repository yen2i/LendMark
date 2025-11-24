package com.example.lendmark.ui.my

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.lendmark.databinding.DialogEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileDialog(
    private val majors: List<String>,
    private val onProfileUpdated: (() -> Unit)? = null   // 콜백 정상 선언
) : DialogFragment() {

    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditProfileBinding.inflate(LayoutInflater.from(context))

        setupMajorDropdown()
        loadExistingProfile()

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        return dialog
    }

    private fun loadExistingProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.tvEmail.text = doc.getString("email") ?: ""
                binding.actvMajor.setText(doc.getString("department") ?: "", false)
                binding.etPhoneNumber.setText(doc.getString("phone") ?: "")
            }
    }

    private fun setupMajorDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, majors)
        binding.actvMajor.setAdapter(adapter)
    }

    private fun saveProfile() {
        val selectedMajor = binding.actvMajor.text.toString()
        val newPhone = binding.etPhoneNumber.text.toString()

        if (selectedMajor.isEmpty()) {
            Toast.makeText(requireContext(), "Please select your major.", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return

        val updates = mapOf(
            "department" to selectedMajor,
            "phone" to newPhone
        )

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()

                // 부모(MyPageFragment)에게 UI 다시 로딩하라고 알림
                onProfileUpdated?.invoke()

                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update profile.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
