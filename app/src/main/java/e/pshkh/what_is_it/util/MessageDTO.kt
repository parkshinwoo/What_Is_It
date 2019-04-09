package e.pshkh.what_is_it.util

import android.content.Intent

// 메세지를 담는 데이터 클래스입니다.
// 내가 보낸 메세지인지 챗봇이 내게 보낸 메세지인지를 체크하는 변수 isMyMesage
// 메세지 내용을 담는 message
data class MessageDTO(
    var isMyMessage:Boolean? = null,
    var message:String? = null,
    var uri: Intent? = null,
    var target1: String? = null, // target_document_layer1, 질문한 어린이의 uid
    var target2: String? = null,  // target_document_layer2, 답변 메세지의 id
    var answered_question_id: String? = null // 막 답변이 된 어린이 질문의 id
)

