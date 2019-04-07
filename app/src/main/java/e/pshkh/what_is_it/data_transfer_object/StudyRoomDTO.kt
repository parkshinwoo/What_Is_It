package e.pshkh.what_is_it.data_transfer_object

data class StudyRoomDTO(var study_room_id: String?=null, // 어린이의 uid
                        var timestamp: Long? = null
){
    data class Message(var message_id: String?=null, // 어린이는 uid 뒤에 타임스탬프를 붙인형태, 선생님은 0 뒤에 타임스탬프를 붙인형태
                       var timestamp: Long? = null,
                       var date: String?=null, // 실제 날짜
                       var is_student: Boolean?=true, // 어린이가 보낸 메세지인지 체크
                       var is_answered: Boolean?=false, // 질문 메세지일 경우, 답변이 됬는지 체크
                       var is_photo: Boolean?=false, // 질문 메세지의 형태가 사진인지, 텍스트인지 체크
                       var is_scraped: Boolean?=false, // 답변 메세지일 경우 다이어리로 스크랩이 됬는지 체크. 향후 다이어리 중복 업로드 방지를 위함
                       var message_content: String?=null, // 메세지의 내용. 텍스트, 사진일 경우엔 사진의 uri. 어린의 질문 or 선생님의 답변
                       var question: String?=null, // 어린이의 질문만 담음 for 다이어리
                       var subject: String?=null, // 응답 메세지가 어떤 과목 선생님의 답변인지 체크 (과목 체크)
                       var question_id: String?=null, // 질문 메세지의 id (응답 메세지와 짝을 이룸)
                       var answer_id: String?=null, // 응답 메세지의 id (질문 메세지와 짝을 이룸)
                       var owner_id: String?=null // 메세지 주인의 uid
    )
}