package com.teammeditalk.medicalconnect.app

import android.app.Application
import com.google.android.datatransport.runtime.BuildConfig
import com.kakao.vectormap.KakaoMapSdk
import timber.log.Timber

class Application : Application() {
    private fun setupTimber() {
        Timber.plant(Timber.DebugTree())
    }

    override fun onCreate() {
        super.onCreate()
        // 디버그 빌드에서만 로그 활성화
        setupTimber()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        KakaoMapSdk.init(this, "b03aaef5a96230996325f15fcf69042e")
    }
}
