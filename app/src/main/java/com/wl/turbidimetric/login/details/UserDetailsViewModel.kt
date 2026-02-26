package com.wl.turbidimetric.login.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.login.list.UserListViewModel
import com.wl.turbidimetric.model.UserModel
import com.wl.turbidimetric.repository.if2.UserSource

class UserDetailsViewModel(private val userSource: UserSource) : BaseViewModel() {


    suspend fun getUser(id: Long): UserModel = userSource.getUser(id)
    suspend fun addUser(user: UserModel): Boolean = userSource.addUser(user)
    suspend fun updateUser(user: UserModel): Boolean = userSource.changePassword(user)

    class UserDetailsViewModelFactory(
        private val userSource: UserSource = ServiceLocator.provideUserSource(
            App.instance!!
        )
    ) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserDetailsViewModel::class.java)) {
                return UserDetailsViewModel(
                    userSource
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
