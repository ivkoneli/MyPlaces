package elfak.mosis.myplaces

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import elfak.mosis.myplaces.auth.AuthRepository
import elfak.mosis.myplaces.data.Pokemon
import elfak.mosis.myplaces.databinding.FragmentHomeBinding
import elfak.mosis.myplaces.model.UserViewModel
import org.w3c.dom.Text

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val userViewModel : UserViewModel by activityViewModels()
    private lateinit var bottomNav: BottomNavigationView


    private val binding get() = _binding!!
    private val authRepo = AuthRepository()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val welcomeCardUser = view.findViewById<TextView>(R.id.welcome_card_user)
        val btnExplore = view.findViewById<Button>(R.id.btn_explore)
        bottomNav = view.findViewById(R.id.bottomNav)

        val user = userViewModel.currentUser.value

        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null && user.pokemonIds.isEmpty()) {
                showStarterDialog()
                bottomNav.isEnabled = false
            }
        }

        if (user != null) {
            welcomeCardUser.text  = user.username
        }

        btnExplore.setOnClickListener {
            findNavController().navigate(R.id.MapFragment)
        }


        val navController = findNavController()
        bottomNav.selectedItemId = R.id.HomeFragment

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

    private fun showStarterDialog() {

        val dialog = StarterDialog { selectedPokemon ->

            val user = userViewModel.currentUser.value ?: return@StarterDialog

            val pokemon = when (selectedPokemon) {
                "Pikachu" -> Pokemon(
                    name = "Pikachu",
                    maxhp = 100,
                    currenthp = 100,
                    attack = 15,
                    ownerId = user.uid,
                    level = 1,
                    alive = true
                )

                "Charmander" -> Pokemon(
                    name = "Charmander",
                    maxhp = 80,
                    currenthp = 80,
                    attack = 20,
                    ownerId = user.uid,
                    level = 1,
                    alive = true
                )

                "Bulbasaur" -> Pokemon(
                    name = "Bulbasaur",
                    maxhp = 120,
                    currenthp = 120,
                    attack = 10,
                    ownerId = user.uid,
                    level = 1,
                    alive = true
                )

                else -> return@StarterDialog
            }

            authRepo.addStarterPokemon(
                user.uid,
                pokemon,
                onSuccess = {updatedUser ->
                    userViewModel.currentUser.value = updatedUser
                    bottomNav.isEnabled = true
                },
                onError = {
                    // toast
                }
            )
        }

        dialog.show(parentFragmentManager, "StarterDialog")
    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main , menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.action_my_places_list -> {
                this.findNavController().navigate(R.id.action_HomeFragment_to_ListFragment)
                true
            }
            R.id.action_new_place -> {
                this.findNavController().navigate(R.id.action_HomeFragment_to_EditFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}