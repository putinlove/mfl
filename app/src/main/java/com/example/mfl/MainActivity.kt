package com.example.mfl

import TaskFragment
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mfl.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import com.yandex.mapkit.MapKitFactory  // Импортируйте MapKitFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private var doubleBackToExitPressedOnce = false //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация Firebase
        FirebaseApp.initializeApp(this)

        // Инициализация MapKit с API ключом
        MapKitFactory.setApiKey("a486b711-6a63-471b-998d-7ce9f4fb9a55")
        MapKitFactory.initialize(this)  // Здесь инициализация MapKit

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    openFragment(MapFragment())  // Переход на экран карты
                    true
                }
                R.id.nav_tasks -> {
                    openFragment(TaskFragment())  // Переход на экран задач
                    true
                }
                R.id.nav_geofence -> {
                   openFragment(UserFragment())  // Переход на экран chat
                    true
                 }
                R.id.nav_profile -> {
                    openFragment(ProfileFragment())  // Переход на экран профиля
                    true
                }
                else -> false
            }
        }

        // Проверка первого запуска
        if (isFirstLaunch()) {
            openFragment(RegistrationFragment())  // Первый запуск - открываем фрагмент регистрации
            saveLaunchState()  // Сохраняем состояние, чтобы не было повторного запуска
        } else {
            if (auth.currentUser != null) {
                openFragment(MapFragment())  // Если пользователь авторизован, открываем карту
            } else {
                openFragment(LoginFragment())  // Иначе - фрагмент авторизации
            }
        }
    }

    // Проверка, первый ли это запуск приложения
    private fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean("isFirstLaunch", true)
    }

    // Сохранение состояния, что приложение было запущено
    private fun saveLaunchState() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isFirstLaunch", false)
        editor.apply()
    }

    // Метод для открытия фрагмента с проверкой, что фрагмент не активен
    private fun openFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.fragment_container)

        // Проверяем, не является ли новый фрагмент текущим, чтобы избежать повторной замены
        if (currentFragment == null || currentFragment.javaClass != fragment.javaClass) {
            Log.d("Main", "Кнопка нажата. Номер телефона:")
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        } else {
            Log.d("Main", "Кнопка ne нажата. Номер телефона:")
        }
    }

    // Меню с кнопкой выхода и другими элементами
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logoutUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logoutUser() {
        if (doubleBackToExitPressedOnce) {
            // Если кнопка была нажата дважды, выходим из приложения
            FirebaseAuth.getInstance().signOut() // Пример выхода из Firebase
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegistrationFragment()) // Переход на экран регистрации
                .addToBackStack(null)
                .commit()
            return
        }

        this.doubleBackToExitPressedOnce = true
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment()) // Переход на экран авторизации
            .addToBackStack(null)
            .commit()

        // Сбрасываем флаг через 2 секунды
        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }





    // Остановка MapKit в onStop()
    override fun onStop() {
        super.onStop()
        MapKitFactory.getInstance().onStop()
    }
}
