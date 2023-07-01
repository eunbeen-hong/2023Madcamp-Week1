package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val myPermissionRequestCode = 22

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_gallery, R.id.navigation_calendar))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        checkPermission() // 파일 접근 권한과 카메라 접근 권한을 확인하자
    }

    private fun checkPermission() {
        val permissionList = arrayOf( // 요청하고자 하는 권한 목록 (파일 쓰기, 파일 읽기, 카메라)
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            //Manifest.permission.READ_MEDIA_AUDIO,
            //Manifest.permission.READ_MEDIA_IMAGES,
            //Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.CAMERA)
        for (i in permissionList) {
            // 리스트 중에 만약 현재 허용하지 않은 권한이 있다면, 권한을 요청한다.
            if (ContextCompat.checkSelfPermission(this, i) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissionList, myPermissionRequestCode)
                break
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,               // 요청한 주체를 확인하는 코드
        permissions: Array<out String>, // 요청한 권한 목록
        grantResults: IntArray          // 요청한 권한 목록에 대한 승인/미승인 결과값
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == myPermissionRequestCode && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this,"권한 성공.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this,"권한 내놔.", Toast.LENGTH_LONG).show()
            checkPermission()
        }



        /*
            private fun checkPermission(): Boolean {
                val mPermissionsGranted: Boolean  // 위험 권한을 모두 승인했는지 여부
                val mRequiredPermissions = arrayOf<String>( // 승인 받기 위한 권한 목록
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                // 필수 권한을 가지고 있는지 확인한다.
                mPermissionsGranted = hasPermissions(mRequiredPermissions)

                // 필수 권한 중에 한 개라도 없는 경우
                if (!mPermissionsGranted) {
                    // 권한을 요청한다.
                    ActivityCompat.requestPermissions(this@MainActivity, mRequiredPermissions, PERMISSIONS_REQUEST_CODE)
                }
                return mPermissionsGranted
            }


            private fun hasPermissions(permissions: Array<String>): Boolean {
                // 필수 권한을 가지고 있는지 확인한다.
                for (permission in permissions) {
                    if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                        return false
                    }
                }
                return true
            }

            override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                if (requestCode == PERMISSIONS_REQUEST_CODE) {
                    // 권한을 모두 승인했는지 여부
                    var chkFlag = false
                    // 승인한 권한은 0 값, 승인 안한 권한은 -1을 값으로 가진다.
                    for (g in grantResults) {
                        if (g == -1) {
                            chkFlag = true
                            break
                        }
                    }

                    // 권한 중 한 개라도 승인 안 한 경우
                    if (chkFlag) {
                        checkPermission()
                    }
                } */
            }
}