package com.example.mfl

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mfl.databinding.FragmentMapBinding
import com.example.mfl.model.Geofence
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.LinearRing
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polygon
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: DatabaseReference

    private val geofences = mutableListOf<Geofence>()
    private val geofencePoints = mutableListOf<Point>()
    private var isCreatingGeofence = false
    private var geofenceIdCounter = 0

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MapFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        mapView = binding.mapView
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        database = FirebaseDatabase.getInstance().reference.child("geofences")

        setupListeners()
        checkLocationPermission()

        return binding.root
    }

    private fun setupListeners() {
        binding.fabViewGeofences.setOnClickListener { showGeofenceListDialog() }
        binding.fabUpdateLocation.setOnClickListener { updateLocationOnMap() }
        binding.fabCreateGeofence.setOnClickListener { toggleGeofenceCreation() }

        setupMapTouchListener()
    }

    private fun setupMapTouchListener() {
        mapView.map.addInputListener(object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                Log.d(TAG, "onMapTap: Tapped at: ${point.latitude}, ${point.longitude}")

                if (isCreatingGeofence) {
                    geofencePoints.add(point)
                    Toast.makeText(requireContext(), "Точка добавлена: ${point.latitude}, ${point.longitude}", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Geofence point added: $point")
                    drawGeofence()
                } else {
                    Log.d(TAG, "Not in geofence creation mode")
                }
            }

            override fun onMapLongTap(map: Map, point: Point) {
                Log.d(TAG, "onMapLongTap: Long tap at: ${point.latitude}, ${point.longitude}")
            }
        })
    }

    private fun checkLocationPermission() {
        if (hasLocationPermission()) {
            updateLocationOnMap()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateLocationOnMap() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val currentLocation = Point(location.latitude, location.longitude)
                        val cameraPosition = CameraPosition(currentLocation, 14.0f, 0.0f, 0.0f)
                        mapView.map.move(cameraPosition)

                        // Добавляем маркер на текущую позицию пользователя
                        val userMarker = mapView.map.mapObjects.addPlacemark(currentLocation)
                        userMarker.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_my_location))

                        // Обновляем текст о местоположении
                        binding.tvLocationInfo.text = "Местоположение: ${location.latitude}, ${location.longitude}"
                        binding.progressBar.visibility = View.GONE // Скрыть прогресс-бар после загрузки
                        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Ошибка при получении местоположения", e)
                }
        } else {
            Log.w(TAG, "Разрешение на использование местоположения не предоставлено")
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showGeofenceListDialog() {
        if (geofences.isNotEmpty()) {
            val geofenceNames = geofences.map { it.name }.toTypedArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Выберите геозону")
                .setItems(geofenceNames) { _, which -> focusOnGeofence(which) }
                .setNegativeButton("Удалить геозону") { _, which -> deleteGeofence(which) }
                .show()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Нет геозон")
                .setMessage("Вы еще не создали ни одной геозоны.")
                .setPositiveButton("ОК", null)
                .show()
        }
    }

    private fun focusOnGeofence(index: Int) {
        val geofence = geofences[index]
        val center = geofence.points.first()
        mapView.map.move(CameraPosition(center, 14.0f, 0.0f, 0.0f))
    }

    private fun deleteGeofence(index: Int) {
        val geofence = geofences[index]
        database.child(geofence.id.toString()).removeValue()
        geofences.removeAt(index)
        redrawGeofences()
    }

    private fun toggleGeofenceCreation() {
        isCreatingGeofence = if (isCreatingGeofence) {
            saveGeofence()
            false
        } else {
            Toast.makeText(requireContext(), "Режим создания геозоны активирован", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Geofence creation mode activated")
            geofencePoints.clear()
            true
        }
    }

    private fun drawGeofence() {
        mapView.map.mapObjects.clear()  // Очищаем карту перед новой отрисовкой

        if (geofencePoints.size >= 3) {
            val polygon = Polygon(LinearRing(geofencePoints), emptyList())
            val mapPolygon = mapView.map.mapObjects.addPolygon(polygon)
            mapPolygon.strokeColor = Color.RED
            mapPolygon.fillColor = Color.argb(50, 255, 0, 0)
            Log.d(TAG, "Geofence drawn with ${geofencePoints.size} points")
        } else {
            Log.d(TAG, "Not enough points to draw geofence")
        }
    }

    private fun saveGeofence() {
        if (geofencePoints.size >= 3) {
            val newGeofence = Geofence(
                id = geofenceIdCounter++,
                points = geofencePoints.toList(),
                name = "Геозона $geofenceIdCounter"
            )
            geofences.add(newGeofence)
            database.child(newGeofence.id.toString()).setValue(newGeofence)
                .addOnSuccessListener {
                    Log.d(TAG, "Геозона успешно сохранена в Firebase")
                    Toast.makeText(requireContext(), "Геозона сохранена", Toast.LENGTH_SHORT).show()
                    geofencePoints.clear()
                    redrawGeofences()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Ошибка сохранения геозоны в Firebase", e)
                }
        } else {
            Toast.makeText(requireContext(), "Недостаточно точек для создания геозоны", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redrawGeofences() {
        mapView.map.mapObjects.clear()  // Очищаем карту перед отрисовкой всех геозон
        geofences.forEach { geofence ->
            val polygon = Polygon(LinearRing(geofence.points), emptyList())
            val mapPolygon = mapView.map.mapObjects.addPolygon(polygon)
            mapPolygon.strokeColor = Color.RED
            mapPolygon.fillColor = Color.argb(50, 255, 0, 0)
        }
    }

    private fun loadGeofences() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                geofences.clear()
                snapshot.children.mapNotNullTo(geofences) { it.getValue(Geofence::class.java) }
                redrawGeofences()
                Log.d(TAG, "Geofences loaded from Firebase: ${geofences.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Ошибка загрузки геозон", error.toException())
            }
        })
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
        loadGeofences()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
