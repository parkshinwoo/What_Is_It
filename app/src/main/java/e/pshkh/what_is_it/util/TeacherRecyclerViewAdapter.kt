package e.pshkh.what_is_it.util

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import e.pshkh.what_is_it.R
import e.pshkh.what_is_it.data_transfer_object.DiaryBookDTO
import kotlinx.android.synthetic.main.recyclerview_item_design_teacher.view.*
import java.text.SimpleDateFormat
import java.util.*

// 다른 fragment들 처럼 activity의 하위화면으로 붙는것이 아니라
// recyclerview만 잡으면 되기에 fragment로 만들지 않았습니다.
// TeacherActivity에서 TeacherRecyclerViewAdapter를 잡습니다.

// 메세지를 담는 데이터 클래스입니다.
// 내가 보낸 메세지인지 챗봇이 내게 보낸 메세지인지를 체크하는 변수 isMyMesage
// 메세지 내용을 담는 message
data class MessageDTO(
    var isMyMessage:Boolean? = null,
    var message:String? = null,
    var uri: Intent? = null,
    var target1: String? = null, // target_document_layer1, 질문한 어린이의 uid
    var target2: String? = null  // target_document_layer2, 답변 메세지의 id
)

class TeacherRecyclerViewAdapter(messageDTOs:ArrayList<MessageDTO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    var messageDTOs = messageDTOs

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        var view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_item_design_teacher, parent, false)

        return CustomViewHolder(view)

    }

    private inner class CustomViewHolder(view: View?) : RecyclerView.ViewHolder(view!!)

    override fun getItemCount(): Int {
        return messageDTOs.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (messageDTOs[position].isMyMessage!!) {

            if(messageDTOs[position].uri == null){
                // 내가 챗봇에게 보낸 텍스트 메세지일 경우에는 나의 말풍선만 보이게 하고
                // 챗봇의 말풍선은 가려야합니다.
                holder.itemView.imagebubble.visibility = View.GONE
                holder.itemView.right_chatbubble.visibility = View.VISIBLE
                holder.itemView.right_chatbubble.text = messageDTOs[position].message
                holder.itemView.left_chatbubble.visibility = View.INVISIBLE
            } else{
                // 내가 챗봇에게 사진을 보냈을 경우
                holder.itemView.left_chatbubble.visibility = View.GONE
                holder.itemView.right_chatbubble.visibility = View.GONE

                var photoUri = messageDTOs[position].uri?.data
                holder.itemView.imagebubble.visibility = View.VISIBLE
                holder.itemView.imagebubble.setImageURI(photoUri)
            }
        } else {
            // 챗봇이 내게 보낸 메세지일 경우
            holder.itemView.imagebubble.visibility = View.GONE
            holder.itemView.left_chatbubble.visibility = View.VISIBLE
            holder.itemView.left_chatbubble.text = messageDTOs[position].message
            holder.itemView.right_chatbubble.visibility = View.INVISIBLE
        }

        // 다이어리에 올리는 이벤트 발생
        holder.itemView.left_chatbubble.setOnLongClickListener {
            if(messageDTOs[position].target1 != null && messageDTOs[position].target2 != null){
                FirebaseFirestore.getInstance()!!.collection("StudyRoom").document(messageDTOs[position].target1!!).collection("message").document(messageDTOs[position].target2!!).addSnapshotListener { documentSnapshot, firebaseFirestoreException ->

                    val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
                    val timestamp = System.currentTimeMillis()

                    var diary_book_id : String?
                    var diary_id : String?

                    diary_book_id = messageDTOs[position].target1
                    diary_id = messageDTOs[position].target1 + timestamp.toString()

                    var is_photo:Boolean? = true
                    if(messageDTOs[position].uri == null){
                        is_photo = false
                    }

                    var owner_id = messageDTOs[position].target1
                    var userEmail:String? = null
                    var question:String? = null
                    var answer:String? = null
                    var subject:String? = null

                    for(snapshot in documentSnapshot.data){
                        if(snapshot.key.equals("question")){
                            question = snapshot.value.toString()
                        }
                        if(snapshot.key.equals("message_content")){
                            answer = snapshot.value.toString()
                        }

                        if(snapshot.key.equals("subject")){
                            subject = snapshot.value.toString()
                        }
                    }

                    FirebaseFirestore.getInstance().collection("users").whereEqualTo("uid", owner_id!!).get().addOnCompleteListener {
                        if (it.isSuccessful){
                            for (document in it.result){
                                userEmail = document.data["userEmail"].toString()
                            }
                        }
                    }

                    var diary = DiaryBookDTO.Diary()

                    diary.diary_id = diary_id
                    diary.answer = answer
                    diary.date = date
                    diary.question = question
                    diary.timestamp = timestamp
                    diary.owner_id = owner_id
                    diary.is_photo = is_photo
                    diary.subject = subject
                    diary.userEmail = userEmail

                    FirebaseFirestore.getInstance().collection("DiaryBook").document(diary_book_id!!).collection("diary").document(diary_id).set(diary)

                }
            }
            true
        }
    }
}