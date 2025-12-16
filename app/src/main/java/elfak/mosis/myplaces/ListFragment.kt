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
import elfak.mosis.myplaces.data.MyPlace
import elfak.mosis.myplaces.databinding.FragmentListBinding
import elfak.mosis.myplaces.model.MyPlacesViewModel

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null


    private val binding get() = _binding!!
    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()

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
        val myPlacesList: ListView = requireView().findViewById<ListView>(R.id.my_places_list)
        myPlacesList.adapter = ArrayAdapter<MyPlace>( view.context, android.R.layout.simple_list_item_1,myPlacesViewModel.myPLacesList)
        myPlacesList.setOnItemClickListener( object  : AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                var myPlace:MyPlace= p0?.adapter?.getItem(p2) as MyPlace
                myPlacesViewModel.selected = myPlace
                view.findNavController().navigate(R.id.action_ListFragment_to_ViewFragment)
            }
        })
        myPlacesList.setOnCreateContextMenuListener( object : View.OnCreateContextMenuListener {
            override fun onCreateContextMenu( menu : ContextMenu , v: View?, menuInfo:ContextMenuInfo) {
                val info = menuInfo as AdapterContextMenuInfo
                val myPlace:MyPlace = myPlacesViewModel.myPLacesList[info.position]
                menu.setHeaderTitle(myPlace.name)
                menu.add(0, 1, 1, "View place")
                menu.add(0, 2, 2, "Edit place")
                menu.add(0, 3, 3, "Delete Place")
                menu.add(0, 4, 4,  "Show on map")
            }
        })
    }


    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo
        if (item.itemId === 1){
            myPlacesViewModel.selected = myPlacesViewModel.myPLacesList[info.position]
            this.findNavController().navigate(R.id.action_ListFragment_to_ViewFragment)
        }else if ( item.itemId === 2){
            myPlacesViewModel.selected = myPlacesViewModel.myPLacesList[info.position]
            this.findNavController().navigate(R.id.action_ListFragment_to_EditFragment)
        }else if (item.itemId === 3) {
            myPlacesViewModel.myPLacesList.removeAt(info.position)
            val myplacesList: ListView = requireView().findViewById<ListView>(R.id.my_places_list)
            myplacesList.adapter = this@ListFragment.context?.let {ArrayAdapter<MyPlace>(it , android.R.layout.simple_list_item_1, myPlacesViewModel.myPLacesList) }
        }else if (item.itemId === 4){
            myPlacesViewModel.selected = myPlacesViewModel.myPLacesList[info.position]
            this.findNavController().navigate(R.id.action_ListFragment_to_MapFragment)
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