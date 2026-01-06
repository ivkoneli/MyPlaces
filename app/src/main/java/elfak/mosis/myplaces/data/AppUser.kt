package elfak.mosis.myplaces.data

// Data model for displaying user info
data class AppUser(
    var uid: String = "",
    var username: String = "",
    var level: Int = 1,
    var xp: Int = 0,
    var pokemonIds: List<String> = emptyList(),
    var wins: Int = 0,
    var loses: Int = 0
)
