package elfak.mosis.myplaces

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import elfak.mosis.myplaces.model.UserViewModel

class LeaderboardFragment : Fragment(R.layout.fragment_leaderboard) {

    private lateinit var adapter: UserLeaderboardAdapter
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.LeaderboardFragment

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

        // RecyclerView setup
        val recyclerView = view.findViewById<RecyclerView>(R.id.leaderboardRecyclerView)
        adapter = UserLeaderboardAdapter(emptyList()) { username ->
            val bundle = Bundle().apply {
                putString("username", username)
            }
            findNavController().navigate(
                R.id.UserProfileFragment,
                bundle
            )
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val btnWins = view.findViewById<Button>(R.id.btnWins)
        val btnLevel = view.findViewById<Button>(R.id.btnLevel)

        // --- Funkcija za selektovanje tab-a ---
        fun selectTab(selected: Button, other: Button) {
            // svetlija nijansa za selektovani, normalna za drugi
            selected.setBackgroundColor(Color.parseColor("#DDF4FF")) // svetla plava
            other.setBackgroundColor(Color.parseColor("#A3CBEF")) // normalna plava

            val scale = 1.05f
            selected.scaleX = scale
            selected.scaleY = scale
            other.scaleX = 1f
            other.scaleY = 1f
        }


        // Funkcija za load leaderboard + fetch avatara
        fun loadLeaderboard(fetchWins: Boolean) {
            if (fetchWins) {
                adapter.setLevelMode(false)

                userViewModel.fetchUserWins { userWins ->
                    val topList = userWins.take(50)
                    adapter.updateData(topList)

                    val usernames = topList.map { it.first }
                    userViewModel.fetchUserAvatars(usernames) { avatarMap ->
                        adapter.updateAvatars(avatarMap)
                    }
                }
            } else {
                adapter.setLevelMode(true)

                userViewModel.fetchUserLevels { userLevels ->
                    val topList = userLevels.take(50)
                    adapter.updateData(topList)

                    val usernames = topList.map { it.first }
                    userViewModel.fetchUserAvatars(usernames) { avatarMap ->
                        adapter.updateAvatars(avatarMap)
                    }
                }
            }
        }


        // inicijalno: Wins tab selektovan
        selectTab(btnWins, btnLevel)
        loadLeaderboard(fetchWins = true)

        // klik na Wins
        btnWins.setOnClickListener {
            selectTab(btnWins, btnLevel)
            loadLeaderboard(fetchWins = true)
        }

        // klik na Level
        btnLevel.setOnClickListener {
            selectTab(btnLevel, btnWins)
            loadLeaderboard(fetchWins = false)
        }
    }
}