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
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MyPlace::class.java)?.apply {
                        id = doc.id
                    }
                }
                myPlacesList.value = list
            }
            .addOnFailureListener { e ->
                Log.e("MyPlacesViewModel", "Failed to fetch locations", e)
            }
    }
    fun fetchUserLocations(currentUserId: String) {
        Firebase.firestore.collection("locations")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MyPlace::class.java)?.apply {
                        id = doc.id
                    }
                }

                // filter po userId
                myPlacesList.value = list.filter { it.userID == currentUserId }
            }
    }

    fun fetchUsername(userId: String, callback: (String) -> Unit) {
        Firebase.firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val username = doc?.getString("username") ?: "Unknown"
                callback(username)
            }
            .addOnFailureListener {
                callback("Unknown")
            }
    }
    fun addLocation(location: MyPlace) {
        Firebase.firestore.collection("locations")
            .add(location)
            .addOnSuccessListener { docRef ->
                location.id = docRef.id
                val currentList = myPlacesList.value?.toMutableList() ?: mutableListOf()
                currentList.add(location)
                myPlacesList.value = currentList
            }
    }

    fun updateLocation(updated: MyPlace) {

        Firebase.firestore.collection("locations")
            .document(updated.id)
            .set(updated)
            .addOnSuccessListener {

                val currentList = myPlacesList.value?.toMutableList() ?: return@addOnSuccessListener

                val index = currentList.indexOfFirst { it.id == updated.id }
                if (index != -1) {
                    currentList[index] = updated
                    myPlacesList.value = currentList
                }
            }
            .addOnFailureListener {
                Log.e("UPDATE", "Failed to update location", it)
            }
    }

    fun removeLocation(location : MyPlace){
        val currentList = myPlacesList.value?.toMutableList() ?: mutableListOf()
        currentList.remove(location)
        myPlacesList.value = currentList
    }
    var selected: MyPlace? = null
}