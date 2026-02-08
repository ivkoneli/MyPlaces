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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import elfak.mosis.myplaces.data.MyPlace
import elfak.mosis.myplaces.model.LocationViewModel
import elfak.mosis.myplaces.model.MyPlacesViewModel
import elfak.mosis.myplaces.model.UserViewModel


class EditFragment : Fragment() {

    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()
    private val locationViewModel: LocationViewModel by activityViewModels()

    private lateinit var placeTypeContent: LinearLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var setLocationButton: Button
    private lateinit var addButton: Button
    private lateinit var cancelButton: Button

    // Dinamički EditText koji se ubacuju
    private var editName: EditText? = null
    private var levelSeekBar: SeekBar? = null
    private var levelText: TextView? = null

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
        var addedByText = view.findViewById<TextView>(R.id.addedByText)

        editName = view.findViewById(R.id.edit_pokemon_name)

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
            addedByText.text = "Added by: Loading..."

            myPlacesViewModel.selected?.let { selected ->
                editName?.setText(selected.name)

                myPlacesViewModel.fetchUsername(selected.userID) { username ->
                    addedByText.text = "Added by: $username"
                }
            }

            val level = selected.level
            levelSeekBar?.progress = level - 1
            levelText?.text = "Level: $level"

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

            val name = getCurrentName()
            val level = (levelSeekBar?.progress ?: 0) + 1
            val type = when (chipGroup.checkedChipId) {
                R.id.chipPokemon -> "Pokemon"
                R.id.chipPokestop -> "Pokestop"
                R.id.chipHealing -> "Healing"
                else -> ""
            }
            val lon = locationViewModel.longitude.value ?: ""
            val lat = locationViewModel.latitude.value ?: ""

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Name and description required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (lon.isNullOrBlank() || lat.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please set location on the map", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userViewModel : UserViewModel by activityViewModels()
            val currentUser = userViewModel.currentUser.value

            if (myPlacesViewModel.selected != null) {
                myPlacesViewModel.selected?.let { selected ->
                    val updated = selected.copy(
                        name = name,
                        level = level,
                        longitude = lon,
                        latitude = lat,
                        type = type,
                        userID = selected.userID
                    )
                    myPlacesViewModel.updateLocation(updated)
                }
            } else {
                myPlacesViewModel.addLocation(MyPlace(
                    name = name, level =  level, longitude =  lon, latitude =  lat,type = type, userID = currentUser?.uid.toString()))
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
        levelSeekBar = layout.findViewById(R.id.seek_pokemon_level)
        levelText = layout.findViewById(R.id.text_pokemon_level)

        // init
        levelSeekBar?.progress = 0
        levelText?.text = "Level: 1"

        levelSeekBar?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                val level = progress + 1
                levelText?.text = "Level: $level"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }


    private fun showPokestopFields() {
        placeTypeContent.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val layout = inflater.inflate(R.layout.content_pokestop, placeTypeContent, false)
        placeTypeContent.addView(layout)

        editName = layout.findViewById(R.id.edit_pokestop_name)
        levelText = layout.findViewById(R.id.text_pokestop_level)
        levelSeekBar = layout.findViewById(R.id.seek_pokestop_level) // moraš dodati seekbar u XML

        // init
        levelSeekBar?.progress = 0
        levelText?.text = "Level: 1"

        levelSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val level = progress + 1
                levelText?.text = "Level: $level"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun showHealingFields() {
        placeTypeContent.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val layout = inflater.inflate(R.layout.content_healing, placeTypeContent, false)
        placeTypeContent.addView(layout)

        editName = layout.findViewById(R.id.edit_healing_name)
        levelText = layout.findViewById(R.id.text_healing_level)
        levelSeekBar = layout.findViewById(R.id.seek_healing_level) // moraš dodati seekbar u XML

        // init
        levelSeekBar?.progress = 0
        levelText?.text = "Level: 1"

        levelSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val level = progress + 1
                levelText?.text = "Level: $level"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }


    private fun getCurrentName(): String {
        return editName?.text?.toString()?.trim() ?: ""
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