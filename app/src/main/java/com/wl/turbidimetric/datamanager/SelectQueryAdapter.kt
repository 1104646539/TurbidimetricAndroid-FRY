package com.wl.turbidimetric.datamanager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.SpinnerAdapter
import android.widget.TextView
import com.wl.turbidimetric.R

class SelectQueryAdapter(
    val context: Context?,
    val strings: Array<String>
) :
    BaseAdapter(), SpinnerAdapter {
    private var items: MutableList<Item>? = null

    init {
        if (strings != null) {
            items = mutableListOf()
            for (i in strings.indices) {
                items!!.add(Item(strings[i]))
            }
        }
    }


    override fun getCount(): Int {
        return items!!.size
    }

    override fun getItem(position: Int): Any? {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val root =
            LayoutInflater.from(context).inflate(R.layout.item_select_drop, parent, false)
//        val name = root.findViewById<TextView>(R.id.tv_name)
        val iv_select = root.findViewById<ImageView>(R.id.iv_select)
        iv_select.visibility = View.GONE
//        //        name.setText(items.get(position).getName());
//        var str = ""
//        for (i in items!!.indices) {
//            if (items!![i].isSelect) {
//                str = str + " " + items!![i].name
//            }
//        }
//        //        name.setText(str.isEmpty()?"请选择":str);
//        name.setTextColor(getResource().getColor(R.color.textGray))
        return root
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

    fun getSelectItems(): List<Item>? {
        val selectItems: MutableList<Item> = ArrayList()
        for (i in items!!.indices) {
            if (items!![i].isSelect) {
                selectItems.add(items!![i])
            }
        }
        return selectItems
    }

    fun getSelectItemsValue(): Array<String>? {
        val selectItems: MutableList<Item> = ArrayList()
        for (i in items!!.indices) {
            if (items!![i].isSelect) {
                selectItems.add(items!![i])
            }
        }
        if (selectItems == null || selectItems.isEmpty()) {
            return null
        }
        val str = selectItems.map { it.name }.toTypedArray()
        return str
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val root = LayoutInflater.from(context).inflate(R.layout.item_select_drop, parent, false)
        val name = root.findViewById<TextView>(R.id.tv_name)
        val iv_select = root.findViewById<ImageView>(R.id.iv_select)
        name.text = items!![position].name
        iv_select.isSelected = items!![position].isSelect
        iv_select.setOnClickListener {
            iv_select.isSelected = !iv_select.isSelected
            items!![position].isSelect = iv_select.isSelected
            if (onItemSelectChange != null) {
                onItemSelectChange?.invoke(position, iv_select.isSelected)
            }
        }
        root.setOnClickListener {
            iv_select.isSelected = !iv_select.isSelected
            items!![position].isSelect = iv_select.isSelected
            if (onItemSelectChange != null) {
                onItemSelectChange?.invoke(position, iv_select.isSelected)
            }
        }
        return root
    }

    var onItemSelectChange: ((position: Int, selected: Boolean) -> Unit)? = null

    fun getSelectText(): String? {
        var str = ""
        for (i in items!!.indices) {
            if (items!![i].isSelect) {
                str = str + "  " + items!![i].name
            }
        }
        return if (str.isEmpty()) "请选择" else str
    }

    class Item(var name: String) {
        var isSelect = false

    }


    fun clearSelectedInfo() {
        for (i in items!!.indices) {
            items!![i].isSelect = false
        }
    }
}
