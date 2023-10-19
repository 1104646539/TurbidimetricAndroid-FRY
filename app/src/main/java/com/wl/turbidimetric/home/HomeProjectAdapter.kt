package com.wl.turbidimetric.home

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.scale
import com.wl.turbidimetric.model.ProjectModel
import com.wl.wllib.LogToFile.i


class HomeProjectAdapter @JvmOverloads constructor(
    private val context: Context,
    private val items: MutableList<ProjectModel>?
) :
    BaseAdapter() {

    override fun getCount(): Int {
        return items!!.size
    }

    override fun getItem(position: Int): Any {
        return items!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View? {
        var root: View? = view;
        if (root == null) {
            root = LayoutInflater.from(context).inflate(R.layout.item_select_project, null)
        }
        root?.let {
            val tvA1 = root.findViewById<TextView>(R.id.tvA1)
            val tvA2 = root.findViewById<TextView>(R.id.tvA2)
            val tvX0 = root.findViewById<TextView>(R.id.tvX0)
            val tvP = root.findViewById<TextView>(R.id.tvP)
            val tvTime = root.findViewById<TextView>(R.id.tvTime)
            val tvNO = root.findViewById<TextView>(R.id.tvNO)

            tvA1?.text = "f0:" + items?.get(position)?.f0?.scale(6).toString()
            tvA2?.text = "f1:" + items?.get(position)?.f1?.scale(6).toString()
            tvX0?.text = "f2:" + items?.get(position)?.f2?.scale(6).toString()
            tvP?.text = "f3:" + items?.get(position)?.f3?.scale(6).toString()
            tvTime?.text = "时间:" + items?.get(position)?.createTime
            tvNO?.text =
                "序号:" + if (items?.get(position)?.reagentNO.isNullOrEmpty()) "-" else items?.get(
                    position
                )?.reagentNO
        }


        return root
    }

    override fun getItemViewType(position: Int): Int {
        return 1
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return items.isNullOrEmpty()
    }


    override fun getDropDownView(position: Int, view: View?, parent: ViewGroup): View? {
        var root: View? = view;
        if (root == null) {
            root = LayoutInflater.from(context).inflate(R.layout.item_select_project, null)
        }
        root?.let {
            val tvA1 = root.findViewById<TextView>(R.id.tvA1)
            val tvA2 = root.findViewById<TextView>(R.id.tvA2)
            val tvX0 = root.findViewById<TextView>(R.id.tvX0)
            val tvP = root.findViewById<TextView>(R.id.tvP)
            val tvTime = root.findViewById<TextView>(R.id.tvTime)
            val tvNO = root.findViewById<TextView>(R.id.tvNO)

            tvA1?.text = "f0:" + items?.get(position)?.f0?.scale(6).toString()
            tvA2?.text = "f1:" + items?.get(position)?.f1?.scale(6).toString()
            tvX0?.text = "f2:" + items?.get(position)?.f2?.scale(6).toString()
            tvP?.text = "f3:" + items?.get(position)?.f3?.scale(6).toString()
            tvTime?.text = "时间:" + items?.get(position)?.createTime
            tvNO?.text =
                "序号:" + if (items?.get(position)?.reagentNO.isNullOrEmpty()) "-" else items?.get(
                    position
                )?.reagentNO
        }
        return root
    }
}
