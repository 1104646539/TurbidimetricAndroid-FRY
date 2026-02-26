package com.wl.turbidimetric.login.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.model.UserModel
import com.wl.turbidimetric.repository.if2.UserSource
import kotlinx.coroutines.flow.Flow

class UserListViewModel(private val userSource: UserSource) : BaseViewModel() {

    suspend fun getUserList(level: Int): Flow<List<UserModel>> {
        return userSource.getAllUsers(level)
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
