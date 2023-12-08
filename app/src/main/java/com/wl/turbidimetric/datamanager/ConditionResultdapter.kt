package com.wl.turbidimetric.datamanager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.SpinnerAdapter
import android.widget.TextView
import com.wl.turbidimetric.R


class ConditionResultdapter @JvmOverloads constructor(
    private val context: Context,
    private val items: Array<String>?
) :
    BaseAdapter(), SpinnerAdapter {
    override fun getCount(): Int {
        return items!!.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View? {
        var convertView = view
        convertView =
            LayoutInflater.from(context).inflate(R.layout.item_select_result, parent, false)
        val tvResult = convertView.findViewById<TextView>(R.id.tv)
        tvResult.text = items?.get(position) ?: ""
        return convertView
    }

    override fun getItemViewType(position: Int): Int {
        return 1
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return false
    }


    override fun getDropDownView(position: Int, view: View?, parent: ViewGroup): View? {
        var convertView = view
        convertView =
            LayoutInflater.from(context).inflate(R.layout.item_select_result, parent, false)
        val tvResult = convertView.findViewById<TextView>(R.id.tv)
        tvResult.text = items?.get(position) ?: ""
        return convertView
    }
}
