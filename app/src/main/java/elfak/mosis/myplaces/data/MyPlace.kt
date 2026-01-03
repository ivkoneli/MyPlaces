package elfak.mosis.myplaces.data

data class MyPlace(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var longitude: String = "",
    var latitude: String = "",
    var type: String = "Pokemon",
    var userId: String = ""
) {
    override fun toString(): String = name
}

