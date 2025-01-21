package com.example.mfl.model

import com.google.firebase.firestore.GeoPoint

data class User(
    val id: String = "",               // Уникальный идентификатор пользователя
    val firstName: String = "",         // Имя пользователя
    val lastName: String = "",          // Фамилия пользователя
    val phoneNumber: String = "",       // Номер телефона
    val role: String = "",              // Роль (родитель или ребенок)
    val location: GeoPoint? = null      // Текущее местоположение (используя GeoPoint Firebase для хранения координат)
)
