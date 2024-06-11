package gamebot.host.domain

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import gamebot.host.domain.crontab.Schedule
import gamebot.host.presentation.schedule.JsonExtension.Companion.decodeFromStringOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    exportSchema = true,
    entities = [Task::class, Extra::class],
    version = 1,
    autoMigrations = [
//        AutoMigration(from = 1, to = 2),
//        AutoMigration(from = 2, to = 3),
    ]
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun extraDao(): ExtraDao

    companion object {
        const val name = "task_db"
    }
}


class Converters {
    @TypeConverter
    fun taskStatusEncode(value: TaskStatus): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun taskStatusDecode(date: String): TaskStatus {
        return Json.decodeFromStringOrNull<TaskStatus>(date) ?: TaskStatus()
    }

    @TypeConverter
    fun scheduleEncode(value: Schedule): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun scheduleDecode(date: String): Schedule {
        return Json.decodeFromStringOrNull<Schedule>(date) ?: Schedule()
    }

}