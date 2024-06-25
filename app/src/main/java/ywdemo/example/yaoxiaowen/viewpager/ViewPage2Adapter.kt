package ywdemo.example.yaoxiaowen.viewpager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ywdemo.example.yaoxiaowen.R

class ViewPage2Adapter : RecyclerView.Adapter<ViewPage2Adapter.ViewHolder> {
    private var mData: List<ViewPageData>

    constructor(data: List<ViewPageData>) : super() {
        mData = data
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_view_page2_singal_page, parent, false)
        return  ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTv.setText("第$position 个page")
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    class ViewHolder : RecyclerView.ViewHolder {

        val mTv:TextView

        constructor(itemView: View) : super(itemView) {
            mTv = itemView.findViewById(R.id.pagetv)
        }
    }
}