package e.pshkh.what_is_it.navigation_activity

import ai.api.AIConfiguration
import ai.api.AIDataService
import ai.api.model.AIRequest
import ai.api.model.Result
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import e.pshkh.what_is_it.R
import e.pshkh.what_is_it.data_transfer_object.StudyRoomDTO
import e.pshkh.what_is_it.data_transfer_object.WeatherDTO
import e.pshkh.what_is_it.util.MessageDTO
import e.pshkh.what_is_it.util.TeacherRecyclerViewAdapter
import kotlinx.android.synthetic.main.activity_teacher.*
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TeacherActivity : AppCompatActivity() {

    var messageDTOs = arrayListOf<MessageDTO>()

    // 다이얼로그 플로우와 통신하기 위한 클래스입니다.
    var aiDataService : AIDataService? = null

    // 챗봇(다이얼로그 플로우)과 날짜를 주고 받으려면 서로 이해할 수 있는 형식이어야 합니다.

    // 사람(HumanText)이 이해 할 수 있는 날짜를 컴퓨터가 이해할 수 있는 날짜 형식으로 맞추기 위한 값입니다.
    var dateFormatFromHumanText = SimpleDateFormat("yyyy-MM-dd")

    // 날씨 API(OpenWeatherMap API)에게 넘겨주기 위한 날짜 형식
    var weatherDataFormatFromHumanText = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

    // 날씨 API에게 넘겨받은 날짜를 사람이 이해하기 쉬운 형식으로 바꾸기
    var weatherDataFormatToHumanText = SimpleDateFormat("MM월 dd일 hh시")

    // 안드로이드 디바이스의 앨범에서 선택된 사진이 맞는지를 확인하는 코드 값입니다.
    // 코드의 하단부 onActivityResult에서 사용됩니다.
    val PICK_IMAGE_FROM_ALBUM = 0

    // 파이어베이스 스토리지(저장소) 접근을 위한 값입니다.
    var storage : FirebaseStorage? = null

    // 어린이가 올린 사진의 주소를 담습니다.
    var photoUri : Uri? = null

    // 어린이가 올린 텍스트를 담습니다.
    var question : String? = "이게 뭐야?"
    // 질문이 사진인지 글인지 체크
    var q_text_flag : Boolean? = true // true면 텍스트

    // 파이어베이스 사용자 인증을 위한 값입니다. 이 값을 통해 사용자의 uid, 이메일 주소 등에 접근합니다.
    var auth : FirebaseAuth? = null

    // 파이어베이스 스토어(데이터베이스) 접근을 위한 값입니다.
    var firestore: FirebaseFirestore? = null

    var teacherSnapshot : ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher)

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 다이얼로그 플로우 v1 API "개발자" 키 값을 넣어줍니다.
        // v2 API는 안드로이드를 아직 지원 안합니다.
        // 이 앱을 배포할 시엔 "클라이언트" 키 값을 넣으면 됩니다.
        var config = AIConfiguration("1d630233cd00435d813e563a28cb5373", AIConfiguration.SupportedLanguages.Korean)
        // 설정한 값으로 aiDataService 객체를 생성합니다.
        aiDataService = AIDataService(config)

        // 텍스트로 질문할 경우 발생하는 이벤트
        chat.setOnClickListener {
            if(!TextUtils.isEmpty(chatText.text)){

                question = chatText.text.toString()

                // 챗봇에게 보낼 말을 입력하는 창이 빈 문자열이 아닐 경우에 메세지를 보낼 수 있게 해줍니다.
                messageDTOs.add(MessageDTO(true, question, null, null, null, null))

                // 내가 전송한 메세지가 recyclerview 화면에 뿌려지게끔 새로고침 합니다.
                recyclerview.adapter?.notifyDataSetChanged()

                // 끝 위치로 이동합니다.
                recyclerview.smoothScrollToPosition(messageDTOs.size-1)

                // DB에 메세지 올리기
                val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
                // 현재 시스템 시간
                val timestamp = System.currentTimeMillis()

                // 공부방에 추가할 메세지 생성
                var message = StudyRoomDTO.Message()
                message.timestamp = timestamp
                message.date = date
                message.message_content = question
                message.message_id = auth?.currentUser?.uid.toString() + timestamp.toString()
                message.question_id = auth?.currentUser?.uid.toString() + timestamp.toString()
                message.owner_id = auth?.currentUser?.uid.toString()
                message.question = question

                // 파이어베이스 DB의 공부방 하위에 메세지 저장
                firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message").document(message.message_id!!).set(message)

                // 챗봇(다이얼로그 플로우)와 통신하는 쓰레드를 실행합니다.
                TalkAsyncTask().execute(question)
                // 메세지 입력창을 빈문자열로 초기화 해줍니다.
                chatText.setText("")
            }
        }

        // 사진으로 질문할 경우 발생하는 이벤트
        chatImage.setOnClickListener {
            q_text_flag = false

            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

            // 챗봇(다이얼로그 플로우)와 통신하는 쓰레드를 실행합니다.
            TalkAsyncTask().execute(question)
            // 메세지 입력창을 빈문자열로 초기화 해줍니다.
            chatText.setText("")
        }
    }

    override fun onResume() {
        super.onResume()
        recyclerview.adapter = TeacherRecyclerViewAdapter(messageDTOs)
        recyclerview.layoutManager = LinearLayoutManager(this)
    }

    override fun onStop() {
        super.onStop()
        teacherSnapshot?.remove()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM){
            if(resultCode == Activity.RESULT_OK){
                photoUri = data?.data

                // 화면에 표시될 메세지에 사진의 Uri를 담습니다.
                messageDTOs.add(MessageDTO(true, null, data, null, null, null)) // intent 타입으로 데이터 넘김

                // 어린이가 올린 사진이 recyclerview 화면에 뿌려지게끔 새로고침 합니다.
                recyclerview.adapter?.notifyDataSetChanged()

                // 끝 위치로 이동합니다.
                recyclerview.smoothScrollToPosition(messageDTOs.size-1)

                photoUpload()
            }
            else if(resultCode == Activity.RESULT_CANCELED){
                finish()
            }
        }
    }

    // 이미지를 파이어베이스 스토리지 및 스토어에 업로드하는 함수입니다.
    fun photoUpload() {

        // 사진이 업로드된 날짜 및 시각을 담는 변수입니다.
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
        // 현재 시스템 시간
        val timestamp = System.currentTimeMillis()
        // 사진의 파일 형식은 png로 하며 파일명은 사진이 업로드된 시각으로 합니다.
        val imageFileName = "PNG_"+date +"_.png"
        // 파이어베이스 스토리지에 images라는 디렉터리를 생성하고 그곳에 사진을 업로드합니다.
        val storageRef = storage?.reference?.child("images")?.child(imageFileName)

        // 파이어베이스 스토리지에 이미지 올리기
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {taskSnapshot ->

            // 업로드된 이미지 주소를 가져오기 파일 경로
            var uri = taskSnapshot.downloadUrl

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

            // 파이어베이스 DB의 공부방 하위에 메세지 저장
            firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message").document(message.message_id!!).set(message)

        }
    }

    // 다이얼로그 플로우와 통신하는 쓰레드를 만들어줍니다.
    inner class TalkAsyncTask : AsyncTask<String, Void, Result>(){

        // 백그라운드에서 수행될 일을 작성합니다.
        override fun doInBackground(vararg params: String?): Result {

            // 사용자가 전송한 메세지를 다이얼로그 플로우로 넘겨주는 쿼리입니다.
            var aiRequest = AIRequest()
            aiRequest.setQuery(params[0])

            // 결과를 리턴합니다.
            return aiDataService?.request(aiRequest)!!.result
        }

        // doInBackground 이후에 수행되는 곳입니다.
        override fun onPostExecute(result: Result?) {
            // 리턴받은 결과가 null이 아니라면 메세지로 만들어줍니다.
            if(result != null){
                makeMessage(result)
            }
        }

    }

    // 챗봇의 답장을 받아와서 메세지로 만들고 채팅창에 띄웁니다.
    fun makeMessage(result: Result?){

        // 다이얼로그 플로우 콘솔에 직접 생성한 Intent가 있을때
        // 종류에 따라 필터링을 하는 겁니다.
        when(result?.metadata?.intentName){

            // 메타데이터의 인텐트 이름이 "Weather"일때 (날씨)
            "Weather" -> {
                if(q_text_flag == true){
                    //Weather라는 인텐트를 다이얼로그 플로우에 생성했습니다.
                    //Training phrases(챗봇 훈련어구)에 서울, 용인 등 지역을 등록을 해놨습니다.
                    // 챗봇한테 서울 등을 물어보면 해당 지역의 정식명칭이 파라미터 geo-city로 넘어옵니다.
                    var city = result.parameters["geo-city"]
                    if (city == null){
                        // 사용자가 도시를 언급안했을시 기본값으로 서울의 날씨를 알려줍니다.
                        weatherMessage("서울특별시")
                    } else{
                        weatherMessage(city.asString)
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

                if(q_text_flag == true){
                    // 질문이 텍스트
                } else {
                    // 질문이 사진
                }

            }

            "Default Welcome Intent" -> {
                if(q_text_flag == true){
                    // 다이얼로그 플로우에 기본 인사 어구로 등록된 말이 출력됩니다.
                    var speech = result?.fulfillment?.speech

                    // is_ansered가 false인 질문을 가져오고 거기에 답변 처리하고 DB에 올리기
                    val subject = "담임"
                    do_answer(speech, subject)

                } else {

                }
            }
        }
    }

    // 날씨에 관련된 챗봇의 답장을 만듭니다.
    fun weatherMessage(city:String){

        // city 파라메터로 넘어온걸 url에 붙입니다.
        // 맨끝의 Units-metric은 날씨를 섭씨로 받아오는 옵션입니다.
        // 발급 받은 api 키는 86299d89d3158e76da1eeb77522844b0
        var weatherUrl = "https://api.openweathermap.org/data/2.5/forecast?id=524901&APPID=86299d89d3158e76da1eeb77522844b0&q="+city+"&units=metric"
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
                for(item in weatherDTO.list!!){
                    // 데이터 수신 시간을 컴퓨터가 이해할 수 있는 형태로 바꿉니다.
                    var weatherItemUnixTime = weatherDataFormatFromHumanText.parse(item.dt_txt).time
                    if(weatherItemUnixTime > System.currentTimeMillis()){
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

                        var message = time + " 기준 " + city + "의 온도는 " + temp + "도 란다 " + "\n" + "습도는 " + humidity + "% 구 " + "바람의 속도는 " + speed + "meter/sec " + "야! " + "\n" + " 공부하기 좋은 날씨구나"

                        runOnUiThread {

                            // is_ansered가 false인 질문을 가져오고 거기에 답변 처리하고 DB에 올리기
                            val subject = "과학"
                            do_answer(message, subject)

                        }
                        break // 현재 시간에서 가장 가까운 미래의 날씨만 가져오면 됩니다. 계속 앞의 미래를 가져올 필요는 없으니 break 해줍니다.
                    }
                }
            }
        })
    }

    fun do_answer(answer : String?, subject : String?){

        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
        val timestamp = System.currentTimeMillis()
        val answer_id = 0.toString() + timestamp.toString()

        val not_answered_question : ArrayList<StudyRoomDTO.Message>
        not_answered_question = ArrayList()

        teacherSnapshot = firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message").orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if(querySnapshot == null) return@addSnapshotListener

            not_answered_question.clear()

            for(snapshot in querySnapshot.documents!!){
                if(snapshot.toObject(StudyRoomDTO.Message::class.java).is_answered == false){

                    val not_answered_message_id = snapshot.toObject(StudyRoomDTO.Message::class.java).message_id

                    var map1 = mutableMapOf<String, Any>()
                    map1["_answered"] = true

                    var map2 = mutableMapOf<String, Any>()
                    map2["answer_id"] = answer_id

                    var map3 = mutableMapOf<String, Any>()
                    map3["subject"] = subject.toString()

                    firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message").document(not_answered_message_id!!).update(map1)?.addOnCompleteListener {
                        task ->
                            if(task.isSuccessful){

                            }
                    }

                    firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message").document(not_answered_message_id!!).update(map2)?.addOnCompleteListener {
                            task ->
                        if(task.isSuccessful){

                        }
                    }

                    firestore!!.collection("StudyRoom").document(auth?.currentUser?.uid!!).collection("message").document(not_answered_message_id!!).update(map3)?.addOnCompleteListener {
                            task ->
                        if(task.isSuccessful){

                        }
                    }

                    // 공부방에 추가할 답변 메세지 생성
                    var message = StudyRoomDTO.Message()

                    message.subject = subject
                    message.timestamp = timestamp
                    message.date = date
                    message.message_content = answer // 선생님의 답변 내용을 담음
                    message.is_answered = true
                    message.message_id = answer_id
                    message.question_id = not_answered_message_id
                    message.answer_id = answer_id
                    message.owner_id = 0.toString()
                    message.is_student = false
                    message.question = snapshot.toObject(StudyRoomDTO.Message::class.java).message_content // for diary 어린이의 질문 내용을 담음

                    var target_document_layer1 = auth?.currentUser?.uid!!
                    var target_document_layer2 = answer_id!!

                    // 파이어베이스 DB의 공부방 하위에 답변 메세지 저장
                    firestore!!.collection("StudyRoom").document(target_document_layer1).collection("message").document(target_document_layer2).set(message)

                    if(messageDTOs.contains(MessageDTO(false, answer, null, target_document_layer1, target_document_layer2, not_answered_message_id))){
                        // 중복 방지
                    }else{
                        messageDTOs.add(MessageDTO(false, answer, null, target_document_layer1, target_document_layer2, not_answered_message_id))
                        recyclerview.adapter?.notifyDataSetChanged()
                        recyclerview.smoothScrollToPosition(messageDTOs.size - 1)
                    }
                }
            }

        }
    }
}
