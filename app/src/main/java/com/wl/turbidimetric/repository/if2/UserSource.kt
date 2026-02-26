package com.wl.turbidimetric.repository.if2

import com.wl.turbidimetric.model.UserModel
import kotlinx.coroutines.flow.Flow


interface UserSource {
    suspend fun getAdmin(userName: String, password: String): UserModel
    suspend fun login(userName: String, password: String): UserModel
    suspend fun getUser(id: Long): UserModel
    suspend fun changePassword(userModel: UserModel): Boolean
    suspend fun addUser(userModel: UserModel): Boolean
    suspend fun deleteUser(userModel: UserModel): Boolean
    suspend fun getAllUsers(level: Int): Flow<List<UserModel>>
}
