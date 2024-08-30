
import android.content.Context
import android.util.Log
import androidx.room.Room
import gamebot.host.ILocalRun
import gamebot.host.IRemoteRun
import gamebot.host.RunnerInfo
import gamebot.host.domain.ExtraRepository
import gamebot.host.domain.Runner
import gamebot.host.domain.RunnerRepository
import gamebot.host.domain.TaskDatabase
import gamebot.host.domain.TaskRepository
import gamebot.host.loader.Git
import dalvik.system.DexClassLoader
import java.io.File

class Container(val context: Context, val localRun: ILocalRun, val remoteRun: IRemoteRun) {

    var taskRepository: TaskRepository
    var runnerRepository: RunnerRepository
    var extraRepository: ExtraRepository

    init {
        val db = Room.databaseBuilder(
            context.applicationContext,
            TaskDatabase::class.java,
            TaskDatabase.name
        ).build()
        taskRepository = TaskRepository(db.taskDao())
        runnerRepository = RunnerRepository()
        extraRepository = ExtraRepository(db.extraDao())
    }


    fun fetchLoadRunner(type: String): Result<Runner> = runCatching {
        Log.d("FetchLoadRunner", type)
        runnerRepository.get(type)?.run {
            return@runCatching this
        }
//        val runnerCache=  runnerRepository.get(type)
//        if (runnerCache !=null){
//            return Result.success(runnerCache)
//        }
        Log.d("FetchLoad", "$type fetchLoad")

        val runnerInfo = RunnerInfo.entries.find {
            it.type == type
        } ?: return Result.failure(Exception("known type $type"))
        val repo = runnerInfo.repo
        val branch = "main"
        val path: String = File(context.externalCacheDir, type).absolutePath
        Git.cloneOrPull(repo, path,branch).getOrThrow()

        val cache = File(context.codeCacheDir, type).also { it.mkdirs() }.absolutePath


        // load dex from disk
        val dex = File(path, "classes.dex").also { it.setReadOnly() }.absolutePath
        val loader = DexClassLoader(
            dex, cache, null, this.javaClass.classLoader
        )
        val taskClass = loader.loadClass("com.gamekeeper.$type.MainRunner")
        val runner = taskClass.getConstructor().newInstance() as Runner
        runnerRepository.set(type, runner)
        runner
    }


}
