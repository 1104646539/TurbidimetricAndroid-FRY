package com.wl.turbidimetric.login.list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentUserListBinding
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.project.list.ProjectListAdapter
import com.wl.turbidimetric.view.dialog.isShow
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
        loadUserList()
        showAddBtn()
    }

    private fun showAddBtn() {
        vd.btnAddUser.visibility = appVm.userModel?.isAdmin()?.isShow() ?: View.GONE
    }

    /**
     * 加载用户列表
     */
    private fun loadUserList() {
        lifecycleScope.launch {
            appVm.userModel?.let { currentUser ->
                vm.getUserList(currentUser.level, currentUser.userId).collectLatest {
                    adapter.submit(it.toMutableList())
                }
            }
        }
    }

    /**
     * 监听登录成功事件，刷新用户列表
     */
    override fun onMessageEvent(event: EventMsg<Any>) {
        super.onMessageEvent(event)
        when (event.what) {
            EventGlobal.WHAT_LOGIN_SUCCESS -> {
                // 切换用户后刷新列表,刷新是否显示添加用户
                loadUserList()
                showAddBtn()
            }
        }
    }
}
