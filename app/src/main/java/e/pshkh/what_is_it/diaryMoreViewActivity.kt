package e.pshkh.what_is_it

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import kotlinx.android.synthetic.main.activity_diary_more_view.*

class diaryMoreViewActivity : AppCompatActivity() {

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
        // if(intent.getBooleanExtra("is_photo", false))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        var mi = MenuInflater(this).inflate(R.menu.diary_more_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}

