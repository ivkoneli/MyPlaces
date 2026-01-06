package elfak.mosis.myplaces

import android.os.Bundle
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.fragment.app.Fragment
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import elfak.mosis.myplaces.data.MyPlace
import elfak.mosis.myplaces.databinding.FragmentListBinding
import elfak.mosis.myplaces.model.MyPlacesViewModel
import elfak.mosis.myplaces.model.UserViewModel

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null


    private val binding get() = _binding!!
    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val myPlacesListView: ListView = binding.myPlacesList
        val adapter = ArrayAdapter<MyPlace>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        myPlacesListView.adapter = adapter

        myPlacesViewModel.myPlacesList.observe(viewLifecycleOwner) { list ->
            adapter.clear()
            adapter.addAll(list)
            adapter.notifyDataSetChanged()
        }

        var currentUser = userViewModel.currentUser.value?.uid
        myPlacesViewModel.fetchUserLocations(currentUser.toString())

        myPlacesListView.setOnItemClickListener { parent, _, position, _ ->
            val myPlace = parent.getItemAtPosition(position) as MyPlace
            myPlacesViewModel.selected = myPlace
            findNavController().navigate(R.id.action_ListFragment_to_EditFragment)
        }

        myPlacesListView.setOnCreateContextMenuListener { menu, v, menuInfo ->
            val info = menuInfo as AdapterView.AdapterContextMenuInfo
            val myPlace = myPlacesViewModel.myPlacesList.value?.get(info.position) ?: return@setOnCreateContextMenuListener
            menu.setHeaderTitle(myPlace.name)
            menu.add(0, 1, 1, "View place")
            menu.add(0, 2, 2, "Edit place")
            menu.add(0, 3, 3, "Delete place")
            menu.add(0, 4, 4, "Show on map")
        }

        super.onViewCreated(view, savedInstanceState)

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNav)
        val navController = findNavController()

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


    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val myPlace = myPlacesViewModel.myPlacesList.value?.get(info.position) ?: return super.onContextItemSelected(item)

        when (item.itemId) {
            1 -> { // View
                myPlacesViewModel.selected = myPlace
                findNavController().navigate(R.id.action_ListFragment_to_ViewFragment)
            }
            2 -> { // Edit
                myPlacesViewModel.selected = myPlace
                findNavController().navigate(R.id.action_ListFragment_to_EditFragment)
            }
            3 -> { // Delete
                myPlacesViewModel.removeLocation(myPlace)
            }
            4 -> { // Show on map
                myPlacesViewModel.selected = myPlace
                findNavController().navigate(R.id.action_ListFragment_to_MapFragment)
            }
        }
        return super.onContextItemSelected(item)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main , menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.action_my_places_list -> {
                this.findNavController().navigate(R.id.action_ListFragment_to_EditFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val item = menu.findItem(R.id.action_my_places_list)
        item.isVisible = false ;
    }
}