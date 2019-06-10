package e.pshkh.what_is_it

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import e.pshkh.what_is_it.data_transfer_object.DiaryBookDTO
import kotlinx.android.synthetic.main.activity_emotion_result.*
import java.text.SimpleDateFormat
import java.util.*

class EmotionResultActivity : AppCompatActivity() {
    private var storage: FirebaseStorage? = null
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setContentView(R.layout.activity_emotion_result)

        var mToolbar: Toolbar = this.resulttoolbar
        setSupportActionBar(mToolbar)
        supportActionBar!!.title = "미리보기"
        var uriStr = intent.getStringExtra("jpeg")
        var emotion = intent.getStringExtra("emotion")
        var imgUri = Uri.parse("file://" + uriStr)
        var bm = BitmapFactory.decodeFile(uriStr)

        emotionResultView.setImageBitmap(bm)

        resultCancelBtn.setOnClickListener {
            finish()
        }

        resultUploadBtn.setOnClickListener {
            val date = SimpleDateFormat("yyyy-MM-dd-hhmmss").format(Date())
            var imageFileName = ""
            // 사진의 파일 형식은 png로 하며 파일명은 사진이 업로드된 시각으로 합니다.
            imageFileName = "JPG_" + date + "_.jpg"
            // 파이어베이스 스토리지에 images라는 디렉터리를 생성하고 그 하위에 UID로 디렉터리를 생성하고 이 아래에 이미지 저장
            val storageRef =
                storage?.reference?.child("images")?.child(auth?.currentUser?.uid.toString())!!.child("emotion")
                    .child(imageFileName)

            val dialog = AlertDialog.Builder(this@EmotionResultActivity)
            val input = EditText(this@EmotionResultActivity)
            input.setSingleLine()
            dialog.setTitle("다이어리 제목").setMessage("다이어리 제목을 입력해주세요.").setView(input)
                .setPositiveButton("확인") { dialogInterface, i ->
                    var subject = input.text.toString()

                    val progressDialog: ProgressDialog = ProgressDialog(this) // Deprecated되었으나 그냥 사용하겠음
                    progressDialog.setMessage("이미지 업로드중...")
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                    progressDialog.show()

                    storageRef?.putFile(imgUri!!)?.addOnSuccessListener { taskSnapshot ->
                        val diaryDTO = DiaryBookDTO.Diary()
                        var owner_id = auth!!.currentUser!!.uid
                        var question = uriStr
                        var answer = "감정 상태: $emotion"
                        var diary_id = ""
                        val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
                        val timestamp = Timestamp.now()

                        var diary_book_id = owner_id
                        diary_id = 0.toString() + timestamp

                        var is_photo = true

                        diaryDTO.diary_id = diary_id
                        diaryDTO.answer = answer
                        diaryDTO.date = date
                        diaryDTO.question = question
                        diaryDTO.timestamp = timestamp
                        diaryDTO.owner_id = owner_id
                        diaryDTO.is_photo = is_photo
                        diaryDTO.subject = subject
                        diaryDTO.userEmail = "t@t.com"

                        firestore!!.collection("DiaryBook").document(auth?.currentUser?.uid!!).collection("diary")
                            .document(diary_id!!).set(diaryDTO)

                    }.addOnProgressListener { taskSnapshot ->
                        val progress = (100 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                        progressDialog.progress = progress.toInt()
                    }.addOnCompleteListener {
                        Toast.makeText(this@EmotionResultActivity, "다이어리에 등록이 완료되었습니다.", Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                        finish()

                    }

                }
                .setNegativeButton("취소")
                { dialogInterface, i -> }
                .show()
        }
    }


}
