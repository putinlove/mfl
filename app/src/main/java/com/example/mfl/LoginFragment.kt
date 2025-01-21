package com.example.mfl


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mfl.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Обработка нажатия кнопки отправки кода
        binding.loginButton.setOnClickListener {
            val phoneNumber = binding.phoneInput.text.toString().trim()
            if (isValidPhoneNumber(phoneNumber)) {
                Log.d("LoginFragment", "Кнопка нажата. Номер телефона: $phoneNumber")
                sendVerificationCode(phoneNumber)
            } else {
                Log.d("LoginFragment", "Неверный формат номера телефона")
                binding.phoneInput.error = "Введите корректный номер телефона"
            }
        }

        // Обработка нажатия кнопки подтверждения кода
        binding.confirmCodeButton.setOnClickListener {
            val code = binding.codeInput.text.toString().trim()
            if (code.isNotEmpty() && verificationId != null) {
                Log.d("LoginFragment", "Код для подтверждения: $code")
                verifyCode(code)
            } else {
                Log.d("LoginFragment", "Код не введен или verificationId пуст")
                binding.codeInput.error = "Введите код"
            }
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val regex = "^\\+7\\s\\d{3}\\s\\d{3}-\\d{2}-\\d{2}$".toRegex()
        return regex.matches(phoneNumber)
    }

    private fun sendVerificationCode(phoneNumber: String) {
        Log.d("LoginFragment", "Отправка кода на номер: $phoneNumber") // Лог перед отправкой кода

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(verificationCallback)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

        Log.d("LoginFragment", "Запущен процесс верификации номера") // Лог после запуска отправки
    }

    private val verificationCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Получили код автоматически
            Log.d("LoginFragment", "onVerificationCompleted вызван")
            val code = credential.smsCode
            if (code != null) {
                Log.d("LoginFragment", "Автоматически получен код: $code")
                verifyCode(code)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // Обработка ошибки
            Log.d("LoginFragment", "onVerificationFailed вызван. Ошибка: ${e.message}")
            binding.phoneInput.error = "Ошибка при отправке кода: ${e.message}"
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // Код был отправлен
            Log.d("LoginFragment", "onCodeSent вызван. Код отправлен, verificationId: $verificationId")
            this@LoginFragment.verificationId = verificationId

            // Отображаем поле для ввода кода и кнопку
            binding.codeLabel.visibility = View.VISIBLE
            binding.codeInput.visibility = View.VISIBLE
            binding.confirmCodeButton.visibility = View.VISIBLE
        }
    }




    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LoginFragment", "Пользователь успешно вошел")

                    // Проверка, есть ли пользователь в базе данных Firestore
                    checkUserInFirestore()
                } else {
                    Log.d("LoginFragment", "Ошибка при подтверждении кода")
                    binding.codeInput.error = "Неверный код"
                }
            }
    }

    private fun checkUserInFirestore() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Log.d("LoginFragment", "Пользователь существует в Firestore")
                        navigateToMap()
                    } else {
                        // Если пользователь не найден в базе данных
                        Toast.makeText(requireContext(), "Пользователь не зарегистрирован", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("LoginFragment", "Ошибка при проверке пользователя в Firestore: ${e.message}")
                }
        }
    }


    private fun navigateToMap() {
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

