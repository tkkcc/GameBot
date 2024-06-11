package bilabila.gamebot.host.domain

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val dao: TaskDao
) {
    fun observeAll(): Flow<List<Task>> = dao.observeAll()

    fun observe(id: Long): Flow<Task> = dao.observe(id)

    suspend fun get(id: Long): Task? = dao.get(id)

    suspend fun getDetail(id: Long): Task? = dao.getDetail(id)

    suspend fun add(task: Task): Long = dao.add(task)
    suspend fun add(task: List<Task>) = dao.add(task)

    suspend fun update(task: Task) = dao.update(task)

    suspend fun update(task: List<Task>) = dao.update(task)

    suspend fun updateOrderId(id: Long, orderId: Int) = dao.updateOrderId(id, orderId)
//    suspend fun updateOrderId(task: List<TaskId>) = dao.updateOrderId(task)
//    suspend fun updateOrderId(task: List<Task>) {
//        dao.updateOrderId(task.map { it.toTaskId() })
//    }

    suspend fun remove(task: Task) = dao.remove(task)
    suspend fun remove(task: List<Task>) = dao.remove(task)

    suspend fun count(): Int = dao.count()
    suspend fun updateStatus(id: Long, status: TaskStatus) {
        dao.updateStatus(id, status)
    }
}