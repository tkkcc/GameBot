package bilabila.gamebot.host.domain

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM task ORDER BY orderId")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE id = :id")
    fun observe(id: Long): Flow<Task>

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun get(id: Long): Task?

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun getDetail(id: Long): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(task: List<Task>)

    @Delete
    suspend fun remove(task: Task)
    @Delete
    suspend fun remove(task: List<Task>)

    @Update
    suspend fun update(task: Task)

    @Update
    suspend fun update(task: List<Task>)

    @Query("select count(*) from task")
    suspend fun count(): Int

    @Query("update task set orderId=:orderId where id=:id")
    suspend fun updateOrderId(id: Long, orderId: Int)

    @Transaction
    suspend fun updateOrderId(task: List<TaskId>) {
        // TODO it's not parallel ?
        task.forEach {
            updateOrderId(it.id, it.orderId)
        }
    }
    @Query("update task set status=:status where id=:id")
    suspend fun updateStatus(id: Long, status: TaskStatus)

//    @Transaction
//    suspend fun updateOrderId(task: List<Task>) {
//        // TODO it's not parallel ?
//        task.forEach {
//            updateOrderId(it.id, it.orderId)
//        }
//    }
}