package elfak.mosis.myplaces

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

        // RecyclerView setup
        val recyclerView = view.findViewById<RecyclerView>(R.id.leaderboardRecyclerView)
        adapter = UserLeaderboardAdapter(emptyList())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Fetch leaderboard
        userViewModel.fetchUserWins { userWins ->
            Log.d("LEADERBOARD", "Fetched ${userWins.size} users")
            userWins.forEach { Log.d("LEADERBOARD", it.toString()) }
            adapter.updateData(userWins.take(50)) // samo prvih 50
        }
    }
}


