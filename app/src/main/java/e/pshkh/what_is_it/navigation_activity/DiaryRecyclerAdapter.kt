package e.pshkh.what_is_it.navigation_activity

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import e.pshkh.what_is_it.R
import e.pshkh.what_is_it.data_transfer_object.DiaryBookDTO
import e.pshkh.what_is_it.diaryMoreViewActivity
import kotlinx.android.synthetic.main.diary_card.view.*
import java.text.SimpleDateFormat

class DiaryRecyclerAdapter(val context: Context?, val emptyMsgView: LinearLayout) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var firestore: FirebaseFirestore? = null
    private var diarySnapshot: ListenerRegistration? = null
    private var storage: FirebaseStorage? = null
    private var auth: FirebaseAuth? = null
    private val diaryList: ArrayList<DiaryBookDTO.Diary> = ArrayList()

    init {
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        diarySnapshot = firestore!!.collection("DiaryBook").document(auth?.currentUser?.uid!!).collection("diary")
            .orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                var item: DiaryBookDTO.Diary?
                diaryList.clear()
                try{
                    for (snapshot in querySnapshot!!.documents) {
                        item = snapshot.toObject(DiaryBookDTO.Diary::class.java)
                        diaryList.add(
                            DiaryBookDTO.Diary(
                                item?.diary_id,
                                item?.timestamp,
                                item?.date,
                                item?.is_photo,
                                item?.question,
                                item?.answer,
                                item?.subject,
                                item?.owner_id,
                                item?.userEmail
                            )
                        )
                    }
                }catch(KotlinNullPointerException : NullPointerException){
                    print("일기장 널포인터 예외")
                }

                if (diaryList.isNotEmpty())
                    emptyMsgView.visibility = View.GONE
                else
                    emptyMsgView.visibility = View.VISIBLE

                notifyDataSetChanged()
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        var view = LayoutInflater.from(parent!!.context).inflate(R.layout.diary_card, parent, false)

        return DiaryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return diaryList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.diaryTitle.text = diaryList[position].subject
        holder.itemView.diaryContent.text = diaryList[position].answer
        holder.itemView.diaryDate.text =
            SimpleDateFormat("yyyy년MM월dd일 aa hh:mm:ss").format(diaryList[position].timestamp)
        //holder.itemView.diarySubject.text = diaryList[position].subject
        if (!diaryList[position].is_photo!!)
            holder.itemView.diaryThumb.visibility = View.GONE
        holder.itemView.diaryCardView.setOnClickListener() { view ->
            var i = Intent(context, diaryMoreViewActivity::class.java)
            i.putExtra("title", diaryList[position].subject)
            i.putExtra("date", SimpleDateFormat("yyyy년MM월dd일 aa hh:mm:ss").format(diaryList[position].timestamp))
            i.putExtra("content", diaryList[position].answer)
            i.putExtra("is_photo", diaryList[position].is_photo)
            i.putExtra("diaryId", diaryList[position].diary_id)
            i.putExtra("ownerId", diaryList[position].owner_id)
            context?.startActivity(i)
        }
    }

    inner class DiaryViewHolder(view: View?) : RecyclerView.ViewHolder(view) {

    }

}