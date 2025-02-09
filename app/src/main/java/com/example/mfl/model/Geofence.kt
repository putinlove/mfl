package com.example.mfl.model

import com.yandex.mapkit.geometry.Point

data class Geofence(
    val id: Int,
    val childId: Int,
    val parentId: Int,
    val points: List<Point>, // Список точек, определяющих геозону
    val name: String,
    val latitude:Float,
    val longitude:Float,
)
