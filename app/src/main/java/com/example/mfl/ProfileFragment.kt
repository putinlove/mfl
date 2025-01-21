package com.example.mfl

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mfl.databinding.FragmentProfileBinding
import com.example.mfl.model.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var isDataChanged = false // Флаг для отслеживания изменений

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("ProfileFragment", "onCreateView вызван")
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ProfileFragment", "onViewCreated вызван")

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Логика загрузки данных профиля
        loadUserProfile()

        // Добавляем TextWatcher для отслеживания изменений
        binding.editTextName.addTextChangedListener(createTextWatcher())
        binding.editTextPhone.addTextChangedListener(createTextWatcher())

        // Логика для кнопки сохранения
        binding.buttonSave.setOnClickListener {
            Log.d("ProfileFragment", "Кнопка сохранения нажата")
            saveUserProfile()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showSnackbar("Пользователь не найден")
            Log.d("ProfileFragment", "Текущий пользователь не найден")
            return
        }

        val userId = currentUser.uid
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        binding.editTextName.setText(user.firstName)
                        binding.editTextPhone.setText(user.phoneNumber)
                        isDataChanged = false // Сбрасываем флаг при загрузке данных
                        Log.d("ProfileFragment", "Данные профиля загружены: ${user.firstName}, ${user.phoneNumber}")
                    } else {
                        showSnackbar("Профиль не найден. Пожалуйста, заполните поля.")
                        Log.d("ProfileFragment", "Документ профиля пуст")
                    }
                }
            }
            .addOnFailureListener { e ->
                showSnackbar("Ошибка загрузки профиля: ${e.message}")
                Log.d("ProfileFragment", "Ошибка загрузки профиля: ${e.message}")
            }
    }

    private fun saveUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showSnackbar("Пользователь не найден")
            Log.d("ProfileFragment", "Текущий пользователь не найден")
            return
        }

        val userId = currentUser.uid
        val firstName = binding.editTextName.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()

        if (firstName.isEmpty() || phone.isEmpty()) {
            showSnackbar("Заполните все поля перед сохранением")
            Log.d("ProfileFragment", "Некоторые поля пустые")
            return
        }

        val user = User(
            id = userId,
            firstName = firstName,
            lastName = "",  // Можно добавить поле для фамилии в UI и тут
            phoneNumber = phone,
            role = "", // Нужно добавить логику для роли, если это необходимо
            location = GeoPoint(0.0, 0.0) // Добавьте логику для получения геопозиции
        )

        firestore.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                if (isDataChanged) {
                    showSnackbar("Данные успешно сохранены")
                    isDataChanged = false // Сбрасываем флаг после успешного сохранения
                }
                Log.d("ProfileFragment", "Данные профиля сохранены успешно")
            }
            .addOnFailureListener { e ->
                showSnackbar("Ошибка сохранения данных: ${e.message}")
                Log.d("ProfileFragment", "Ошибка сохранения данных: ${e.message}")
            }
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isDataChanged = true // Устанавливаем флаг при изменении текста
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }

    private fun showSnackbar(message: String) {
        if (isAdded && view != null) {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
            Log.d("ProfileFragment", "Snackbar показан: $message")
        } else {
            Log.d("ProfileFragment", "Snackbar не может быть показан: $message")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Освобождение ресурсов binding
        Log.d("ProfileFragment", "onDestroyView вызван")
    }
}
