package gamebot.host

enum class RunnerInfo(
    val type: String, val repo: String
) {
    Arknights(
        "arknights_cn",
//        "https://gitee.com/bilabila/gamekeeper_arknights.git",
        "https://e.coding.net/bilabila/gamekeeper/arknights_cn.git"

    ),
    StarRail(
        "star_rail_cn",
//        "https://gitee.com/bilabila/gamekeeper_star_rail.git"
        "https://e.coding.net/bilabila/gamekeeper/star_rail_cn.git"
    ),
    BlueArchive(
        "blue_archive_cn",
//        "https://gitee.com/bilabila/gamekeeper_blue_archive.git"
        "https://e.coding.net/bilabila/gamekeeper/blur_archive_cn.git"
    )
}