package e.pshkh.what_is_it.data_transfer_object

data class DiaryBookDTO(var diary_book_id: String?="", // 일기장 고유 식별자로, 어린이의 uid
                        var timestamp: Long? = 0
){
    data class Diary(var diary_id: String?="", // 어린이의 uid 뒤에 타임스탬프를 붙인형태
                       var timestamp: Long? = 0,
                       var date: String?="", // 실제 날짜
                       var is_photo: Boolean?=false, // 일기장을 이루는 메인 내용(어린이의 질문)이 사진인지, 텍스트인지 체크
                       var question: Any?="", // 어린이의 질문
                       var answer: String?="", // 선생님의 답변
                       var subject: String?="", // 어떤 과목인지
                       var owner_id: String?="", // 다이어리 주인 어린이의 uid
                       var userEmail: String?="" // 다이어리 주인 어린이의 이메일
    )
}