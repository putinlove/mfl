package com.example.mfl


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.mfl.databinding.FragmentRegistrationBinding
import com.example.mfl.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class RegistrationFragment : Fragment() {

    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("RegistrationFragment", "onViewCreated вызван")

        auth = FirebaseAuth.getInstance()

        // Настройка выбора роли
        val roles = arrayOf("Родитель", "Ребенок")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        binding.roleSpinner.adapter = adapter

        binding.sendCodeButton.setOnClickListener {
            val phoneNumber = binding.phoneInput.text.toString().trim()
            Log.d("RegistrationFragment", "Кнопка нажата. Номер телефона: $phoneNumber")
            if (isValidPhoneNumber(phoneNumber)) {
                sendVerificationCode(phoneNumber)
            } else {
                binding.phoneInput.error = "Введите корректный номер телефона"
            }
        }

        binding.verifyCodeButton.setOnClickListener {
            val code = binding.codeInput.text.toString().trim()
            if (code.isNotEmpty() && verificationId != null) {
                verifyCode(code)
            } else {
                binding.codeInput.error = "Введите код"
            }
        }
    }

    // Валидация номера телефона
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val regex = "^\\+7\\s\\d{3}\\s\\d{3}-\\d{2}-\\d{2}$".toRegex()
        return regex.matches(phoneNumber)
    }

    // Отправка кода подтверждения
    private fun sendVerificationCode(phoneNumber: String) {
        Log.d("RegistrationFragment", "Отправка кода на номер: $phoneNumber")

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // номер телефона для отправки кода
            .setTimeout(60L, TimeUnit.SECONDS) // время ожидания
            .setActivity(requireActivity())    // текущая активность
            .setCallbacks(verificationCallback) // коллбэки
            .build()

        try {
            PhoneAuthProvider.verifyPhoneNumber(options)
            Log.d("RegistrationFragment", "Код успешно отправлен.")
        } catch (e: Exception) {
            Log.e("RegistrationFragment", "Ошибка при отправке кода: ${e.message}")
        }
    }

    // Коллбэки для обработки проверки номера
    private val verificationCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            val code = credential.smsCode
            if (code != null) {
                verifyCode(code)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("RegistrationFragment", "Ошибка при отправке кода: ${e.message}")
            binding.phoneInput.error = "Ошибка при отправке кода: ${e.message}"
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            this@RegistrationFragment.verificationId = verificationId
            Log.d("RegistrationFragment", "Код отправлен. Verification ID: $verificationId")

            // Показать поле для ввода кода
            binding.codeInput.visibility = View.VISIBLE
            binding.verifyCodeButton.visibility = View.VISIBLE
        }
    }

    // Подтверждение кода
    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Успешная авторизация, сохранение данных пользователя
                    saveUserToFirestore()
                } else {
                    binding.codeInput.error = "Неверный код"
                }
            }
    }

    // Сохранение данных пользователя в Firestore
    private fun saveUserToFirestore() {
        val user = auth.currentUser
        val phoneNumber = user?.phoneNumber
        val firstName = binding.nameInput.text.toString().trim() // Получите значение имени
        val lastName = binding.surnameInput.text.toString().trim()   // Получите значение фамилии
        val role = binding.roleSpinner.selectedItem.toString() // Получите выбранную роль

        if (phoneNumber != null) {
            val db = FirebaseFirestore.getInstance()
            val userData = UserProfile(
                id = user.uid,
                phoneNumber = phoneNumber,
                firstName = firstName,
                lastName = lastName,
                role = role
            )

            db.collection("users").document(user.uid)
                .set(userData)
                .addOnSuccessListener {
                    Log.d("RegistrationFragment", "Данные пользователя успешно сохранены.")
                    navigateToMainScreen()
                }
                .addOnFailureListener { e ->
                    Log.e("RegistrationFragment", "Ошибка при сохранении данных: ${e.message}")
                    binding.phoneInput.error = "Ошибка при сохранении данных: ${e.message}"
                }
        }
    }


    // Переход на главный экран
    private fun navigateToMainScreen() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, MapFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
