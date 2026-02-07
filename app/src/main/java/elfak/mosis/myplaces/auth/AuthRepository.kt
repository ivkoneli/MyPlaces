package elfak.mosis.myplaces.auth

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
        username : String,
        email: String,
        password: String,
        onSuccess: (AppUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser!!.uid

                val newUser = AppUser(
                    uid = uid,
                    username = username,
                    xp = 0,
                    level = 1,
                    pokemonIds = emptyList()
                )

                db.collection("users").document(uid)
                    .set(newUser)
                    .addOnSuccessListener {
                        addStarterPokemon(uid, onSuccess, onError)
                    }
                    .addOnFailureListener {
                        onError(it.message ?: "Failed to create user profile")
                    }
            }
            .addOnFailureListener {
                onError(it.message ?: "Register failed")
            }
    }

    fun addStarterPokemon(
        ownerId : String,
        onSuccess: (AppUser) -> Unit,
        onError: (String) -> Unit
    ){
        val starterPokemon = Pokemon(
            name = "Pikachu",
            maxhp = 100,
            currenthp = 100,
            attack = 15,
            ownerId = ownerId,
            level = 1
        )
        db.collection("pokemons")
            .add(starterPokemon)
            .addOnSuccessListener { docRef ->
                db.collection("users").document(ownerId)
                    .update("pokemonIds", listOf(docRef.id))
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
            currenthp = 0
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
