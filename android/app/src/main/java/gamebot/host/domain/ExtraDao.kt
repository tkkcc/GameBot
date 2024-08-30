package gamebot.host.domain

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtraDao {
    @Query("SELECT * FROM extra")
    fun observeAll(): Flow<List<Extra>>

    @Query("SELECT * FROM extra WHERE type = :type")
    fun observe(type: String): Flow<Extra>

    @Query("SELECT * FROM extra WHERE type = :type")
    suspend fun get(type: String): Extra?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(extra: Extra)

    @Delete
    suspend fun remove(extra: Extra)

    @Update
    suspend fun update(extra: Extra)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addIfNotExist(extra: Extra)

}