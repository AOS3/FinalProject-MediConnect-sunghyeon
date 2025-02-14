package com.teammeditalk.medicalconnect.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.teammeditalk.medicalconnect.R
import com.teammeditalk.medicalconnect.databinding.ActivityMapBinding
import timber.log.Timber

class MapActivity : AppCompatActivity() {
    private lateinit var startPosition: LatLng
    private lateinit var centerLabel: Label
    private var kakaoMap: KakaoMap? = null
    private lateinit var binding: ActivityMapBinding
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 요청할 위치 권한 목록입니다.
    private val locationPermissions =
        arrayOf<String>(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

    // 권한 확인
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                locationPermissions[0],
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                locationPermissions[1],
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Timber.d("권한 있음")
        } else {
            ActivityCompat.requestPermissions(
                this,
                locationPermissions,
                1001,
            )
        }
    }

    // 권한 요청 다이얼로그에 응답하면 이 메소드가 실행된다.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            Timber.d("권한 있음")
        } else {
            Timber.d("권한 없음")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        // 여기를 바인딩으로 안바꿔줘서 라이브러리가 적용 안된 거였다!!
        setContentView(binding.root)
        checkLocationPermission()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {
                    // 지도 api가 정상적으로 종료될 때 호출
                }

                override fun onMapError(e: Exception?) {
                    // 인증 실패 및 지도 사용 중 에러 발생
                    e?.printStackTrace()
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    getCurrentLocation()
                }
            },
        )
    }

    private fun getCurrentLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lifecycleScope.let {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    it?.let {
                        val currentLatLng = LatLng.from(it.latitude, it.longitude)
                        kakaoMap?.let { map ->

                            // 카메라 이동

                            map.moveCamera(CameraUpdateFactory.newCenterPosition(currentLatLng, 20))

                            // 1. LabelStyles 생성하기
                            val style =
                                LabelStyles.from(
                                    LabelStyle.from(R.drawable.location),
                                )

                            val markerStyles =
                                map.labelManager?.addLabelStyles(
                                    style,
                                )

                            Timber.d("마커 스타일 :$style")

                            // LabelOptions 생성하기
                            val options =
                                LabelOptions
                                    .from(LatLng.from(it.latitude, it.longitude))
                                    .setStyles(markerStyles)

                            val layer = kakaoMap?.labelManager?.layer

                            // LabelLayer 가져오기
                            val label = layer?.addLabel(options)

                            Timber.d("layer : ${layer}\n label : $label")
                        }
                    }
                }
            }
        }
    }
}
