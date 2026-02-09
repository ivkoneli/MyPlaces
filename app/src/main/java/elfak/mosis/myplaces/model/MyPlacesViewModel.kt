package elfak.mosis.myplaces.model

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.myplaces.data.MyPlace

class MyPlacesViewModel: ViewModel() {

    // -----------------------------
    // LIVE DATA LISTS
    // -----------------------------
    // allPlaces -> sve fetchovane lokacije
    var allPlaces = MutableLiveData<List<MyPlace>>(emptyList())
    // filterovane userove lokacije
    var userPlaces = MutableLiveData<List<MyPlace>>(emptyList())
    // myPlacesList -> trenutno filtrirana lista koja ide na mapu
    var myPlacesList = MutableLiveData<List<MyPlace>>(emptyList())

    // -----------------------------
    // FILTER PARAMETERS
    // -----------------------------
    var selectedType: String? = null
    var searchQuery: String? = null
    var maxDistanceMeters: Float? = 1000f // 1KM default
    var currentUserLocation: Pair<Double, Double>? = null // (lat, lon)

    var selected: MyPlace? = null

    fun fetchLocations() {
        Firebase.firestore.collection("locations")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MyPlace::class.java)?.apply {
                        id = doc.id
                    }
                }
                allPlaces.value = list
                applyFilters()
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
                }.filter { it.userID == currentUserId }

                userPlaces.value = list      // čista lista za ListFragment
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
                val currentList = allPlaces.value?.toMutableList() ?: mutableListOf()
                currentList.add(location)
                allPlaces.value = currentList
                applyFilters()
            }
    }

    fun updateLocation(updated: MyPlace) {

        Firebase.firestore.collection("locations")
            .document(updated.id)
            .set(updated)
            .addOnSuccessListener {

                val currentList = allPlaces.value?.toMutableList() ?: return@addOnSuccessListener
                val index = currentList.indexOfFirst { it.id == updated.id }
                if (index != -1) {
                    currentList[index] = updated
                    allPlaces.value = currentList
                    applyFilters()
                }
            }
            .addOnFailureListener {
                Log.e("UPDATE", "Failed to update location", it)
            }
    }

    fun removeLocation(location : MyPlace){
        val currentList = myPlacesList.value?.toMutableList() ?: mutableListOf()
        currentList.remove(location)
        allPlaces.value = currentList
        applyFilters()
    }

    fun applyFilters() {
        var result = allPlaces.value ?: emptyList()

        // FILTER BY TYPE
        selectedType?.let { type ->
            result = result.filter { it.type == type }
        }

        // FILTER BY SEARCH QUERY (FUZZY)
        searchQuery?.let { query ->
            if (query.isNotBlank()) {
                result = result.filter {
                    fuzzyMatch(it.name, query)
                }
            }
        }

        // FILTER BY DISTANCE
        maxDistanceMeters?.let { maxDist ->
            currentUserLocation?.let { (lat, lon) ->
                result = result.filter {
                    val d = distanceInMeters(lat, lon, it.latitude.toDouble(), it.longitude.toDouble())
                    d <= maxDist
                }
            }
        }

        // UPDATE LIVE DATA
        myPlacesList.value = result
    }

    // HELPER: distance between two lat/lon points in meters
    private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun fuzzyMatch(text: String, query: String): Boolean {
        var tIndex = 0
        var qIndex = 0

        val t = text.lowercase()
        val q = query.lowercase()

        while (tIndex < t.length && qIndex < q.length) {
            if (t[tIndex] == q[qIndex]) {
                qIndex++
            }
            tIndex++
        }

        return qIndex == q.length
    }


}