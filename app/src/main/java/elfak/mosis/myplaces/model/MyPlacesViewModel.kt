package elfak.mosis.myplaces.model

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.myplaces.data.MyPlace

class MyPlacesViewModel: ViewModel() {

    var myPlacesList = MutableLiveData<List<MyPlace>>(emptyList())
    fun fetchLocations() {
        Firebase.firestore.collection("locations")
            .get()
            .addOnSuccessListener { result ->
                val list = result.map { doc ->
                    doc.toObject(MyPlace::class.java)
                }
                myPlacesList.value = list
            }
            .addOnFailureListener { e ->
                Log.e("MyPlacesViewModel", "Failed to fetch locations", e)
            }
    }
    fun addLocation(location: MyPlace) {
        Firebase.firestore.collection("locations")
            .add(location)
            .addOnSuccessListener { docRef ->
                // update lokalne LiveData
                location.id = docRef.id
                val currentList = myPlacesList.value?.toMutableList() ?: mutableListOf()
                currentList.add(location)
                myPlacesList.value = currentList
            }
    }

    fun removeLocation(location : MyPlace){
        val currentList = myPlacesList.value?.toMutableList() ?: mutableListOf()
        currentList.remove(location)
        myPlacesList.value = currentList
    }
    var selected: MyPlace? = null
}