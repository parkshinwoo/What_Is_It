package e.pshkh.what_is_it.data_transfer_object

data class DiaryBookDTO(var diary_book_id: String?=null, // 일기장 고유 식별자로, 어린이의 uid
                        var timestamp: Long? = null
){
    data class Diary(var diary_id: String?=null, // 어린이의 uid 뒤에 타임스탬프를 붙인형태
                       var timestamp: Long? = null,
                       var date: String?=null, // 실제 날짜
                       var is_photo: Boolean?=false, // 일기장을 이루는 메인 내용(어린이의 질문)이 사진인지, 텍스트인지 체크
                       var question: String?=null, // 어린이의 질문
                       var answer: String?=null, // 선생님의 답변
                       var subject: String?=null, // 어떤 과목인지
                       var owner_id: String?=null, // 다이어리 주인 어린이의 uid
                       var userEmail: String?=null // 다이어리 주인 어린이의 이메일
    )
}