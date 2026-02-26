package com.wl.turbidimetric.dao

import com.wl.turbidimetric.model.UserModel
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Update
import com.wl.turbidimetric.repository.if2.UserSource
import kotlinx.coroutines.flow.Flow

@Dao
@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
interface LoginDao {
    @Query("SELECT * FROM UserModel WHERE userName = :userName and password = :password")
    fun login(userName: String, password: String): UserModel

    @Query("SELECT * FROM UserModel WHERE userId = :id")
    fun getUser(id: Long): UserModel

    @Update
    fun changePassword(userModel: UserModel): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addUser(userModel: UserModel): Long

    @Query("SELECT * FROM UserModel where level >= :level")
    fun getAllUsers(level: Int): Flow<List<UserModel>>

    @Query("SELECT * FROM UserModel")
    fun getAllUsers(): Flow<List<UserModel>>

    @Delete
    fun deleteUser(userModel: UserModel): Int
}
