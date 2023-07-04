package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class AppLoadingActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_loading_screen) // 먼저 로딩 화면을 보여준다.

        android.os.Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish() // AppLoadingActivity를 닫는다.
        }, 3000) // 3초 뒤에 이 메서드가 실행되어, MainActivity로 이동한다.
    }
}