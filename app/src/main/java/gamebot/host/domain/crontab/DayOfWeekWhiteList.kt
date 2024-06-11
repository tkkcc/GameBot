package gamebot.host.domain.crontab

import kotlinx.serialization.Serializable

@Serializable
data class DayOfWeekWhitelist(
    val monday: Boolean = true,
    val tuesday: Boolean = true,
    val wednesday: Boolean = true,
    val thursday: Boolean = true,
    val friday: Boolean = true,
    val saturday: Boolean = true,
    val sunday: Boolean = true,
) {
    fun toList(): List<Boolean> =
        listOf(
            this.monday,
            this.tuesday,
            this.wednesday,
            this.thursday,
            this.friday,
            this.saturday,
            this.sunday
        )

    fun isAllAllowed(): Boolean = this.toList().all { it }
    fun banned(): List<Int> = this.toList().foldIndexed(emptyList()) { idx, acc, cur ->
        if (cur) {
            acc
        } else {
            acc + idx
        }
    }

}
