package e.pshkh.what_is_it.navigation_activity

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import e.pshkh.what_is_it.R
import kotlinx.android.synthetic.main.activity_diary.*
import kotlinx.android.synthetic.main.activity_diary.view.*

class DiaryFragment : androidx.fragment.app.Fragment() {
    private lateinit var diaryAdapter: DiaryRecyclerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val diaryView = inflater!!.inflate(R.layout.activity_diary, container, false)

        diaryAdapter = DiaryRecyclerAdapter(context, diaryView.emptyMsg)
        diaryView.DiaryRecyclerView.adapter = diaryAdapter
        return diaryView
    }

    override fun onResume() {
        super.onResume()
        diaryAdapter?.notifyDataSetChanged()
    }
}
