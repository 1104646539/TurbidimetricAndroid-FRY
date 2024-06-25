package com.wl.turbidimetric.db

import android.content.Context
import androidx.room.Room
import com.wl.turbidimetric.repository.DefaultCurveDataSource
import com.wl.turbidimetric.repository.DefaultLocalDataDataSource
import com.wl.turbidimetric.repository.DefaultProjectDataSource
import com.wl.turbidimetric.repository.DefaultTestResultDataSource
import com.wl.turbidimetric.repository.if2.CurveSource
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.repository.if2.ProjectSource
import com.wl.turbidimetric.repository.if2.TestResultSource

object ServiceLocator {
    @Volatile
    var database: MainRoomDatabase? = null
    var projectDataSource: ProjectSource? = null
    var curveSource: CurveSource? = null
    var localDataSource: LocalDataSource? = null
    var testResultSource: TestResultSource? = null


     fun getDb(context: Context,isMemory: Boolean = false) :MainRoomDatabase{
        return database ?: createDataBase(context,isMemory).apply {
            database = this
        }
    }

    fun provideLocalDataSource(): LocalDataSource {
        synchronized(this) {
            return localDataSource ?: DefaultLocalDataDataSource().apply {
                localDataSource = this
            }
        }
    }

    fun provideCurveSource(context: Context): CurveSource {
        synchronized(this) {
            return curveSource ?: DefaultCurveDataSource(dao = getDb(context).mainDao()).apply {
                curveSource = this
            }
        }
    }

    fun provideTestResultSource(context: Context): TestResultSource {
        synchronized(this) {
            return testResultSource ?: DefaultTestResultDataSource(dao = getDb(context).mainDao()).apply {
                testResultSource = this
            }
        }
    }

    fun provideProjectSource(context: Context): ProjectSource {
        synchronized(this) {
            return projectDataSource ?: DefaultProjectDataSource(dao = getDb(context).mainDao()).apply {
                projectDataSource = this
            }
        }
    }

    /**
     * 获取数据库唯一实例 isMemory为true时，新建的数据库只存在于内存中
     * @param context Context
     * @param isMemory Boolean
     * @return MainRoomDatabase
     */
    fun createDataBase(context: Context, isMemory: Boolean = false): MainRoomDatabase {
        return database ?: synchronized(this) {
            val instance = if (isMemory) {
                Room.inMemoryDatabaseBuilder(
                    context.applicationContext,
                    MainRoomDatabase::class.java,
                )
                    .allowMainThreadQueries()
                    .build()
            } else {
                Room.databaseBuilder(
                    context.applicationContext,
                    MainRoomDatabase::class.java,
                    "word_database"
                )
                    .allowMainThreadQueries()
//                    .createFromFile(File("sdcard/bf/word_database"))
//                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
            }
            database = instance
            instance
        }
    }

    fun resetDataSource() {
        database?.apply {
            clearAllTables()
            close()
        }
        database = null
        projectDataSource = null
        curveSource = null
        localDataSource = null
        testResultSource = null
    }

}
