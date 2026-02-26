package com.wl.turbidimetric.login.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.model.UserModel
import com.wl.turbidimetric.repository.if2.UserSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserListViewModel(private val userSource: UserSource) : BaseViewModel() {

    /**
     * 根据当前登录用户的 level 和 userId 获取用户列表
     * @param currentUserLevel 当前登录用户的 level
     * @param currentUserId 当前登录用户的 userId
     * 
     * 规则：
     * - level 为 0：显示所有用户信息
     * - level 为 1：显示所有 level >= 1 的用户信息 + 当前用户信息
     * - level 为 3：只显示当前用户信息
     */
    suspend fun getUserList(currentUserLevel: Int, currentUserId: Long): Flow<List<UserModel>> {
        return when (currentUserLevel) {
            0 -> {
                // level 为 0：显示所有用户信息
                userSource.getAllUsers()
            }
            1 -> {
                // level 为 1：显示所有 level >= 1 的用户信息（包含当前用户）
                userSource.getAllUsers(1)
            }
            else -> {
                // level 为 3 或其他：只显示当前用户信息
                userSource.getAllUsers(1).map { userList ->
                    userList.filter { it.userId == currentUserId }
                }
            }
        }
    }

    class UserListViewModelFactory(
        private val userSource: UserSource = ServiceLocator.provideUserSource(
            App.instance!!
        )
    ) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserListViewModel::class.java)) {
                return UserListViewModel(
                    userSource
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}
