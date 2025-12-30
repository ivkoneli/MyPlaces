package elfak.mosis.myplaces

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.ChipGroup
import elfak.mosis.myplaces.data.MyPlace
import elfak.mosis.myplaces.model.LocationViewModel
import elfak.mosis.myplaces.model.MyPlacesViewModel


class EditFragment : Fragment() {

    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()
    private val locationViewModel: LocationViewModel by activityViewModels()

    private lateinit var placeTypeContent: LinearLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var setLocationButton: Button
    private lateinit var addButton: Button
    private lateinit var cancelButton: Button

    // Dinamički EditText/e koji se ubacuju
    private var editName: EditText? = null
    private var editDesc: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // View binding
        chipGroup = view.findViewById(R.id.chipGroupPlaceType)
        placeTypeContent = view.findViewById(R.id.placeTypeContent)
        setLocationButton = view.findViewById(R.id.editmyplace_location_button)
        addButton = view.findViewById(R.id.editmyplace_finished_button)
        cancelButton = view.findViewById(R.id.editmyplace_cancel_button)


        requireActivity().title =
            if (myPlacesViewModel.selected == null) "Add Place"
            else "Edit Place"


        // default selekcija – Pokemon
        if (chipGroup.checkedChipId == View.NO_ID) {
            chipGroup.check(R.id.chipPokemon)
            showPokemonFields()
        }

        // ChipGroup listener: menja sadržaj u ScrollView-u
        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipPokemon -> showPokemonFields()
                R.id.chipPokestop -> showPokestopFields()
                R.id.chipHealing -> showHealingFields()
            }
        }

        locationViewModel.longitude.observe(viewLifecycleOwner) { updateLocationButtonColor() }
        locationViewModel.latitude.observe(viewLifecycleOwner) { updateLocationButtonColor() }

        // Ako imamo selektovan MyPlace, postavi tip i podatke
        myPlacesViewModel.selected?.let { selected ->

            when (selected.type) {
                "Pokemon" -> chipGroup.check(R.id.chipPokemon)
                "Pokestop" -> chipGroup.check(R.id.chipPokestop)
                "Healing" -> chipGroup.check(R.id.chipHealing)
            }

            editName?.setText(selected.name)
            editDesc?.setText(selected.description)

            if (
                locationViewModel.longitude.value.isNullOrBlank() &&
                locationViewModel.latitude.value.isNullOrBlank()
            ) {
                locationViewModel.setLocation(
                    selected.longitude,
                    selected.latitude
                )
            }

        }

        setLocationButton.setOnClickListener {
            locationViewModel.isSettingLocation = true
            findNavController().navigate(R.id.action_EditFragment_to_MapFragment)
        }

        addButton.setOnClickListener {
            val name = editName?.text.toString()
            val desc = editDesc?.text.toString()
            val type = when (chipGroup.checkedChipId) {
                R.id.chipPokemon -> "Pokemon"
                R.id.chipPokestop -> "Pokestop"
                R.id.chipHealing -> "Healing"
                else -> ""
            }
            val lon = locationViewModel.longitude.value ?: ""
            val lat = locationViewModel.latitude.value ?: ""

            if (name.isBlank() || desc.isBlank()) {
                Toast.makeText(requireContext(), "Name and description required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (lon.isNullOrBlank() || lat.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please set location on the map", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (myPlacesViewModel.selected != null) {
                myPlacesViewModel.selected?.apply {
                    this.name = name
                    this.description = desc
                    this.longitude = lon
                    this.latitude = lat
                    this.type = type
                }
            } else {
                myPlacesViewModel.addPlace(MyPlace(name, desc, lon, lat, type))
            }

            myPlacesViewModel.selected = null
            locationViewModel.setLocation("", "")
            findNavController().popBackStack()
        }

        cancelButton.setOnClickListener {
            myPlacesViewModel.selected = null
            locationViewModel.setLocation("", "")
            findNavController().popBackStack()
        }

    }

    private fun showPokemonFields() {
        placeTypeContent.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val layout = inflater.inflate(R.layout.content_pokemon, placeTypeContent, false)
        placeTypeContent.addView(layout)
        editName = layout.findViewById(R.id.edit_pokemon_name)
        editDesc = layout.findViewById(R.id.edit_pokemon_desc)
    }

    private fun showPokestopFields() {
        placeTypeContent.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val layout = inflater.inflate(R.layout.content_pokestop, placeTypeContent, false)
        placeTypeContent.addView(layout)
        editName = layout.findViewById(R.id.edit_pokestop_name)
        editDesc = layout.findViewById(R.id.edit_pokestop_desc)
    }

    private fun showHealingFields() {
        placeTypeContent.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val layout = inflater.inflate(R.layout.content_healing, placeTypeContent, false)
        placeTypeContent.addView(layout)
        editName = layout.findViewById(R.id.edit_healing_name)
        editDesc = layout.findViewById(R.id.edit_healing_desc)
    }

    private fun updateLocationButtonColor() {
        val lon = locationViewModel.longitude.value
        val lat = locationViewModel.latitude.value
        if (!lon.isNullOrBlank() && !lat.isNullOrBlank()) {
            setLocationButton.text = "Location added ✓"
            setLocationButton.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.button_enabled)
        } else {
            setLocationButton.text = "Add Location"
            setLocationButton.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.button_disabled)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

}