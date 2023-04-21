package com.wl.turbidimetric.home

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.SpinnerAdapter
import android.widget.TextView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.model.ProjectModel


class HomeProjectAdapter @JvmOverloads constructor(
    private val context: Context,
    private val items: MutableList<ProjectModel>?
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
            LayoutInflater.from(context).inflate(R.layout.item_select_project, parent, false)
        val tvA1 = convertView.findViewById<TextView>(R.id.tvA1)
        val tvA2 = convertView.findViewById<TextView>(R.id.tvA2)
        val tvX0 = convertView.findViewById<TextView>(R.id.tvX0)
        val tvP = convertView.findViewById<TextView>(R.id.tvP)
        tvA1.text = "A1:" + items?.get(position)?.a1.toString()
        tvA2.text = "A2:" + items?.get(position)?.a2.toString()
        tvX0.text = "X0:" + items?.get(position)?.x0.toString()
        tvP.text = "P:" + items?.get(position)?.p.toString()
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
            LayoutInflater.from(context).inflate(R.layout.item_select_project, parent, false)
        val tvA1 = convertView.findViewById<TextView>(R.id.tvA1)
        val tvA2 = convertView.findViewById<TextView>(R.id.tvA2)
        val tvX0 = convertView.findViewById<TextView>(R.id.tvX0)
        val tvP = convertView.findViewById<TextView>(R.id.tvP)
        tvA1.text = "A1:" + items?.get(position)?.a1.toString()
        tvA2.text = "A2:" + items?.get(position)?.a2.toString()
        tvX0.text = "X0:" + items?.get(position)?.x0.toString()
        tvP.text = "P:" + items?.get(position)?.p.toString()
        return convertView
    }
}
