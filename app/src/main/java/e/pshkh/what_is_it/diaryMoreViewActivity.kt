package e.pshkh.what_is_it

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_diary_more_view.*

class diaryMoreViewActivity : AppCompatActivity() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val diaryDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var diaryId: String = ""
    private var ownerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_more_view)

        val actionBar = supportActionBar
        actionBar?.title = "더보기"
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setDisplayShowHomeEnabled(true)

        diaryMoreTitle.text = intent.getCharSequenceExtra("title")
        diaryMoreDate.text = intent.getCharSequenceExtra("date")
        diaryMoreContent.text = intent.getCharSequenceExtra("content")
        diaryId = intent.getCharSequenceExtra("diaryId").toString()
        ownerId = auth.currentUser!!.uid
        // if(intent.getBooleanExtra("is_photo", false))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        var mi = MenuInflater(this).inflate(R.menu.diary_more_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            R.id.action_diary_delete -> {
                diaryDB.collection("DiaryBook").document(ownerId).collection("diary").document(diaryId).delete()
                diaryDB.collection("StudyRoom").document(ownerId).collection("message").document(diaryId).update("_scraped", false)
                onBackPressed()
                Toast.makeText(this@diaryMoreViewActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            R.id.action_diary_share -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}

