package gamebot.host

class Native {
    companion object {
        init {
            System.loadLibrary("rust")
        }
    }


    external fun start(x: Any)

    // it's for ui event, we simply encode to json string
    external fun callback(x: String)

}