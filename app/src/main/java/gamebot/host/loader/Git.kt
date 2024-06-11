package gamebot.host.loader

import android.app.Activity
import android.content.Context
import android.util.Log
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.transport.RefSpec
import java.io.File
import java.time.Duration

object Git {

    fun clone(url: String, path: String, branch: String? = null): Result<Unit> = runCatching {
        val dir = File(path)
        dir.deleteRecursively()
        val cmd =
            Git.cloneRepository().setURI(url).setDirectory(dir).setCloneAllBranches(false).setCloneSubmodules(true)
        cmd.call()
    }

    fun count(path: String): Result<Int> = runCatching {
        val repo = Git.open(File(path))
        val count = repo.reflog().call().count()
        count
    }

//    fun gc(path: String): Result<Unit> = runCatching {
//        val repo = Git.open(File(path))
//        repo.gc().call()
//    }

    fun pull(path: String): Result<Unit> = runCatching {
        val repo = Git.open(File(path))
//        repo.pull().setStrategy(MergeStrategy.THEIRS)
        repo.fetch().call()
//        Log.d("", repo.branchList())
        repo.reset().setRef("FETCH_HEAD").setMode(ResetCommand.ResetType.HARD).call()

//        repo.reset().setMode(ResetCommand.ResetType.HARD).call()
//        repo.pull()
        // pull
//        repo.fetch().setRemote("origin")
//            .setRefSpecs(RefSpec("+refs/heads/" + branch + ":refs/remotes/origin/" + branch))
//            .setCheckFetchedObjects(true).setRemoveDeletedRefs(true).call()
//        repo.reset().setMode(ResetCommand.ResetType.HARD)
//            .setRef("refs/remotes/origin/" + branch).call()
    }

    // clone or pull
    fun fetch(
        context: Activity,
        repo: String,
        branch: String,
        path: String,
    ): Result<Unit> = runCatching {
        // clone or fetch
        val key = "repo_cloned_$path"


        // if cloned, fetch and reset
        if (SaveLoadString.load(context, key) == "true") {
           pull( path).getOrThrow()
            return Result.success(Unit)
        }
        // if not cloned, clone
        clone(repo, branch, path).getOrThrow()
        SaveLoadString.save(context, key, "true")

        // no way to gc in jgit or git2
        // reinit after X old commit
        if (count(path).getOrThrow() > 50) {
            SaveLoadString.save(context, key, "")
        }
    }
}