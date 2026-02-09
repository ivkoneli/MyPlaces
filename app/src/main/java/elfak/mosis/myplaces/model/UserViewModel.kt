package elfak.mosis.myplaces.model

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import elfak.mosis.myplaces.data.AppUser
import elfak.mosis.myplaces.data.Pokemon

// View Model for userData
class UserViewModel : ViewModel() {
    val currentUser = MutableLiveData<AppUser>()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun fetchUserPokemons(ownerId: String, onComplete: (List<Pokemon>) -> Unit) {
        db.collection("pokemons")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val pokemons = snapshot.map { it.toObject(Pokemon::class.java).apply { id = it.id } }
                onComplete(pokemons)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }
    fun fetchUserWins(onComplete: (List<Pair<String, Int>>) -> Unit) {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("LEADERBOARD", "Documents fetched: ${snapshot.documents.size}")
                val list = snapshot.mapNotNull { doc ->
                    val username = doc.getString("username") ?: return@mapNotNull null
                    val wins = doc.getLong("wins")?.toInt() ?: 0
                    Pair(username, wins)
                }.sortedByDescending { it.second } // sort po wins desc
                onComplete(list)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

}