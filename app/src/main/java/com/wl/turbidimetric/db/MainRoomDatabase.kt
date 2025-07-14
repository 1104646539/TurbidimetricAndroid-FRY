package com.wl.turbidimetric.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wl.turbidimetric.db.converters.StringToStringListConverters
import com.wl.turbidimetric.dao.GlobalDao
import com.wl.turbidimetric.dao.LogDao
import com.wl.turbidimetric.dao.MainDao
import com.wl.turbidimetric.db.converters.BigDecimalConverters
import com.wl.turbidimetric.db.converters.DoubleArrayConverters
import com.wl.turbidimetric.db.converters.IntArrayConverters
import com.wl.turbidimetric.db.converters.LogLevelConverters
import com.wl.turbidimetric.log.LogModel
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.GlobalConfig
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.model.TestResultModel

@Database(
    entities = [TestResultModel::class, ProjectModel::class, CurveModel::class, GlobalConfig::class, LogModel::class],
    version = 1,
    exportSchema = true,
//    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(
    BigDecimalConverters::class,
    IntArrayConverters::class,
    DoubleArrayConverters::class,
    LogLevelConverters::class,
    StringToStringListConverters::class
)
abstract class MainRoomDatabase : RoomDatabase() {

    abstract fun mainDao(): MainDao
    abstract fun globalDao(): GlobalDao
    abstract fun logDao(): LogDao


//    companion object {
//        @Volatile
//        private var INSTANCE: MainRoomDatabase? = null
//        fun getDatabase(context: Context): MainRoomDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    MainRoomDatabase::class.java,
//                    "word_database"
//                )
//                    .allowMainThreadQueries()
////                    .createFromFile(File("sdcard/bf/word_database"))
////                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
//                    .build()
//                INSTANCE = instance
//                instance
//            }
//        }

//        val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("ALTER TABLE CurveModel ADD COLUMN reactionValues varchar")
//            }
//        }
//        val MIGRATION_2_3 = object : Migration(2, 3) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("ALTER TABLE TestResultModel ADD COLUMN testV varchar")
//            }
//        }
//    }
}

