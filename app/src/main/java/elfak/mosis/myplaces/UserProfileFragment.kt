package elfak.mosis.myplaces

import AvatarAdapter
import PokemonAdapter
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import elfak.mosis.myplaces.data.AppUser
import elfak.mosis.myplaces.model.UserViewModel

class UserProfileFragment : Fragment(R.layout.fragment_user_profile) {

    private val userViewModel: UserViewModel by activityViewModels()
    private var loadedUser: AppUser? = null
    private lateinit var adapter: PokemonAdapter

    // UI refs
    private lateinit var usernameText: TextView
    private lateinit var levelText: TextView
    private lateinit var xpText: TextView
    private lateinit var xpProgress: ProgressBar
    private lateinit var winsText: TextView
    private lateinit var losesText: TextView
    private lateinit var avatarImage: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PROFILE", "onViewCreated userId=${userViewModel.visitedUserId}")

        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        // bind UI
        usernameText = view.findViewById(R.id.usernameInput)
        levelText = view.findViewById(R.id.levelText)
        xpText = view.findViewById(R.id.xpText)
        xpProgress = view.findViewById(R.id.xpProgress)
        winsText = view.findViewById(R.id.winText)
        losesText = view.findViewById(R.id.lossText)
        avatarImage = view.findViewById(R.id.avatarImage)

        // recycler
        val recyclerView = view.findViewById<RecyclerView>(R.id.pokemonList)
        adapter = PokemonAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // argument
        val username = arguments?.getString("username") ?: return

        // fetch user po username
        userViewModel.fetchUserByUsername(username) { user ->
            if (user == null) return@fetchUserByUsername
            loadedUser = user

            // **Popuni sve UI podatke uključujući avatar**
            populateProfileUI(user)
        }
    }

    private fun populateProfileUI(user: AppUser) {
        usernameText.text = user.username
        levelText.text = "Level: ${user.level}"

        val xpNeeded = user.level * 100
        val xpPercent = ((user.xp.toFloat() / xpNeeded) * 100).coerceIn(0f, 100f).toInt()
        xpText.text = "XP: $xpPercent%"
        xpProgress.progress = xpPercent
        winsText.text = "Wins: ${user.wins}"
        losesText.text = "Loses: ${user.loses}"

        // **AVATAR LOGIKA**
        val avatarPath = user.localAvatarPath
        Log.d("USER_PROFILE", "Avatar path: $avatarPath")

        when {
            !avatarPath.isNullOrEmpty() -> {
                if (avatarPath.startsWith("drawable://")) {
                    val resId = avatarPath.removePrefix("drawable://").toInt()
                    avatarImage.setImageResource(resId)
                } else {
                    val bitmap = BitmapFactory.decodeFile(avatarPath)
                    avatarImage.setImageBitmap(bitmap)
                }
            }
            else -> avatarImage.setImageResource(R.drawable.ic_pokemon_placeholder)
        }

        // fetch pokemone
        userViewModel.fetchUserPokemons(user.uid) { pokemons ->
            Log.d("USER_PROFILE", "Pokemons fetched: ${pokemons.size}")
            adapter.submitList(pokemons)
        }
    }

    override fun onResume() {
        super.onResume()
        val user = loadedUser ?: return
        userViewModel.fetchUserPokemons(user.uid) { pokemons ->
            adapter.submitList(pokemons)
        }
    }
}
