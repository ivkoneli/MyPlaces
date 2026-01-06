package elfak.mosis.myplaces

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.myplaces.data.MyPlace
import elfak.mosis.myplaces.model.LocationViewModel
import elfak.mosis.myplaces.model.MyPlacesViewModel
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

    lateinit var map: MapView
    private val locationViewModel: LocationViewModel by activityViewModels()
    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()

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
            observeMyPlaces()
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

    private fun addMyPlaceMarkers(places : List<MyPlace>) {
        map.overlays.removeAll { it is Marker }

        places.forEach { place ->
            val marker = Marker(map)

            marker.position = GeoPoint(place.latitude.toDouble(), place.longitude.toDouble())
            marker.title = "Lv.${place.level} ${place.name}"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            val infoWindow = object : MarkerInfoWindow(R.layout.custom_marker_info, map) {
                override fun onOpen(item: Any?) {

                    val title = mView.findViewById<TextView>(R.id.bubble_title)
                    val sub = mView.findViewById<TextView>(R.id.bubble_subdescription)
                    val button = mView.findViewById<Button>(R.id.btn_fight)

                    title.text = marker.title
                    sub.text = marker.subDescription

                    button.setOnClickListener {
                        marker.closeInfoWindow()

                        val dialog = PokemonBattleDialog.newInstance(place)
                        dialog.show(parentFragmentManager, "pokemon_battle")
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
                if (locationViewModel.isSettingLocation) { // block click while setting location
                    false //
                } else {
                    myPlacesViewModel.selected = place
                    m.showInfoWindow()
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
    private fun observeMyPlaces() {
        myPlacesViewModel.myPlacesList.observe(viewLifecycleOwner){ list ->
            addMyPlaceMarkers(list)
        }
    }
    private fun setMyLocationOverlay()
    {
        var myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(activity), map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)
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