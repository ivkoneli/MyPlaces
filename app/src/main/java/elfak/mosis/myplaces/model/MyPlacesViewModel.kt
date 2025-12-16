package elfak.mosis.myplaces.model

import androidx.lifecycle.ViewModel
import elfak.mosis.myplaces.data.MyPlace

class MyPlacesViewModel: ViewModel() {

    var myPLacesList: ArrayList<MyPlace> = ArrayList<MyPlace>()
    fun addPlace( place : MyPlace){
        myPLacesList.add(place);
    }
    var selected: MyPlace? = null
}