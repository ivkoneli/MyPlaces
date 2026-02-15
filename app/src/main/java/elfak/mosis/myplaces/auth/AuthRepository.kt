package elfak.mosis.myplaces.auth

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import elfak.mosis.myplaces.data.AppUser
import elfak.mosis.myplaces.data.Pokemon

// Singleton class representing the authorisation logic using fire base auth/store
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Logs in our user with data collected from the inputs
    fun login(
        email: String,
        password: String,
        onSuccess: (AppUser) -> Unit,
        onError: (String) -> Unit
    ) { // Calls Firebase function to Login , OnSuccess gets the user from fire store
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser!!.uid
                fetchAppUser(uid, onSuccess, onError)
            }
            .addOnFailureListener {
                auth.signInWithEmailAndPassword(email, password)
                onError(it.message ?: "Login failed")
            }
    }

    // Getting the user from fire store and making a new instance of AppUser
    fun fetchAppUser(
        uid: String,
        onSuccess: (AppUser) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(AppUser::class.java)!!
                    onSuccess(user)
                } else {
                    onError("User profile does not exist")
                }
            }
            .addOnFailureListener {
                onError(it.message ?: "Failed to load user")
            }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        phone: String,
        avatarDrawable: Int?,
        avatarUri: Uri?,
        onSuccess: (AppUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser!!.uid

                // Priprema localAvatarPath
                val localAvatarPath = when {
                    avatarUri != null -> avatarUri.toString() // galerija/kamera
                    avatarDrawable != null -> "drawable://$avatarDrawable" // drawable
                    else -> null
                }

                val newUser = AppUser(
                    uid = uid,
                    username = username,
                    phone = phone,
                    level = 1,
                    xp = 0,
                    pokemonIds = emptyList(),
                    wins = 0,
                    loses = 0,
                    avatarUrl = null,           // nikad više ne koristi avatarUrl
                    localAvatarPath = localAvatarPath
                )

                db.collection("users").document(uid)
                    .set(newUser)
                    .addOnSuccessListener {
                        onSuccess(newUser) // fragment zna da je ok
                    }
                    .addOnFailureListener { e ->
                        onError("Failed to create user document: ${e.message}")
                    }

            }
            .addOnFailureListener {
                onError(it.message ?: "Register failed")
            }
    }





    fun addStarterPokemon(
        ownerId: String,
        starterPokemon: Pokemon,
        onSuccess: (AppUser) -> Unit,
        onError: (String) -> Unit
    ) {

        val pokemonToSave = starterPokemon.copy(ownerId = ownerId)

        db.collection("pokemons")
            .add(pokemonToSave)
            .addOnSuccessListener { docRef ->

                db.collection("users").document(ownerId)
                    .update("pokemonIds", FieldValue.arrayUnion(docRef.id))
                    .addOnSuccessListener {
                        fetchAppUser(ownerId, onSuccess, onError)
                    }
                    .addOnFailureListener { e ->
                        onError("Failed to update user's pokemonIds: ${e.message}")
                    }

            }
            .addOnFailureListener { e ->
                onError("Failed to create starter Pokemon: ${e.message}")
            }
    }


    fun addPokemonToUser(
        pokemon: Pokemon,
        ownerId: String,
        onSuccess: (AppUser) -> Unit,
        onError: (String) -> Unit
    ) {

        val ownedPokemon = pokemon.copy(
            ownerId = ownerId,
            currenthp = 0,
            alive = false
        )

        db.collection("pokemons")
            .add(ownedPokemon)
            .addOnSuccessListener { docRef ->

                db.collection("users").document(ownerId)
                    .update("pokemonIds", FieldValue.arrayUnion(docRef.id))
                    .addOnSuccessListener {
                        fetchAppUser(ownerId, onSuccess, onError)
                    }
                    .addOnFailureListener { e ->
                        onError("Failed to update user's pokemonIds: ${e.message}")
                    }

            }
            .addOnFailureListener { e ->
                onError("Failed to add Pokemon: ${e.message}")
            }
    }


}
