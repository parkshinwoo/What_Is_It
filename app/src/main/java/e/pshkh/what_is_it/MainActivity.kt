package e.pshkh.what_is_it

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import e.pshkh.what_is_it.navigation_activity.DiaryFragment
import e.pshkh.what_is_it.navigation_activity.EmotionFragment
import e.pshkh.what_is_it.navigation_activity.TeacherActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // 하단 네비게이션 바에 여러 아이템들이 있습니다.
        // 사용자가 네비게이션 바의 특정 아이템을 선택하면 해당하는 화면으로 이동시켜주는 기능입니다.
        when (item.itemId) {

            R.id.action_teacher -> {
                if (bottom_navigation.selectedItemId == R.id.action_teacher)
                    return false
                // 스토리지, 카메라 접근 권한 체크를 합니다.
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                    ) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    startActivity(Intent(this, TeacherActivity::class.java))
                } else {
                    Toast.makeText(this, "EXTERNAL STORAGE 읽기 권한이 없습니다.", Toast.LENGTH_LONG).show()
                }

                return false
            }

            R.id.action_diary -> {
                if (bottom_navigation.selectedItemId == R.id.action_diary)
                    return false

                supportActionBar!!.title = "다이어리"
                val fragment = DiaryFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, fragment).commit()
                return true
            }


            R.id.action_recognize_emotion -> {

                if (bottom_navigation.selectedItemId == R.id.action_recognize_emotion)
                    return false
                supportActionBar!!.title = "내기분"
                // 스토리지, 카메라 접근 권한 체크를 합니다.
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                    ) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    val fragment = EmotionFragment()
                    supportFragmentManager.beginTransaction().replace(R.id.main_content, fragment).commit()
                    return true
                } else {
                    Toast.makeText(this, "EXTERNAL STORAGE 읽기 권한이 없습니다.", Toast.LENGTH_LONG).show()
                }


            }

        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var mToolbar: Toolbar = this.findViewById(R.id.toolbar2)
        setSupportActionBar(mToolbar)
        supportActionBar!!.title = "다이어리"
        val transaction: androidx.fragment.app.FragmentTransaction = supportFragmentManager.beginTransaction();
        transaction.replace(R.id.main_content, DiaryFragment()).commitAllowingStateLoss()
        bottom_navigation.setOnNavigationItemSelectedListener(this)

        // 시작할때마다 홈에서 시작하게끔 하단 네비게이션의 현재 선택된 아이템을 다이어리로 지정합니다.
        //  bottom_navigation.selectedItemId = R.id.action_diary

        // 디바이스 사진첩, 카메라에 접근할 권한을 줍니다.
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
            1
        )
    }
}
