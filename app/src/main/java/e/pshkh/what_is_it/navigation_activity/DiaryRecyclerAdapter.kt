package e.pshkh.what_is_it.navigation_activity

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import e.pshkh.what_is_it.data_transfer_object.DiaryBookDTO

class DiaryRecyclerAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    val diaryList : ArrayList<DiaryBookDTO> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    inner class DiaryViewHolder(view: View?) : RecyclerView.ViewHolder(view){

    }

}