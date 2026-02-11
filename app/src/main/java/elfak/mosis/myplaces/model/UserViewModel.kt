package elfak.mosis.myplaces.model

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import elfak.mosis.myplaces.data.AppUser
import elfak.mosis.myplaces.data.Pokemon

// View Model for userData
class UserViewModel : ViewModel() {
    val currentUser = MutableLiveData<AppUser>()
    var visitedUserId: String? = null
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun fetchUserPokemons(ownerId: String, onComplete: (List<Pokemon>) -> Unit) {
        db.collection("pokemons")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val pokemons = snapshot.map { it.toObject(Pokemon::class.java).apply { id = it.id } }

                // Custom sort: prvo živi (hp > 0) po levelu descending, zatim mrtvi (hp <= 0) po levelu descending
                val sortedPokemons = pokemons.sortedWith(compareByDescending<Pokemon> { it.currenthp > 0 } // živi prvi
                    .thenByDescending { it.level } // po levelu descending
                )
                onComplete(sortedPokemons)
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

    fun fetchUserLevels(onComplete: (List<Pair<String, Int>>) -> Unit) {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.mapNotNull { doc ->
                    val username = doc.getString("username") ?: return@mapNotNull null
                    val level = doc.getLong("level")?.toInt() ?: 0
                    Pair(username, level)
                }.sortedByDescending { it.second } // sort po level desc
                onComplete(list)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    fun fetchUserByUsername(username: String, onComplete: (AppUser?) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                val user = doc?.toObject(AppUser::class.java)?.apply { this.uid = doc.id }
                onComplete(user)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    fun fetchUserAvatars(usernames: List<String>, onComplete: (Map<String, String?>) -> Unit) {
        db.collection("users")
            .whereIn("username", usernames)
            .get()
            .addOnSuccessListener { snapshot ->
                val map = snapshot.associate { doc ->
                    val username = doc.getString("username") ?: ""
                    val avatar = doc.getString("avatarUrl") ?: doc.getString("localAvatarPath")
                    username to avatar
                }
                onComplete(map)
            }
            .addOnFailureListener {
                onComplete(emptyMap())
            }
    }



    fun updateAvatarPath(path: String) {
        currentUser.value = currentUser.value?.copy(localAvatarPath = path)
        currentUser.value?.let { user ->
            db.collection("users").document(user.uid)
                .update("localAvatarPath", path)
        }
    }

    // Update drawable ID
    fun updateAvatarDrawable(drawableId: Int) {
        val path = "drawable://$drawableId" // pretvaramo int u String
        currentUser.value = currentUser.value?.copy(localAvatarPath = path)
        currentUser.value?.let { user ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .update("localAvatarPath", path)
        }
    }


    fun disownPokemon(userId: String, pokemonId: String, onComplete: (Boolean) -> Unit) {
        val userRef = db.collection("users").document(userId)
        val pokemonRef = db.collection("pokemons").document(pokemonId)

        // 1️⃣ Uklanjamo Pokemon ID iz liste korisnika
        userRef.update("pokemonIds", FieldValue.arrayRemove(pokemonId))
            .addOnSuccessListener {
                // 2️⃣ Brišemo dokument Pokemona iz pokemons kolekcije
                pokemonRef.delete()
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        onComplete(false)
                    }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onComplete(false)
            }
    }





}