package com.example.lendmark.ui.my

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.lendmark.databinding.FragmentMyInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyInfoFragment : Fragment() {

    private var _binding: FragmentMyInfoBinding? = null
    private val binding get() = _binding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyInfoBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserInfo()
    }

    private fun loadUserInfo() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->

                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (document != null) {
                    binding?.valueName?.text = document.getString("name")
                    binding?.valueMajor?.text = document.getString("department")
                    binding?.valueEmail?.text = document.getString("email")
                    binding?.valuePhone?.text = document.getString("phone")

                    // 예약 컬렉션 읽기
                    db.collection("users").document(uid)
                        .collection("reservations").get()
                        .addOnSuccessListener { reservations ->

                            if (!isAdded || _binding == null) return@addOnSuccessListener

                            binding?.valueTotalReservation?.text = "${reservations.size()}회"
                        }
                        .addOnFailureListener { exception ->
                            if (!isAdded || _binding == null) return@addOnFailureListener

                            Log.d("MyInfoFragment", "Error getting reservations: ", exception)
                            binding?.valueTotalReservation?.text = "0회"
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.d("MyInfoFragment", "get failed with ", exception)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
