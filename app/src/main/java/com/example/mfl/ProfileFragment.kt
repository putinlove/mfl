package com.example.mfl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mfl.databinding.FragmentProfileBinding
import com.example.mfl.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var selectedRole: String = "Родитель" // Значение по умолчанию

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Инициализация ViewBinding
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Инициализация спиннера
        val roles = resources.getStringArray(R.array.roles_array)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRole.adapter = adapter

        // Обработка выбора роли
        binding.spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRole = roles[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Логика загрузки данных профиля
        loadUserProfile()

        // Логика для кнопки сохранения
        binding.buttonSave.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        currentUser?.let {
            val userId = it.uid
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userProfile = document.toObject(UserProfile::class.java)
                        userProfile?.let { profile ->
                            binding.editTextName.setText(profile.firstName)
                            binding.editTextPhone.setText(profile.phoneNumber)
                            // Установить роль в спиннере
                            val roleIndex = resources.getStringArray(R.array.roles_array).indexOf(profile.role)
                            binding.spinnerRole.setSelection(roleIndex)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Профиль не найден", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Ошибка загрузки профиля: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserProfile() {
        val currentUser = auth.currentUser
        currentUser?.let {
            val userId = it.uid
            val name = binding.editTextName.text.toString()
            val phone = binding.editTextPhone.text.toString()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
            } else {
                val userProfile = UserProfile(
                    id = userId,
                    firstName = name,
                    phoneNumber = phone,
                    role = selectedRole
                )

                firestore.collection("users").document(userId).set(userProfile)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Данные успешно сохранены", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Ошибка сохранения данных: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Освобождение ресурсов binding
    }
}
