package com.wl.turbidimetric.login.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemUserListBinding
import com.wl.turbidimetric.model.UserModel

class UserListAdapter(private val users: MutableList<UserModel>) :
    RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    fun submit(userModels: List<UserModel>) {
        this.users.clear()
        this.users.addAll(userModels)
        notifyDataSetChanged()
    }

    var onItemClick: OnItemClick? = null

    class UserViewHolder(val binding: ItemUserListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(project: UserModel) {
            binding.tvUserName.text = project.userName
            binding.tvPsw.text = project.password
            binding.tvLevel.text = project.showLevel()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = DataBindingUtil.inflate<ItemUserListBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_user_list, parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
        holder.binding.root.setOnClickListener {
            onItemClick?.invoke(users[position])
        }
    }

    override fun getItemCount(): Int {
        return if (this.users.isNullOrEmpty()) 0 else this.users.size
    }


}
typealias OnItemClick = (UserModel) -> Unit
