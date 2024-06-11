package gamebot.host.domain

import kotlinx.coroutines.flow.Flow

class ExtraRepository(
    private val dao: ExtraDao
) {
     fun observeAll(): Flow<List<Extra>> = dao.observeAll()

     fun observe(type: String): Flow<Extra> = dao.observe(type)

     suspend fun get(type: String): Extra? = dao.get(type)

     suspend fun add(extra: Extra) = dao.add(extra)

     suspend fun update(extra: Extra) = dao.update(extra)

     suspend fun remove(extra: Extra) = dao.remove(extra)

     suspend fun addIfNotExist(extra: Extra) = dao.addIfNotExist(extra)

}