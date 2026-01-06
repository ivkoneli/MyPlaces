package elfak.mosis.myplaces

import PokemonAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import elfak.mosis.myplaces.data.Pokemon
import elfak.mosis.myplaces.model.UserViewModel
import org.w3c.dom.Text

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val userViewModel : UserViewModel by activityViewModels()
    private lateinit var adapter : PokemonAdapter
    private val db = FirebaseFirestore.getInstance()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logoutBtn = view.findViewById<Button>(R.id.btnLogout)
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNav)
        val usernameText = view.findViewById<TextView>(R.id.usernameInput)
        val levelText = view.findViewById<TextView>(R.id.levelText)
        val xpText = view.findViewById<TextView>(R.id.xpText)
        val xpProgress = view.findViewById<ProgressBar>(R.id.xpProgress)
        var winsText = view.findViewById<TextView>(R.id.winText)
        var loseText = view.findViewById<TextView>(R.id.lossText)

        val recyclerView = view.findViewById<RecyclerView>(R.id.pokemonList)
        adapter = PokemonAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        // Update user info when the view model changes
        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user ?: return@observe

            usernameText.text = "${user.username}"
            levelText.text = "Level: ${user.level}"

            val xpNeededForNextLevel = user.level * 100
            val xpPercent = ((user.xp.toFloat() / xpNeededForNextLevel) * 100).coerceIn(0f, 100f).toInt()

            xpText.text = "XP: $xpPercent%"
            xpProgress.progress = xpPercent
            winsText.text = "Wins: ${user.wins}"
            loseText.text = "Loses: ${user.loses}"
        }

        logoutBtn.setOnClickListener {
            // Logout - delete user view model
            FirebaseAuth.getInstance().signOut()
            userViewModel.currentUser.value = null

            // Delete the stack so users cant backtrace
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Move on to Login screen
            findNavController().navigate(
                R.id.action_ProfileFragment_to_LoginFragment
            )
        }

        // Bottom nav bar actions
        bottomNav.setOnItemSelectedListener { item ->
            val navController = findNavController()
            when (item.itemId) {
                R.id.HomeFragment -> { navController.navigate(R.id.HomeFragment); true }
                R.id.ListFragment -> { navController.navigate(R.id.ListFragment); true }
                R.id.MapFragment -> { navController.navigate(R.id.MapFragment); true }
                R.id.LeaderboardFragment -> { navController.navigate(R.id.LeaderboardFragment); true }
                R.id.ProfileFragment -> { navController.navigate(R.id.ProfileFragment); true }
                else -> false
            }
        }
    }
    override fun onResume() {
        super.onResume()

        val user = userViewModel.currentUser.value ?: return
        userViewModel.fetchUserPokemons(user.uid) { pokemons ->
            adapter.submitList(pokemons)
        }
    }

}

