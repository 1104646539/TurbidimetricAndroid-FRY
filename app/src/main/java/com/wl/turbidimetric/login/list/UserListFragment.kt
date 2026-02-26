package com.wl.turbidimetric.login.list

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentUserListBinding
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.project.list.ProjectListAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class UserListFragment :
    BaseFragment<UserListViewModel, FragmentUserListBinding>(R.layout.fragment_user_list) {
    override val vm: UserListViewModel by viewModels { UserListViewModel.UserListViewModelFactory() }
    val adapter by lazy { UserListAdapter(mutableListOf()) }
    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        vd.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        vd.rv.adapter = adapter
        vd.btnAddUser.setOnClickListener {
            EventBus.getDefault().post(EventMsg<Long>(EventGlobal.WHAT_USER_LIST_TO_DETAILS, 0L))
        }
        adapter.onItemClick = {
            EventBus.getDefault()
                .post(EventMsg<Long>(EventGlobal.WHAT_USER_LIST_TO_DETAILS, it.userId))
        }
        lifecycleScope.launch {
            appVm.userModel?.let {
                vm.getUserList(it.level).collectLatest {
                    adapter.submit(it.toMutableList())
                }
            }
        }
    }
}
