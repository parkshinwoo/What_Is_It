package e.pshkh.what_is_it.navigation_activity

import ai.api.AIConfiguration
import ai.api.AIDataService
import ai.api.model.AIRequest
import ai.api.model.Result
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.theartofdev.edmodo.cropper.CropImage
import e.pshkh.what_is_it.PhotoViewActivity
import e.pshkh.what_is_it.R
import e.pshkh.what_is_it.data_transfer_object.DiaryBookDTO
import e.pshkh.what_is_it.data_transfer_object.StudyRoomDTO
import e.pshkh.what_is_it.data_transfer_object.WeatherDTO
import e.pshkh.what_is_it.data_transfer_object.NaverEnDTO
import kotlinx.android.synthetic.main.activity_teacher.*
import kotlinx.android.synthetic.main.recyclerview_item_design_teacher.view.*
import net.objecthunter.exp4j.ExpressionBuilder
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TeacherActivity : AppCompatActivity() {
    val message_list: ArrayList<StudyRoomDTO.Message?> = ArrayList()
    // 다이얼로그 플로우와 통신하기 위한 클래스입니다.
    var aiDataService: AIDataService? = null

    // 챗봇(다이얼로그 플로우)과 날짜를 주고 받으려면 서로 이해할 수 있는 형식이어야 합니다.

    // 사람(HumanText)이 이해 할 수 있는 날짜를 컴퓨터가 이해할 수 있는 날짜 형식으로 맞추기 위한 값입니다.
    var dateFormatFromHumanText = SimpleDateFormat("yyyy-MM-dd")

    // 날씨 API(OpenWeatherMap API)에게 넘겨주기 위한 날짜 형식
    var weatherDataFormatFromHumanText = SimpleDateFormat("yyyy-MM-dd")

    // 날씨 API에게 넘겨받은 날짜를 사람이 이해하기 쉬운 형식으로 바꾸기
    var weatherDataFormatToHumanText = SimpleDateFormat("MM월 dd일 hh시")

    // 안드로이드 디바이스의 앨범에서 선택된 사진이 맞는지를 확인하는 코드 값입니다.
    // 코드의 하단부 onActivityResult에서 사용됩니다.
    val PICK_IMAGE_FROM_ALBUM = 0
    val REQUEST_TAKE_ALBUM = 1

    // 파이어베이스 스토리지(저장소) 접근을 위한 값입니다.
    var storage: FirebaseStorage? = null

    // 어린이가 올린 사진의 주소를 담습니다.
    var photoUri: Uri? = null

    // 어린이가 올린 텍스트를 담습니다.
    var question: String? = "이게 뭐야?"

    // 질문이 사진인지 글인지 체크
    var q_text_flag: Boolean? = true // true면 텍스트

    // 파이어베이스 사용자 인증을 위한 값입니다. 이 값을 통해 사용자의 uid, 이메일 주소 등에 접근합니다.
    var auth: FirebaseAuth? = null

    // 파이어베이스 스토어(데이터베이스) 접근을 위한 값입니다.
    var firestore: FirebaseFirestore? = null

    var teacherSnapshot: ListenerRegistration? = null
    var chatSnapshot: ListenerRegistration? = null
    var diarySnapshot: ListenerRegistration? = null

    val langaugeIdentifier = FirebaseNaturalLanguage.getInstance().languageIdentification
    var language_code: String? = ""
    val language_code_map: Map<String, String> = mapOf(
        "en" to "영어",
        "es" to "스페인어",
        "fr" to "프랑스어",
        "ja" to "일본어",
        "ko" to "한국어",
        "ru" to "러시아어",
        "zh" to "중국어",
        "de" to "독일어",
        "pt" to "포르투갈어",
        "sl" to "슬로베니아어"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher)

        val actionBar = supportActionBar
        actionBar!!.title = "선생님"
        actionBar.setDisplayHomeAsUpEnabled(true)

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        /*
        v1 will be deprecated.. soon
         */
        // 다이얼로그 플로우 v1 API "개발자" 키 값을 넣어줍니다.
        // v2 API는 안드로이드를 아직 지원 안합니다.
        // 이 앱을 배포할 시엔 "클라이언트" 키 값을 넣으면 됩니다.
        var config = AIConfiguration("1d630233cd00435d813e563a28cb5373", AIConfiguration.SupportedLanguages.Korean)
        // 설정한 값으로 aiDataService 객체를 생성합니다.
        aiDataService = AIDataService(config)

        // 텍스트로 질문할 경우 발생하는 이벤트
        chat.setOnClickListener {
            if (chatText.text.toString() != "") {
                question = chatText.text.toString()

                // processing question here
                // 언어감지, 실시간 번역
                langaugeIdentifier.identifyLanguage(question!!).addOnSuccessListener {
                    language_code = it // 언어감지 결과 식별 언어의 코드 (영어는 en, 한글은 ko)
                    sendMessage(language_code)
                }.addOnFailureListener {
                    sendMessage("")
                }

                chatText.setText("")
            } else {
                Toast.makeText(this, "질문을 입력해주세요", Toast.LENGTH_SHORT).show()
                chatText.setText("")
            }
        }

        // 키보드 전송버튼을 눌렀을 경우 발생하는 이벤트
        chatText.setOnEditorActionListener() { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                if (chatText.text.toString() != "") {
                    question = chatText.text.toString()

                    // processing question here
                    // 언어감지, 실시간 번역
                    langaugeIdentifier.identifyLanguage(question!!).addOnSuccessListener {
                        language_code = it // 언어감지 결과 식별 언어의 코드 (영어는 en, 한글은 ko)
                        sendMessage(language_code)
                    }.addOnFailureListener {
                        sendMessage("")
                    }

                    chatText.setText("")
                } else {
                    Toast.makeText(this, "질문을 입력해주세요", Toast.LENGTH_SHORT).show()
                    chatText.setText("")
                }
                true
            } else {
                false
            }
        }

        // 사진으로 질문할 경우 발생하는 이벤트
        chatImage.setOnClickListener {

            q_text_flag = false

            // crop image properly by user
            CropImage.activity().start(this)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        val rLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerview.adapter = TeacherRecyclerViewAdapter()
        rLayoutManager.stackFromEnd = true
        recyclerview.layoutManager = rLayoutManager
    }

    override fun onStop() {
        super.onStop()
        teacherSnapshot?.remove()
        chatSnapshot?.remove()
        diarySnapshot?.remove()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)

            if (resultCode == Activity.RESULT_OK) {
                // if crop task done upload the photo
                photoUri = result.uri
                photoUpload()
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "에러 : ${result.error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    inner class TeacherRecyclerViewAdapter :
        androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

        init {
            chatSnapshot = firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message")
                .orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                    if (querySnapshot == null) return@addSnapshotListener

                    message_list.clear()
                    var message: StudyRoomDTO.Message?
                    for (snapshot in querySnapshot.documents) {
                        message_list.add(snapshot.toObject(StudyRoomDTO.Message::class.java))
                        message = message_list.get(message_list.size - 1)
                    }

                    if (message_list.isNotEmpty()) {
                        recyclerview.adapter?.notifyDataSetChanged()
                        recyclerview.layoutManager?.scrollToPosition(message_list.size - 1)
                    }

                }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): androidx.recyclerview.widget.RecyclerView.ViewHolder {
            var view =
                LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_item_design_teacher, parent, false)
            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return message_list.size
        }

        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {

            holder.setIsRecyclable(false)

            if (message_list[position]?.owner_id != "0") {

                if (message_list[position]?.is_photo == false) {
                    // 내가 챗봇에게 보낸 텍스트 메세지일 경우에는 나의 말풍선만 보이게 하고
                    // 챗봇의 말풍선은 가려야합니다.
                    holder.itemView.imagebubble.visibility = View.GONE
                    holder.itemView.right_chatbubble.visibility = View.VISIBLE
                    holder.itemView.right_chatbubble.text = message_list[position]?.message_content.toString()
                    holder.itemView.rightTime.text =
                        SimpleDateFormat("aa hh:mm").format(message_list[position]?.timestamp!!.toDate())
                    holder.itemView.left_chatbubble.visibility = View.GONE
                    holder.itemView.leftTime.visibility = View.GONE
                } else {
                    // 내가 챗봇에게 사진을 보냈을 경우
                    holder.itemView.left_chatbubble.visibility = View.GONE
                    holder.itemView.right_chatbubble.visibility = View.GONE
                    holder.itemView.leftTime.visibility = View.GONE
                    holder.itemView.rightTime.text =
                        SimpleDateFormat("aa hh:mm").format(message_list[position]?.timestamp!!.toDate())

                    var photoUri: String?
                    photoUri = message_list[position]?.message_content as String
                    holder.itemView.imagebubble.visibility = View.VISIBLE

                    // using Glide
                    var requestOptions = RequestOptions()
                    requestOptions = requestOptions.transform(RoundedCorners(16))
                    Glide.with(holder.itemView.context).load(photoUri).apply(RequestOptions().centerCrop())
                        .apply(requestOptions).into(holder.itemView.imagebubble)

                    holder.itemView.imagebubble.setOnClickListener {
                        val photoViewIntent = Intent(this@TeacherActivity, PhotoViewActivity::class.java)
                        photoViewIntent.putExtra("photoUri", photoUri)
                        startActivity(photoViewIntent)
                    }
                }
            } else {
                // 챗봇이 내게 보낸 메세지일 경우
                holder.itemView.rightBubbleLayout.visibility = View.GONE
                holder.itemView.left_chatbubble.visibility = View.VISIBLE
                holder.itemView.left_chatbubble.text = message_list[position]?.message_content.toString()
                holder.itemView.leftTime.text =
                    SimpleDateFormat("aa hh:mm").format(message_list[position]?.timestamp!!.toDate())

                holder.itemView.checkImg.visibility =
                    if (message_list[position]?.is_scraped!!) View.VISIBLE else View.GONE
                holder.itemView.rightTime.visibility = View.GONE
            }

            // 다이어리에 올리는 이벤트 발생
            holder.itemView.left_chatbubble.setOnLongClickListener {
                if (message_list[position]?.owner_id != null && message_list[position]?.message_id != null) {
                    if (!message_list[position]?.is_scraped!!) {
                        var owner_id = auth!!.currentUser!!.uid
                        var question = message_list[position]?.question
                        var answer = message_list[position]?.message_content.toString()
                        var subject = message_list[position]?.subject
                        var diary = DiaryBookDTO.Diary()

                        val dialog = AlertDialog.Builder(this@TeacherActivity)
                        val input = EditText(this@TeacherActivity)
                        input.setSingleLine()
                        dialog.setTitle("다이어리 제목").setMessage("다이어리 제목을 입력해주세요.").setView(input)
                            .setPositiveButton("확인") { dialogInterface, i ->
                                subject = input.text.toString()

                                val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
                                val timestamp = Timestamp.now()

                                var diary_book_id = owner_id
                                var diary_id = message_list[position]?.message_id

                                var is_photo: Boolean? = false
                                if (message_list[position - 1]?.is_photo != false) {
                                    is_photo = true
                                    question = message_list[position - 1]?.message_content
                                }

                                diary.diary_id = diary_id
                                diary.answer = answer
                                diary.date = date
                                diary.question = question
                                diary.timestamp = timestamp
                                diary.owner_id = owner_id
                                diary.is_photo = is_photo
                                diary.subject = subject

                                FirebaseFirestore.getInstance().collection("users").whereEqualTo("uid", owner_id!!)
                                    .get().addOnCompleteListener {
                                        if (it.isSuccessful) {
                                            for (document in it.result!!) {
                                                diary.userEmail = document.data["userEmail"].toString()
                                            }
                                            // 다이어리에 답변 저장
                                            FirebaseFirestore.getInstance().collection("DiaryBook")
                                                .document(diary_book_id!!).collection("diary").document(diary_id!!)
                                                .set(diary)
                                        }
                                    }

                                // 답변 스크랩 업데이트
                                var map = mutableMapOf<String, Any>()
                                map["_scraped"] = true
                                FirebaseFirestore.getInstance().collection("StudyRoom").document(owner_id)
                                    .collection("message").document(message_list[position]?.message_id!!).update(map)
                                    ?.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(this@TeacherActivity, "다이어리에 추가되었습니다.", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    }
                            }
                            .setNegativeButton("취소") { dialogInterface, i -> }
                            .show()
                    } else {
                        Toast.makeText(this@TeacherActivity, "이미 다이어리에 추가된 메세지입니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
        }
    }

    // 메세지를 전송할 때 사용하는 함수
    fun sendMessage(language_code: String?) {
        // DB에 메세지 올리기
        val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val timestamp = Timestamp.now()

        // 공부방에 추가할 메세지 생성
        var message = StudyRoomDTO.Message()
        message.timestamp = timestamp
        message.date = date
        message.message_content = question
        message.message_id = auth?.currentUser?.uid.toString() + timestamp.toString()
        message.question_id = auth?.currentUser?.uid.toString() + timestamp.toString()
        message.owner_id = auth?.currentUser?.uid.toString()
        message.question = question
        message.subject = ""

        // 파이어베이스 DB의 공부방 하위에 메세지 저장
        firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message")
            .document(message.message_id!!).set(message)

        val calReg = Regex("[0-9]+[*\\/\\-+][0-9]+")
        var language_answer = "무슨 말인지 모르겠구나"
        if (question!!.contains("달러") || question!!.contains("유로") || question!!.contains("엔") || question!!.contains("위안")) {
            if (question!!.contains("달러")) {
                val amount = question!!.split("달러")
                convert_currency_rate(amount[0].toInt(), "USDKRW", message.message_id)
            } else if (question!!.contains("유로")) {
                val amount = question!!.split("유로")
                convert_currency_rate(amount[0].toInt(), "EURKRW", message.message_id)
            } else if (question!!.contains("엔")) {
                val amount = question!!.split("엔")
                convert_currency_rate(amount[0].toInt(), "JPYKRW", message.message_id)
            } else if (question!!.contains("위안")) {
                val amount = question!!.split("위안")
                convert_currency_rate(amount[0].toInt(), "CNYKRW", message.message_id)
            }
        } else if (calReg.containsMatchIn(question!!)) {
            try {
                val expression = ExpressionBuilder(calReg.find(question!!)!!.value).build()
                language_answer = "연산 결과는 " + expression.evaluate().toString() + " 이란다."
            } catch (ex: ArithmeticException) {
                language_answer = "무슨 말인지 모르겠구나. 식을 정확히 써줄래?"
            } catch (ex: IllegalArgumentException) {
                language_answer = "무슨 말인지 모르겠구나. 식을 정확히 써줄래?"
            }
            do_answer(language_answer, "수학", message.message_id)
        } else {
            if (language_code.equals("") or language_code.equals("ko")) {
                // 챗봇(다이얼로그 플로우)와 통신하는 쓰레드를 실행합니다.
                TalkAsyncTask().execute(question, message.message_id)
            } else if (language_code.equals("und")) {
                // 숫자
            } else {
                // 언어코드를 활용하여 답변 메세지 생성

                var code = FirebaseTranslateLanguage.languageForLanguageCode(language_code) ?: 0
                val options = FirebaseTranslatorOptions.Builder().setSourceLanguage(code)
                    .setTargetLanguage(FirebaseTranslateLanguage.KO).build()
                val translator = FirebaseNaturalLanguage.getInstance().getTranslator(options)
                translator.downloadModelIfNeeded().addOnSuccessListener {
                    translator.translate(message.question.toString()).addOnSuccessListener { it ->
                        var language_answer =
                            "방금 질문한건 " + language_code_map.get(language_code) + " 란다!\n번역하면 아래와 같은 뜻이야.\n$it"
                        do_answer(language_answer, "언어", message.message_id)
                    }.addOnFailureListener {
                        Log.d("Translation Error", it.message)
                    }
                }.addOnFailureListener {
                    Log.d("TranslateDownloadError", it.message)
                }
            }
        }
    }

    // 이미지를 파이어베이스 스토리지 및 스토어에 업로드하는 함수입니다.
    fun photoUpload() {

        // 사진이 업로드된 날짜 및 시각을 담는 변수입니다.
        val date = SimpleDateFormat("yyyy-MM-dd-hhmmss").format(Date())
        val timestamp = Timestamp.now()
        // 사진의 파일 형식은 png로 하며 파일명은 사진이 업로드된 시각으로 합니다.
        val imageFileName = "PNG_" + date + "_.png"
        // 파이어베이스 스토리지에 images라는 디렉터리를 생성하고 그 하위에 UID로 디렉터리를 생성하고 이 아래에 이미지 저장
        val storageRef =
            storage?.reference?.child("images")?.child(auth?.currentUser?.uid.toString())!!.child(imageFileName)

        val progressDialog: ProgressDialog = ProgressDialog(this) // Deprecated되었으나 그냥 사용하겠음
        progressDialog.setMessage("이미지 업로드중...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.show()
        // 파이어베이스 스토리지에 이미지 올리기
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener { taskSnapshot ->
            storageRef.downloadUrl.addOnCompleteListener {
                // 업로드된 이미지 주소를 가져오기 파일 경로
                var uri = it.result

                // 공부방에 추가할 메세지 생성
                var message = StudyRoomDTO.Message()
                message.timestamp = timestamp
                message.date = date
                message.message_content = uri!!.toString() // 사진일 경우 메세지의 내용은 사진의 uri
                message.is_photo = true
                message.message_id = auth?.currentUser?.uid.toString() + timestamp.toString()
                message.question_id = auth?.currentUser?.uid.toString() + timestamp.toString()
                message.owner_id = auth?.currentUser?.uid.toString()
                message.question = uri!!.toString()

                // ML Kit Image Labeling 사용
                val imageML =
                    FirebaseVisionImage.fromBitmap(MediaStore.Images.Media.getBitmap(this.contentResolver, photoUri))

                val labelDetector = FirebaseVision.getInstance().cloudImageLabeler

                labelDetector.processImage(imageML)
                    .addOnSuccessListener {
                        var resultTexts: ArrayList<String> = ArrayList()
                        for (label in it) {
                            resultTexts.add(label.text)
                        }
                        if ("Landmark" in resultTexts) {
                            val landmarkDetector = FirebaseVision.getInstance().getVisionCloudLandmarkDetector()
                            landmarkDetector.detectInImage(imageML).addOnSuccessListener { landmarks ->
                                langaugeIdentifier.identifyLanguage(landmarks[0].landmark!!).addOnSuccessListener {
                                    var langCode = FirebaseTranslateLanguage.languageForLanguageCode(it) ?: 0
                                    val options =
                                        FirebaseTranslatorOptions.Builder().setSourceLanguage(langCode)
                                            .setTargetLanguage(FirebaseTranslateLanguage.KO).build()
                                    val translator = FirebaseNaturalLanguage.getInstance().getTranslator(options)
                                    translator.downloadModelIfNeeded().addOnSuccessListener {
                                        translator.translate(landmarks[0].landmark).addOnSuccessListener { it ->
                                            getInfoNaver(it, message.message_id)
                                        }.addOnFailureListener {
                                            Log.d("Translation Error", it.message)
                                            getInfoNaver(landmarks[0].landmark, message.message_id)
                                        }
                                    }.addOnFailureListener {
                                        Log.d("TranslateDownloadError", it.message)
                                    }
                                }

                            }
                        } else if ("Text" in resultTexts || "Font" in resultTexts || "Document" in resultTexts || "Writing" in resultTexts || "Novel" in resultTexts) {

                            val options = FirebaseVisionCloudTextRecognizerOptions.Builder()
                                .setLanguageHints(Arrays.asList("en", "es", "ja", "ru", "zh", "de", "pt"))
                                .build()

                            val textRecognizer = FirebaseVision.getInstance().getCloudTextRecognizer(options)
                            textRecognizer.processImage(imageML)
                                .addOnSuccessListener {
                                    var ocrMsg = ""
                                    var blockLangugeCode: String? = "EN"
                                    var targetText: String? = ""
                                    for (block in it.textBlocks) {
                                        targetText += block.text + " "
                                    }

                                    langaugeIdentifier.identifyLanguage(targetText!!).addOnSuccessListener {it ->
                                        var code = FirebaseTranslateLanguage.languageForLanguageCode(it) ?: 0
                                        val options = FirebaseTranslatorOptions.Builder().setSourceLanguage(code)
                                            .setTargetLanguage(FirebaseTranslateLanguage.KO).build()
                                        val translator = FirebaseNaturalLanguage.getInstance().getTranslator(options)

                                        ocrMsg += "이 사진 속 글은 " + language_code_map.get(it) + "로 쓰여있구나.\n이 글의 뜻은 이것이란다\n\n"

                                        translator.downloadModelIfNeeded().addOnSuccessListener {
                                            translator.translate(targetText!!).addOnSuccessListener { it ->
                                                ocrMsg += it
                                                do_answer(ocrMsg, "언어", message.message_id)
                                            }.addOnFailureListener {
                                                Log.d("Translation Error", it.message)
                                            }
                                        }.addOnFailureListener {
                                            Log.d("TranslateDownloadError", it.message)
                                        }
                                    }.addOnFailureListener {
                                    }


                                }
                                .addOnFailureListener {
                                    Log.d("Text Recognition Error", it.message)
                                }
                        } else {
                            val options =
                                FirebaseTranslatorOptions.Builder().setSourceLanguage(FirebaseTranslateLanguage.EN)
                                    .setTargetLanguage(FirebaseTranslateLanguage.KO).build()
                            val translator = FirebaseNaturalLanguage.getInstance().getTranslator(options)
                            translator.downloadModelIfNeeded().addOnSuccessListener {
                                translator.translate(resultTexts[0]).addOnSuccessListener { it ->
                                    var photoMsg = "이 사진은 ${it}을(를) 찍은 사진 같구나."
                                    getInfoNaver(it, message.message_id)
                                }.addOnFailureListener {
                                    Log.d("Translation Error", it.message)
                                    getInfoNaver(resultTexts[0], message.message_id)
                                }
                            }.addOnFailureListener {
                                Log.d("TranslateDownloadError", it.message)
                            }
                        }
                    }.addOnFailureListener {
                        var photo_answer: String? = "무슨 사진인지 모르겠구나 좀 더 자세히 찍어볼래?"
                        do_answer(photo_answer, "사진", message.message_id) // 분석 결과로 답변
                    }

                // 파이어베이스 DB의 공부방 하위에 질문 메세지 저장
                firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message")
                    .document(message.message_id!!).set(message)
                progressDialog.dismiss()
            }
        }.addOnProgressListener { taskSnapshot ->
            val progress = (100 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            progressDialog.progress = progress.toInt()
        }
    }

    fun getInfoNaver(query: String, messageId: String?) {
        var enUrl =
            "https://openapi.naver.com/v1/search/encyc.json?query=${query}"
        var request = Request.Builder().url(enUrl).addHeader("X-Naver-Client-Id", "9TSnvZkgqp7ktSrVrPUy")
            .addHeader("X-Naver-Client-Secret", "C6zEcYNP4i").build()
        var infoMsg = "이 사진은 ${query}을(를) 찍은 사진 같구나.\n\n"
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
            }

            override fun onResponse(call: Call?, response: Response) {
                var result = response?.body()?.string()
                // json 값을 NaverEnDTO 오브젝트로 만듭니다.
                var enDTO = Gson().fromJson(result, NaverEnDTO::class.java)
                if (response.isSuccessful && enDTO.items!!.size != 0) {
                    infoMsg += enDTO.items!![0].description?.replace(Regex("(<([^>]+)>)"), "")
                    infoMsg += "\n\n더 자세한 설명을 보고싶으면 아래 링크를 참고하렴.\n${enDTO.items!![0].link}"
                } else {
                    infoMsg += "사진에 대해 자세한 정보를 찾는데에는 실패했어."
                }
                do_answer(infoMsg, "사진", messageId)
            }

        })
    }

    fun convert_currency_rate(amount: Int, code: String?, messageId: String?) {
        var rate_url = "https://earthquake.kr:23490/query/${code}"
        var request = Request.Builder().url(rate_url).build()
        var rateMsg = ""

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {

            }

            override fun onResponse(call: Call?, response: Response) {
                if (response.isSuccessful) {
                    var result = response.body()!!.string().split(",")

                    var cash = (result[6].toDouble() * amount)

                    rateMsg = cash.toString() + "원 이란다."
                } else {
                    rateMsg = "이 화폐의 환율은 나도 모른단다. 달러, 유로, 엔, 위안 넷중 한가지로 물어봐주렴"
                }
                do_answer(rateMsg, "경제", messageId)
            }
        })

    }

