package e.pshkh.what_is_it

import android.Manifest
import android.os.Bundle
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener {

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        setToolbarDefault()

        // 하단 네비게이션 바에 여러 아이템들이 있습니다.
        // 사용자가 네비게이션 바의 특정 아이템을 선택하면 해당하는 화면으로 이동시켜주는 기능입니다.

        /*
        when (item.itemId) {
            R.id.action_home -> {

                /*
                val homeFragment = HomeFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, homeFragment).commit()
                */

                return true
            }

              R.id.recognize_text -> {



                return true

            }

            R.id.recognize_face -> {



                return true

            }

            R.id.action_image -> {

                // 스토리지 접근 권한 체크를 합니다.
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, UploadPhotoActivity::class.java))
                } else {
                    Toast.makeText(this, "EXTERNAL STORAGE 읽기 권한이 없습니다.", Toast.LENGTH_LONG).show()
                }


                return true
            }

            R.id.landmark -> {


            }
            R.id.language -> {



                return true
            }


            R.id.action_teacher -> {

                startActivity(Intent(this, TeacherActivity::class.java))

                return true
            }

        */
        return false
    }

    // 맨 상단에 위치하는게 툴바입니다.
    // 툴바의 기본 디자인을 지정하는 코드입니다.
    fun setToolbarDefault(){

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottom_navigation_top.setOnNavigationItemSelectedListener(this)
        bottom_navigation_middle.setOnNavigationItemSelectedListener(this)
        bottom_navigation_down.setOnNavigationItemSelectedListener(this)

        // 시작할때마다 홈에서 시작하게끔 하단 네비게이션의 현재 선택된 아이템을 "home"으로 지정합니다.
        bottom_navigation_top.selectedItemId = R.id.action_home

        // 디바이스 사진첩에 접근할 권한을 줍니다.
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1 )
    }
}
