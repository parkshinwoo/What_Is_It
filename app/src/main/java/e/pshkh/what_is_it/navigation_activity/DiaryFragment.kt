package e.pshkh.what_is_it.navigation_activity

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import e.pshkh.what_is_it.R
import kotlinx.android.synthetic.main.activity_diary.view.*

class DiaryFragment : Fragment() {
    var diaryAdapter: DiaryRecyclerAdapter = DiaryRecyclerAdapter()
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val diaryView = inflater!!.inflate(R.layout.activity_diary, container, false)
        diaryView.DiaryRecyclerView.adapter = diaryAdapter

        return diaryView
    }

    override fun onResume() {
        super.onResume()
        diaryAdapter.notifyDataSetChanged()
    }
}