/* 비동기 언어의 지옥에 빠진 함수
 var translatedMsg = ""
    fun postTranslate(it: String) {
        translatedMsg = it
    }
    fun translate(sourceStr: String, translateLanguage: Int?) { // 번역할 문자열과 문자열의 언어 값
        var translateLanguage = translateLanguage // Non-nullable에 대응
        if (translateLanguage == null)
            translateLanguage = FirebaseTranslateLanguage.EN
        val options = FirebaseTranslatorOptions.Builder().setSourceLanguage(translateLanguage)
            .setTargetLanguage(FirebaseTranslateLanguage.KO).build()
        val translator = FirebaseNaturalLanguage.getInstance().getTranslator(options)
        translator.downloadModelIfNeeded().addOnSuccessListener {
            translator.translate(sourceStr).addOnSuccessListener { it ->
                postTranslate(it)
            }.addOnFailureListener {
                Log.d("Translation Error", it.message)
            }
        }.addOnFailureListener {
            Log.d("TranslateDownloadError", it.message)
        }
    }
    */

    // 챗봇의 답장이 이뤄지는 함수
    fun do_answer(answer: String?, subject: String?, messageId: String?) {

        val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val timestamp = Timestamp.now()
        val answer_id = 0.toString() + timestamp.toString()

        val notAnsweredMessageId = messageId

        var map1 = mutableMapOf<String, Any>()
        map1["_answered"] = true

        var map2 = mutableMapOf<String, Any>()
        map2["answer_id"] = answer_id

        var map3 = mutableMapOf<String, Any>()
        map3["subject"] = subject.toString()

        // 공부방에 추가할 답변 메세지 생성
        var message = StudyRoomDTO.Message()

        message.subject = subject
        message.timestamp = timestamp
        message.date = date
        message.message_content = answer // 선생님의 답변 내용을 담음
        message.is_answered = true
        message.message_id = answer_id
        message.question_id = notAnsweredMessageId
        message.answer_id = answer_id
        message.owner_id = 0.toString()
        message.is_student = false
        message.question = question

        // 파이어베이스 DB의 공부방 하위에 답변 메세지 저장
        firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message")
            .document(answer_id).set(message)

        // 질문 DB 업데이트
        firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message")
            .document(notAnsweredMessageId!!).update(map1).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!)
                        .collection("message")
                        .document(notAnsweredMessageId).update(map2).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!)
                                    .collection("message")
                                    .document(notAnsweredMessageId).update(map3)
                                    .addOnCompleteListener { task ->
                                    }
                            }
                        }
                }
            }
    }

    // 다이얼로그 플로우와 통신하는 쓰레드를 만들어줍니다.
    inner class TalkAsyncTask : AsyncTask<String, Void, Array<Any?>>() {

        override fun doInBackground(vararg params: String?): Array<Any?> {

            // 사용자가 전송한 메세지를 다이얼로그 플로우로 넘겨주는 쿼리입니다.
            var aiRequest = AIRequest()
            var result = arrayOfNulls<Any?>(2)
            aiRequest.setQuery(params[0])
            // 결과를 리턴합니다.
            result[0] = aiDataService?.request(aiRequest)!!.result // 답변
            result[1] = params[1] // messageid
            return result
        }

        // doInBackground 이후에 수행되는 곳입니다.
        override fun onPostExecute(result: Array<Any?>) {
            // 리턴받은 결과가 null이 아니라면 메세지로 만들어줍니다.
            if (result != null) {
                makeMessage(result[0] as Result, result[1] as String)
            }
        }

    }

    // 챗봇의 답장을 받아와서 메세지로 만들고 채팅창에 띄웁니다.
    fun makeMessage(result: Result?, messageId: String?) {

        // 다이얼로그 플로우 콘솔에 직접 생성한 Intent가 있을때
        // 종류에 따라 필터링을 하는 겁니다.
        when (result?.metadata?.intentName) {

            // 메타데이터의 인텐트 이름이 "Weather"일때 (날씨)
            "Weather" -> {
                if (q_text_flag == true) {
                    //Weather라는 인텐트를 다이얼로그 플로우에 생성했습니다.
                    //Training phrases(챗봇 훈련어구)에 서울, 용인 등 지역을 등록을 해놨습니다.
                    // 챗봇한테 서울 등을 물어보면 해당 지역의 정식명칭이 파라미터 geo-city로 넘어옵니다.
                    var city = result.parameters["geo-city"]
                    if (city == null) {
                        // 사용자가 도시를 언급안했을시 기본값으로 서울의 날씨를 알려줍니다.
                        weatherMessage("서울특별시", messageId)
                    } else {
                        weatherMessage(city.asString, messageId)
                    }
                } else {

                }
            }

            "Default Fallback Intent" -> {
                // 다이얼로그플로우에 인텐트가 지정되지 않은 질문에 대한 응답이 이뤄지는 부분입니다.
                // 즉 사진, 텍스트로 어린이의 질문이 들어왔을시 ML Kit을 통해 분석을 하고 그 결과로 응답 메세지를 구성해야합니다.

                // Using ML Kit here
                // is_ansered가 false인 항목을 DB에서 가져오고 그걸 API에 입력값으로..응답 메세지 생성하고 답변 처리하고 DB에 올리기

                // is_ansered가 false인 질문을 가져오고 거기에 답변 처리하고 DB에 올리기

                if (q_text_flag == true) {
                    var speech = result?.fulfillment?.speech
                    // is_ansered가 false인 질문을 가져오고 거기에 답변 처리하고 DB에 올리기
                    val subject = "기타"
                    do_answer(speech, subject, messageId)
                } else {
                    // 질문이 사진
                }

            }

            "Default Welcome Intent" -> {
                if (q_text_flag == true) {
                    // 다이얼로그 플로우에 기본 인사 어구로 등록된 말이 출력됩니다.
                    var speech = result?.fulfillment?.speech

                    // is_ansered가 false인 질문을 가져오고 거기에 답변 처리하고 DB에 올리기
                    val subject = "담임"
                    do_answer(speech, subject, messageId)

                } else {

                }
            }
        }
    }

    // 날씨에 관련된 챗봇의 답장을 만듭니다.
    fun weatherMessage(city: String, messageId: String?) {

        // city 파라메터로 넘어온걸 url에 붙입니다.
        // 맨끝의 Units-metric은 날씨를 섭씨로 받아오는 옵션입니다.
        // 발급 받은 api 키는 86299d89d3158e76da1eeb77522844b0
        var weatherUrl =
            "https://api.openweathermap.org/data/2.5/forecast?id=524901&APPID=86299d89d3158e76da1eeb77522844b0&q=" + city + "&units=metric"
        // openWeatherMap API에 해당 url을 넘겨주고 날씨 정보를 요청합니다.
        var request = Request.Builder().url(weatherUrl).build()

        // request를 OkHttp로 호출합니다
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                // 요청이 실패했을 시 실행됩니다.
            }

            override fun onResponse(call: Call?, response: Response?) {
                // 결과로 넘어온 json 값을 담습니다.
                var result = response?.body()?.string()

                // json 값을 weatherDTO 오브젝트로 만듭니다.
                var weatherDTO = Gson().fromJson(result, WeatherDTO::class.java)
                for (item in weatherDTO.list!!) {
                    // 데이터 수신 시간을 컴퓨터가 이해할 수 있는 형태로 바꿉니다.
                    var weatherItemUnixTime = weatherDataFormatFromHumanText.parse(item.dt_txt).time
                    if (weatherItemUnixTime > System.currentTimeMillis()) {
                        // json을 파싱해서 가져온 시간이 현재 시간보다 미래일 경우에 가져다 씁니다.

                        // 온도
                        var temp = item.main?.temp
                        // 날씨 상태
                        var description = item.weather!![0].description
                        // 시간
                        var time = weatherDataFormatToHumanText.format(weatherItemUnixTime)
                        // 습도
                        var humidity = item.main?.humidity
                        // 풍속
                        var speed = item.wind?.speed
                        // 3시간 동안 침적되는 비의 양
                        var three_hour = item.rain?.three_hour

                        var message =
                            time + " 기준 " + city + "의 온도는 " + temp + "도 란다 " + "\n" + "습도는 " + humidity + "% 구 " + "바람의 속도는 " + speed + "meter/sec " + "야! " + "\n" + " 공부하기 좋은 날씨구나"

                        runOnUiThread {

                            // is_ansered가 false인 질문을 가져오고 거기에 답변 처리하고 DB에 올리기
                            val subject = "과학"
                            do_answer(message, subject, messageId)

                        }
                        break // 현재 시간에서 가장 가까운 미래의 날씨만 가져오면 됩니다. 계속 앞의 미래를 가져올 필요는 없으니 break 해줍니다.
                    }
                }
            }
        })
    }
}

