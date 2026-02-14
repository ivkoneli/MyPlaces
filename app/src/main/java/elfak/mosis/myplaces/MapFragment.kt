package elfak.mosis.myplaces

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import android.R.layout.simple_dropdown_item_1line
import androidx.preference.PreferenceManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.myplaces.data.AppUser
import elfak.mosis.myplaces.data.MyPlace
import elfak.mosis.myplaces.model.LocationViewModel
import elfak.mosis.myplaces.model.MyPlacesViewModel
import elfak.mosis.myplaces.model.UserViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*


class MapFragment : Fragment() {

    private lateinit var locationProvider: GpsMyLocationProvider
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    lateinit var map: MapView

    private lateinit var adapter: FilteredListAdapter

    private val sortOptions = listOf("Distance", "Level", "Date")
    private lateinit var sortDropdown: MaterialAutoCompleteTextView
    private var currentSort = "Distance"  // default


    private val locationViewModel: LocationViewModel by activityViewModels()
    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()
    private val userViewModel : UserViewModel by activityViewModels()

    private lateinit var typeChipGroup: ChipGroup
    private lateinit var chipPokemon: Chip
    private lateinit var chipPokestop: Chip
    private lateinit var chipHeal: Chip

    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val loadingOverlay = view.findViewById<View>(R.id.map_loading_overlay)

