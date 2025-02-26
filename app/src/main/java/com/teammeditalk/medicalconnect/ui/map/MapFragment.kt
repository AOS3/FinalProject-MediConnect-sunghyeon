package com.teammeditalk.medicalconnect.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.teammeditalk.medicalconnect.R
import com.teammeditalk.medicalconnect.base.BaseFragment
import com.teammeditalk.medicalconnect.data.model.search.SearchLocationItem
import com.teammeditalk.medicalconnect.databinding.FragmentMapBinding
import com.teammeditalk.medicalconnect.ui.ExcelHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MapFragment :
    BaseFragment<FragmentMapBinding>(
        FragmentMapBinding::inflate,
    ) {
    private var kakaoMap: KakaoMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude = 0.0
    private var longitude = 0.0
    private val excelHelper by lazy { ExcelHelper(requireContext()) }
    private val viewModel: MapViewModel by viewModels()
    private var hospitalLabelList: MutableList<Label> = mutableListOf()
    private var pharLabelList: MutableList<Label> = mutableListOf()
    private var layer: LabelLayer? = null

    // 요청할 위치 권한 목록입니다.
    private val locationPermissions =
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

    // 권한 확인
    private fun checkLocationPermission() {
        if (checkSelfPermission(
                requireContext(),
                locationPermissions[0],
            ) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                requireContext(),
                locationPermissions[1],
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Timber.d("권한 있음")
        } else {
            Timber.d("권한 없음")
            ActivityCompat.requestPermissions(
                requireActivity(),
                locationPermissions,
                1001,
            )
        }
    }

    // 권한 요청 다이얼로그에 응답하면 이 메소드가 실행된다.
    @Deprecated("Deprecated in Java")
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        checkLocationPermission()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // 여기를 바인딩으로 안바꿔줘서 라이브러리가 적용 안된 거였다!!
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        with(binding) {
            btnIsOpen.setOnClickListener {
                it.isSelected = !it.isSelected
                if (it.isSelected) {
                    btnIsOpen.setTextColor(getColor(requireContext(), R.color.white))
                } else {
                    btnIsOpen.setTextColor(getColor(requireContext(), R.color.gray70))
                }
            }
            btnLang.setOnClickListener {
                it.isSelected = !it.isSelected
                if (it.isSelected) {
                    btnLang.setTextColor(getColor(requireContext(), R.color.white))
                } else {
                    btnLang.setTextColor(getColor(requireContext(), R.color.gray70))
                }
            }

            btnHospital.setOnClickListener {
                it.isSelected = !it.isSelected
                if (hospitalLabelList.isEmpty() and it.isSelected) {
                    viewModel.searchHospitalByKeyword(latitude.toString(), longitude.toString(), 1)
                    getHospitalNearByMe()
                } else if (!it.isSelected) {
                    removeHospitalLabel()
                }
            }
            btnPill.setOnClickListener {
                it.isSelected = !it.isSelected
                if (pharLabelList.isEmpty() and it.isSelected) {
                    viewModel.searchPharmacyLocation(longitude.toString(), latitude.toString(), 1)
                    getPharmacyNearByMe()
                } else if (!it.isSelected) {
                    removePharLabel()
                }
            }

            mapView.start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() {
                        // 지도 api가 정상적으로 종료될 때 호출
                    }

                    override fun onMapError(e: Exception?) {
                        // 인증 실패 및 지도 사용 중 에러 발생
                        e?.printStackTrace()
                        Timber.e("지도 에러 :${e?.message}")
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(map: KakaoMap) {
                        kakaoMap = map
                        getCurrentLocation()
                        onLabelClickListener()
                    }
                },
            )
        }
    }

    // https://apis.map.kakao.com/android_v2/reference/com/kakao/vectormap/KakaoMap.OnLabelClickListener.html
    private fun onLabelClickListener() {
        kakaoMap?.setOnLabelClickListener { kakaoMap, labelLayer, label ->
            val bottomSheet = MapInfoBottomSheet(label.tag as SearchLocationItem)
            bottomSheet.show(parentFragmentManager, "dialog")
            true
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.getForeignLanguageAvailableList()
        viewModel.loadForeignLanguageAvailablePharmacyList(excelHelper)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }

    private fun getCurrentLocation() {
        if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lifecycleScope.launch {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    it?.let {
                        val currentLatLng = LatLng.from(it.latitude, it.longitude)
                        latitude = it.latitude
                        longitude = it.longitude
                        viewModel.getLocation(currentLatLng.latitude, currentLatLng.longitude)
                        lifecycleScope.launch {
                            viewModel.langPharmacyList.collect {
                                if (it.isNotEmpty()) {
                                    viewModel.searchPharmacyLocation(
                                        currentLatLng.longitude.toString(),
                                        currentLatLng.latitude.toString(),
                                        1,
                                    )
                                }
                            }
                        }

                        kakaoMap?.let { map ->

                            // 카메라 이동

                            map.moveCamera(CameraUpdateFactory.newCenterPosition(currentLatLng, 20))

                            val bitmap = vectorToBitmap(R.drawable.current_pin)
                            // 1. LabelStyles 생성하기
                            val style =
                                LabelStyles.from(
                                    LabelStyle.from(bitmap),
                                )

                            val markerStyles =
                                map.labelManager?.addLabelStyles(
                                    style,
                                )

                            // LabelOptions 생성하기
                            val options =
                                LabelOptions
                                    .from(LatLng.from(it.latitude, it.longitude))
                                    .setStyles(markerStyles)

                            Timber.d("latitude :${it.latitude}, longitude:${it.longitude}")
                            layer = kakaoMap?.labelManager?.layer

                            // LabelLayer 가져오기
                            layer?.addLabel(options)
                        }
                    }
                }
            }
        }
    }

    private fun removeHospitalLabel() {
        hospitalLabelList.forEach {
            layer?.remove(it)
        }
        hospitalLabelList = emptyList<Label>().toMutableList()
    }

    private fun removePharLabel() {
        pharLabelList.forEach {
            layer?.remove(it)
        }
        pharLabelList = emptyList<Label>().toMutableList()
    }

    // 벡터 드로어블을 명시적으로 비트맵으로 변환
    private fun vectorToBitmap(
        @DrawableRes vectorResId: Int,
    ): Bitmap {
        val drawable = AppCompatResources.getDrawable(requireContext(), vectorResId)
        val bitmap =
            Bitmap.createBitmap(
                drawable!!.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888,
            )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun putLabelOnLocation(
        bitmap: Bitmap,
        list: List<SearchLocationItem>,
    ) {
        kakaoMap.let { map ->
            // 1. LabelStyles 생성하기

            val style =
                LabelStyles
                    .from(
                        LabelStyle.from(bitmap),
                    )

            val markerStyles =
                map?.labelManager?.addLabelStyles(
                    style,
                )

            list.forEach {
                val position =
                    LatLng.from(
                        it.latitude.toDouble(),
                        it.longitude.toDouble(),
                    )

                // LabelOptions 생성하기
                val options =
                    LabelOptions
                        .from(position)
                        .setStyles(markerStyles)

                val label = layer?.addLabel(options)
                if (it.type == "Hospital") {
                    if (label != null) {
                        hospitalLabelList.add(label)
                        Timber.d("병원 라벨 $hospitalLabelList")
                    }
                } else {
                    if (label != null) {
                        pharLabelList.add(label)
                        Timber.d("약국 라벨 $hospitalLabelList")
                    }
                }
                label?.tag = it

                map?.moveCamera(
                    CameraUpdateFactory.newCenterPosition(
                        LatLng.from(
                            latitude,
                            longitude,
                        ),
                        15,
                    ),
                )
            }
        }
    }

    private fun getHospitalNearByMe() {
        lifecycleScope.launch {
            viewModel.hospitalList.collectLatest {
                if (it.isNotEmpty()) {
                    val bitmap = vectorToBitmap(R.drawable.hospital_pin)
                    putLabelOnLocation(bitmap, it)
                } else {
                    return@collectLatest
                }
            }
        }
    }

    private fun getPharmacyNearByMe() {
        lifecycleScope.launch {
            viewModel.pharmacyList.collectLatest {
                if (it.isNotEmpty()) {
                    val bitmap = vectorToBitmap(R.drawable.pha_pin)
                    putLabelOnLocation(bitmap, it)
                }
            }
        }
    }
}
