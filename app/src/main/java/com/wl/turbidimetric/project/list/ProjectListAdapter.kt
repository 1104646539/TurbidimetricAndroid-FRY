package com.wl.turbidimetric.project.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.wl.turbidimetric.R
import com.wl.turbidimetric.databinding.ItemProjectListBinding
import com.wl.turbidimetric.model.ProjectModel

class ProjectListAdapter(private val projects: MutableList<ProjectModel>) :
    RecyclerView.Adapter<ProjectListAdapter.ProjectViewHolder>() {

    fun submit(projects: List<ProjectModel>) {
        this.projects.clear()
        this.projects.addAll(projects)
        notifyDataSetChanged()
    }
    var onItemClick: OnItemClick? = null
    class ProjectViewHolder(val binding: ItemProjectListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(project: ProjectModel) {
            binding.tvProjectName.text = project.projectName
            binding.tvProjectCode.text = project.projectCode
            binding.tvProjectLjz.text = "${project.projectLjz}"
            binding.tvProjectUnit.text = project.projectUnit
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = DataBindingUtil.inflate<ItemProjectListBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_project_list, parent, false
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(projects[position])
        holder.binding.root.setOnClickListener {
            onItemClick?.invoke(projects[position])
        }
    }

    override fun getItemCount(): Int {
        return if (this.projects.isNullOrEmpty()) 0 else this.projects.size
    }


}
typealias OnItemClick = (ProjectModel) -> Unit
