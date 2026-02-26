package com.wl.turbidimetric.repository

import com.wl.turbidimetric.dao.LoginDao
import com.wl.turbidimetric.dao.MainDao
import com.wl.turbidimetric.model.UserModel
import com.wl.turbidimetric.repository.if2.TestResultSource
import com.wl.turbidimetric.repository.if2.UserSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class DefaultUserModelSource(
    private val dao: LoginDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserSource {
    override suspend fun getAdmin(userName: String, password: String): UserModel {
        return dao.login(userName, password)
    }

    override suspend fun login(userName: String, password: String): UserModel {
        return dao.login(userName, password)
    }

    override suspend fun getUser(id: Long): UserModel {
        return dao.getUser(id)
    }

    override suspend fun changePassword(userModel: UserModel): Boolean {
        return dao.changePassword(userModel) > 0
    }

    override suspend fun addUser(userModel: UserModel): Boolean {
        return dao.addUser(userModel) > 0
    }

    override suspend fun deleteUser(userModel: UserModel): Boolean {
        return dao.deleteUser(userModel) > 0
    }

    override suspend fun getAllUsers(level: Int): Flow<List<UserModel>> {
        return dao.getAllUsers(level)
    }
}
