package com.wl.turbidimetric.matchingargs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.wl.turbidimetric.R
import com.wl.turbidimetric.ex.scaleStr
import com.wl.turbidimetric.model.CurveModel


class MatchingConfigSampleAdapter @JvmOverloads constructor(
    private val context: Context,
    private val items: MutableList<String>?
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
        var root: View? = view
        if (root == null) {
            root = LayoutInflater.from(context).inflate(R.layout.item_matching_sample, null)
        }
        root?.let {
            val tvName = root.findViewById<TextView>(R.id.tvName)
            tvName.setText(items?.get(position) ?: "")
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
        var root: View? = view
        if (root == null) {
            root = LayoutInflater.from(context).inflate(R.layout.item_matching_sample, null)
        }
        root?.let {
            val tvName = root.findViewById<TextView>(R.id.tvName)
            tvName.setText(items?.get(position) ?: "")
        }
        return root
    }
}
