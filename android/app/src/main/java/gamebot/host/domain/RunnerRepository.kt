package gamebot.host.domain

class RunnerRepository{

    var type2runner = mutableMapOf<String, Runner>()
     fun get(type: String): Runner?  =
        type2runner[type]


     fun set(type:String, runner: Runner) {
        type2runner[type] = runner
    }


}