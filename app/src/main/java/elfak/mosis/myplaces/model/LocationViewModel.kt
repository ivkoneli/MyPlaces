package elfak.mosis.myplaces.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel : ViewModel() {

    val longitude = MutableLiveData<String>("")
    val latitude = MutableLiveData<String>("")

    var isSettingLocation = false

    fun setLocation(lon: String, lat: String) {
        longitude.value = lon
        latitude.value = lat
        isSettingLocation = false
    }
}


