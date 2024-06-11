package bilabila.gamebot.host.domain.crontab

import kotlinx.serialization.Serializable

@Serializable
enum class CrontabInterval {
//    Hour0,
    Hour1,
    Hour2,
    Hour3,
    Hour4,
    Hour6,
    Hour8,
    Hour12;

    fun toHour(): Int =
        when(this){
//            Hour0 -> 0
            Hour1 -> 1
            Hour2 -> 2
            Hour3 -> 3
            Hour4 -> 4
            Hour6 -> 6
            Hour8 -> 8
            Hour12 -> 12
        }

}