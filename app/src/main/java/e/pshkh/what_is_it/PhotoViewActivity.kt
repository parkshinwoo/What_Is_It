package e.pshkh.what_is_it

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_photo_view.*
import kotlinx.android.synthetic.main.recyclerview_item_design_teacher.view.*

class PhotoViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)

        val actionBar = supportActionBar
        actionBar?.title = "사진보기"
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setDisplayShowHomeEnabled(true)

        var photoUri = intent.getCharSequenceExtra("photoUri")

        Glide.with(this).load(photoUri).apply(RequestOptions().fitCenter())
            .into(this.photo_view)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}