        var ctx: Context? = getActivity()?.getApplicationContext()
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences((ctx!!)))
        map = requireView().findViewById<MapView>(R.id.map)
        map.setMultiTouchControls(true)
        if (ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }else {
            setupMap()
            observeMyPlaces(loadingOverlay)
        }

        adapter = FilteredListAdapter { place ->
            val lat = place.latitude.toDoubleOrNull() ?: 0.0
            val lon = place.longitude.toDoubleOrNull() ?: 0.0
            map.controller.setCenter(GeoPoint(lat, lon))
        }
        // Setup horizontal RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.placesRecyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val sortSpinner: Spinner = view.findViewById(R.id.sortSpinner)


        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val selected = parent.getItemAtPosition(position).toString()
                currentSort = selected
                sortMyPlaces()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val defaultIndex = resources.getStringArray(R.array.sort_options).indexOf("Distance")
        if (defaultIndex >= 0) sortSpinner.setSelection(defaultIndex)

        super.onViewCreated(view, savedInstanceState)


        typeChipGroup = view.findViewById(R.id.type_chip_group)
        chipPokemon = view.findViewById(R.id.chip_pokemon)
        chipPokestop = view.findViewById(R.id.chip_pokestop)
        chipHeal = view.findViewById(R.id.chip_heal)

        chipPokemon.setColor(R.color.pokemon_blue_faded)
        chipHeal.setColor(R.color.heal_green_faded)
        chipPokestop.setColor(R.color.pokestop_yellow_faded)

        typeChipGroup.setOnCheckedChangeListener { _, checkedId ->

            // reset svih chipova na faded
            chipPokemon.setColor(R.color.pokemon_blue_faded)
            chipHeal.setColor(R.color.heal_green_faded)
            chipPokestop.setColor(R.color.pokestop_yellow_faded)

            when (checkedId) {
                R.id.chip_pokemon -> {
                    myPlacesViewModel.selectedType = "Pokemon"
                    chipPokemon.setColor(R.color.pokemon_blue)
                }
                R.id.chip_pokestop -> {
                    myPlacesViewModel.selectedType = "Pokestop"
                    chipPokestop.setColor(R.color.pokestop_yellow)
                }
                R.id.chip_heal -> {
                    myPlacesViewModel.selectedType = "Healing"
                    chipHeal.setColor(R.color.heal_green)
                }
                else -> {
                    myPlacesViewModel.selectedType = null
                }
            }

            myPlacesViewModel.applyFilters()
        }



        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNav)
        val navController = findNavController()
        bottomNav.selectedItemId = R.id.MapFragment

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

        val searchInput = view.findViewById<EditText>(R.id.search_input)
        searchInput.addTextChangedListener { text ->
            searchRunnable?.let { searchHandler.removeCallbacks(it) }

            searchRunnable = Runnable {
                myPlacesViewModel.searchQuery = text?.toString()
                myPlacesViewModel.applyFilters()
            }

            searchHandler.postDelayed(searchRunnable!!, 300)
        }

        val filterButton = view.findViewById<ImageButton>(R.id.filter_button)
        val distanceSlider = view.findViewById<Slider>(R.id.distance_slider)
        val sliderContainer = view.findViewById<LinearLayout>(R.id.slider_container)

        filterButton.setOnClickListener {
            sliderContainer.visibility =
                if (sliderContainer.visibility == View.GONE) View.VISIBLE
                else View.GONE
        }

        distanceSlider.value = 1000f
        myPlacesViewModel.maxDistanceMeters = 1000f

        distanceSlider.setLabelFormatter { value ->
            if (value < 1000) {
                "${value.toInt()} m"
            } else {
                val km = value / 1000f
                if (km % 1f == 0f) {
                    "${km.toInt()} km"
                } else {
                    String.format("%.1f km", km)
                }
            }
        }

        distanceSlider.addOnChangeListener { _, value, _ ->
            myPlacesViewModel.maxDistanceMeters = value
            myPlacesViewModel.applyFilters()
        }

    }
    private fun isUserNearby(place: MyPlace, maxDistance: Float = 250f): Boolean {

        val userLocation = myPlacesViewModel.currentUserLocation ?: return false
        val (userLat, userLon) = userLocation

        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            userLat,
            userLon,
            place.latitude.toDouble(),
            place.longitude.toDouble(),
            results
        )
        return results[0] <= maxDistance
    }

    private fun addMyPlaceMarkers(places : List<MyPlace>) {
        map.overlays.removeAll { it is Marker }

        places.forEach { place ->
            val marker = Marker(map)

            marker.position = GeoPoint(place.latitude.toDouble(), place.longitude.toDouble())
            marker.title = "Lv.${place.level} ${place.name}"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            val infoWindow = object : MarkerInfoWindow(R.layout.custom_marker_info, map) {
                override fun onOpen(item: Any?) {

                    val user = userViewModel.currentUser.value ?: null
                    val title = mView.findViewById<TextView>(R.id.bubble_title)
                    val status = mView.findViewById<TextView>(R.id.bubble_status)
                    val button = mView.findViewById<Button>(R.id.btn_fight)
                    val progressBar = mView.findViewById<ProgressBar>(R.id.healProgressBar)

                    title.text = "Lv.${place.level} ${place.name}"

                    val isNearby = isUserNearby(place)
                    if (!isNearby) {
                        status.visibility = View.VISIBLE
                        status.text = "Move closer to interact"
                        button.isEnabled = false
                        button.alpha = 0.5f
                        return
                    }

                    status.visibility = View.GONE
                    button.isEnabled = true

                    when (place.type) {

                        "Pokemon" -> {
                            button.text = "Fight"

                            button.setOnClickListener {
                                marker.closeInfoWindow()
                                PokemonBattleDialog
                                    .newInstance(place)
                                    .show(parentFragmentManager, "pokemon_battle")
                            }
                        }

                        "Pokestop" -> {
                            button.text = "Collect"

                            val now = System.currentTimeMillis()
                            val cooldown = 10 * 60 * 500 // 5 minuta
                            val last = place.lastCollectedAt ?: 0L
                            val remaining = cooldown - (now - last)

                            // Ako je uzeto pre proveri koliko jos dok ne bude dostupno
                            if (remaining > 0) {
                                val minutes = (remaining / 60000) + 1
                                status.visibility = View.VISIBLE
                                status.text = "Available in $minutes min"
                                button.isEnabled = false

                                // Timer
                                val timer = object : CountDownTimer(remaining, 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        val mins = (millisUntilFinished / 60000)
                                        val secs = (millisUntilFinished % 60000) / 1000
                                        status.text = String.format("Available in %02d:%02d min", mins, secs)
                                    }

                                    override fun onFinish() {
                                        status.visibility = View.GONE
                                        button.isEnabled = true
                                    }
                                }
                                timer.start()

                            }
                            // Ako je dostupno omoguci klik
                            else {
                                status.visibility = View.GONE
                                button.isEnabled = true
                                button.setOnClickListener {
                                    marker.closeInfoWindow()
                                    val user = userViewModel.currentUser.value ?: return@setOnClickListener

                                    collectPokestopXp(user, place, 50) {
                                        // Otvori XP dijalog samo za korisnika
                                        XpRewardDialog( pokemon = null, gainedXp = 50)
                                            .show(parentFragmentManager, "xp_dialog")

                                        // Update infoWindow odmah
                                        status.visibility = View.VISIBLE
                                        status.text = "Available in 5:00 min"
                                        button.isEnabled = false

                                        // Start cooldown timer
                                        object : CountDownTimer(cooldown.toLong(), 1000) {
                                            override fun onTick(millisUntilFinished: Long) {
                                                val mins = (millisUntilFinished / 60000)
                                                val secs = (millisUntilFinished % 60000) / 1000
                                                status.text = String.format("Available in %02d:%02d min", mins, secs)
                                            }

                                            override fun onFinish() {
                                                status.visibility = View.GONE
                                                button.isEnabled = true
                                            }
                                        }.start()
                                    }
                                }
                            }
                        }

                        "Healing" -> {
                            button.text = "Heal"
                            setupHealingInfoWindow(marker, place,status,button,progressBar)
                        }

                    }
                }

            }
            marker.infoWindow = infoWindow

            // 👉 IKONICA PO TIPU
            marker.icon = when (place.type) {
                "Pokemon" -> scaledBitmapIcon(R.drawable.poke_stop, 20)
                "Pokestop" -> scaledBitmapIcon(R.drawable.poke_xp, 20)
                "Healing" -> scaledBitmapIcon(R.drawable.poke_heal, 20)
                else -> scaledBitmapIcon(R.drawable.poke_stop, 20)
            }

            marker.setOnMarkerClickListener { m, _ ->
                if (locationViewModel.isSettingLocation) {
                    false
                } else {
                    if (m.isInfoWindowOpen) {
                        m.closeInfoWindow()
                    } else {
                        map.overlays.forEach {
                            if (it is Marker && it.isInfoWindowOpen) {
                                it.closeInfoWindow()
                            }
                        }
                        myPlacesViewModel.selected = place
                        m.showInfoWindow()
                    }
                    true
                }
            }
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    private fun scaledBitmapIcon(
        @DrawableRes resId: Int,
        sizeDp: Int
    ): Drawable {
        val drawable = ContextCompat.getDrawable(requireContext(), resId)!!
        val bitmap = (drawable as BitmapDrawable).bitmap

        val density = resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            sizePx,
            sizePx,
            true
        )

        return BitmapDrawable(resources, scaledBitmap)
    }


    private fun setupMap(){
        var startPoint:GeoPoint = GeoPoint(43.3289 , 21.8958)
        map.controller.setZoom(15.0)
        if(locationViewModel.isSettingLocation){
            setOnMapClickOverlay()
        }
        else {
            if(myPlacesViewModel.selected!=null){
                startPoint = GeoPoint(myPlacesViewModel.selected!!.latitude.toDouble(), myPlacesViewModel.selected!!.longitude.toDouble())
            } else {
                setMyLocationOverlay()
            }
        }
        map.controller.animateTo(startPoint)
        myPlacesViewModel.fetchLocations()

    }
    private fun observeMyPlaces(loadingOverlay: View) {
        myPlacesViewModel.myPlacesList.observe(viewLifecycleOwner) { list ->

            // ⛔ nemamo user lokaciju → ne crtamo ništa
            if (myPlacesViewModel.currentUserLocation == null) {
                map.overlays.removeAll { it is Marker }
                map.invalidate()
                loadingOverlay.visibility = View.VISIBLE
                return@observe
            }
            loadingOverlay.visibility = View.GONE
            addMyPlaceMarkers(list)

            sortMyPlaces()
        }
    }



    private fun setMyLocationOverlay()
    {
        locationProvider = GpsMyLocationProvider(requireContext())

        locationProvider.startLocationProvider { location, _ ->
            location?.let {
                myPlacesViewModel.currentUserLocation =
                    it.latitude to it.longitude

                myPlacesViewModel.applyFilters()
            }
        }

        myLocationOverlay = MyLocationNewOverlay(locationProvider, map)

        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()

        map.overlays.add(myLocationOverlay)

        myLocationOverlay.runOnFirstFix {
            val loc = myLocationOverlay.lastFix
            loc?.let {
                requireActivity().runOnUiThread {
                    myPlacesViewModel.currentUserLocation =
                        it.latitude to it.longitude

                    myPlacesViewModel.applyFilters()
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                setMyLocationOverlay()
                setOnMapClickOverlay()
            }
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when( item.itemId) {
            R.id.action_new_place -> {

                myPlacesViewModel.selected = null

                this.findNavController().navigate(R.id.action_MapFragment_to_EditFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setOnMapClickOverlay() {
        map.overlays.removeAll { it is MapEventsOverlay }

        if (!locationViewModel.isSettingLocation) return

        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p ?: return false

                locationViewModel.setLocation(
                    p.longitude.toString(),
                    p.latitude.toString()
                )

                locationViewModel.isSettingLocation = false
                findNavController().popBackStack()
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }

        map.overlays.add(0, MapEventsOverlay(receiver))
    }

    private fun isHealingAvailable(place: MyPlace): Boolean {
        val now = System.currentTimeMillis()
        val cooldown = 10 * 60 * 1500L // 15 minuta
        val last = place.lastHealedAt ?: 0L
        return now - last >= cooldown
    }
    private fun getRemainingHealingTime(place: MyPlace): Long {
        val now = System.currentTimeMillis()
        val cooldown = 10 * 60 * 1500L
        val last = place.lastHealedAt ?: 0L
        return (cooldown - (now - last)).coerceAtLeast(0)
    }

    private fun setupHealingInfoWindow(marker: Marker, place: MyPlace, status: TextView, button: Button, progressBar: ProgressBar) {

        // Proveri da li je healing dostupan
        if (!isHealingAvailable(place)) {
            val minutes = (getRemainingHealingTime(place) / 60000) + 1
            status.visibility = View.VISIBLE
            status.text = "Available in $minutes min"
            button.isEnabled = false
        } else {
            status.visibility = View.GONE
            button.isEnabled = true
            button.text = "Heal"

            button.setOnClickListener {
                startHealing(marker, place, button, status, progressBar)
            }
        }
    }

    private fun startHealing(
        marker: Marker,
        place: MyPlace,
        button: Button,
        status: TextView,
        progressBar: ProgressBar
    ) {
        button.isEnabled = false
        status.visibility = View.VISIBLE
        status.text = "Healing..."
        progressBar.progress = 0

        val healDuration = 5000L // 5 sekundi
        val interval = 100L
        val steps = (healDuration / interval).toInt()
        var currentStep = 0

        val timer = object : Runnable {
            override fun run() {
                currentStep++
                val progress = (currentStep * 100 / steps).coerceAtMost(100)
                progressBar.progress = progress
                status.text = "Healing... $progress%"

                if (currentStep < steps) {
                    progressBar.postDelayed(this, interval)
                } else {
                    completeHealing(place, button, status, progressBar)
                }
            }
        }

        progressBar.post(timer)
    }

    private fun completeHealing(
        place: MyPlace,
        button: Button,
        status: TextView,
        progressBar: ProgressBar
    ) {
        val user = userViewModel.currentUser.value ?: return
        val db = FirebaseFirestore.getInstance()

        // Fetch user pokemone po ownerId
        userViewModel.fetchUserPokemons(user.uid) { pokemons ->
            if (pokemons.isEmpty()) {
                Toast.makeText(requireContext(), "No Pokémons to heal!", Toast.LENGTH_SHORT).show()
                return@fetchUserPokemons
            }

            val batch = db.batch()

            // Update HP svakom Pokémonu na maxhp
            pokemons.forEach { pokemon ->
                val ref = db.collection("pokemons").document(pokemon.id)
                batch.update(ref, "currenthp", pokemon.maxhp)
                batch.update(ref, "alive", true)
            }

            // Update lastHealedAt za ovu lokaciju
            val placeRef = db.collection("locations").document(place.id)
            batch.update(placeRef, "lastHealedAt", System.currentTimeMillis())

            // Commit batch
            batch.commit().addOnSuccessListener {
                Toast.makeText(requireContext(), "All your Pokémons healed!", Toast.LENGTH_SHORT).show()
                status.text = "Healed!"
                progressBar.progress = 100
                button.isEnabled = false // odmah sivo

                // Ne koristimo postDelayed za dugme
                // Umesto toga, postavi lastHealedAt u place objektu
                place.lastHealedAt = System.currentTimeMillis()

                // Ako korisnik ponovo otvori info window, cooldown će se pravilno prikazati
                val minutes = (getRemainingHealingTime(place) / 60000) + 1
                status.text = "Available in $minutes min"

            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Healing failed: ${e.message}", Toast.LENGTH_SHORT).show()
                button.isEnabled = true
                status.visibility = View.GONE
                progressBar.progress = 0
            }
        }
    }




    private fun Chip.setColor(colorRes: Int, textColorRes: Int = android.R.color.white) {
        chipBackgroundColor = ColorStateList.valueOf(
            ContextCompat.getColor(context, colorRes)
        )
        setTextColor(ContextCompat.getColor(context, textColorRes))
    }



    private fun collectPokestopXp(user: AppUser, place: MyPlace, gainedXp: Int = 50, onComplete: (() -> Unit)? = null) {
        val db = FirebaseFirestore.getInstance()

        // 1️⃣ Update korisnikov XP i level
        var newXp = user.xp + gainedXp
        var newLevel = user.level
        var xpThreshold = user.level * 100

        while (newXp >= xpThreshold && newLevel < 30) {
            newXp -= xpThreshold
            newLevel += 1
            xpThreshold = newLevel * 100
        }

        // 2️⃣ Setujemo sadašnji timestamp za lastCollectedAt
        val now = System.currentTimeMillis()

        // 3️⃣ Firestore update u jednom call-u
        val userRef = db.collection("users").document(user.uid)
        val placeRef = db.collection("locations").document(place.id ?: return)

        // Start transaction da bude atomarno
        db.runBatch { batch ->
            batch.update(userRef, mapOf("xp" to newXp, "level" to newLevel))
            batch.update(placeRef, mapOf("lastCollectedAt" to now))
        }.addOnSuccessListener {
            // Update ViewModel
            userViewModel.currentUser.value = user.copy(level = newLevel, xp = newXp)
            place.lastCollectedAt = now

            onComplete?.invoke()

            Toast.makeText(requireContext(),
                "Collected XP +$gainedXp, Level: $newLevel",
                Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(),
                "Failed to collect XP: ${e.message}",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun sortMyPlaces() {
        val list = myPlacesViewModel.myPlacesList.value ?: return
        val sortedList = when(currentSort) {
            "Distance" -> {
                val userLoc = myPlacesViewModel.currentUserLocation
                if (userLoc != null) {
                    list.sortedBy { distanceInMeters(userLoc.first, userLoc.second, it.latitude.toDouble(), it.longitude.toDouble()).toDouble() }
                } else list
            }
            "Level" -> list.sortedByDescending { it.level }
            "Date" -> list.sortedByDescending { it.date ?: System.currentTimeMillis() } // default danasnji datum
            else -> list
        }

        adapter.submitList(sortedList)
    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        var item = menu.findItem(R.id.action_my_places_list)
        item.isVisible = false ;

    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}