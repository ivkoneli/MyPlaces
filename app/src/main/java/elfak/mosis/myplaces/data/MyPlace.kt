package elfak.mosis.myplaces.data
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MyPlace(
    var id: String = "",
    var name: String = "",
    var level: Int = 1,
    var longitude: String = "",
    var latitude: String = "",
    var type: String = "Pokemon",
    var userID: String = ""
) : Parcelable{
    override fun toString(): String = name
}

