package elfak.mosis.myplaces

import AvatarAdapter
import PokemonAdapter
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import elfak.mosis.myplaces.data.AppUser
import elfak.mosis.myplaces.data.Pokemon
import elfak.mosis.myplaces.model.UserViewModel
import org.w3c.dom.Text
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val userViewModel : UserViewModel by activityViewModels()
    private lateinit var adapter : PokemonAdapter
    private val db = FirebaseFirestore.getInstance()

    // UI elementi kao class members
    private lateinit var usernameText: TextView
    private lateinit var levelText: TextView
    private lateinit var xpText: TextView
    private lateinit var xpProgress: ProgressBar
    private lateinit var winsText: TextView
    private lateinit var loseText: TextView
    private lateinit var logoutBtn: Button
    private lateinit var sortSpinner: Spinner
    private lateinit var checkboxAlive: CheckBox
    private lateinit var btnSortToggle: ImageButton

    private var currentPokemons: List<Pokemon> = emptyList()
    private var isAscending = false // default descending
    private var visitedUsername: String? = null

    private lateinit var avatarImage: ImageView
    private var cameraPhotoUri: Uri? = null
    private var cameraFilePath: String? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        visitedUsername = arguments?.getString("username") // username od posete, ako postoji
        avatarImage = view.findViewById(R.id.avatarImage)

        logoutBtn = view.findViewById(R.id.btnLogout)
        usernameText = view.findViewById(R.id.usernameInput)
        levelText = view.findViewById(R.id.levelText)
        xpText = view.findViewById(R.id.xpText)
        xpProgress = view.findViewById(R.id.xpProgress)
        winsText = view.findViewById(R.id.winText)
        loseText = view.findViewById(R.id.lossText)

        sortSpinner = view.findViewById(R.id.sortSpinner)
        checkboxAlive = view.findViewById(R.id.checkboxAlive)
        btnSortToggle = view.findViewById(R.id.btnSortToggle)

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.ProfileFragment


        val recyclerView = view.findViewById<RecyclerView>(R.id.pokemonList)
        adapter = PokemonAdapter(
            isBattleMode = false,
            showDisown = true, // 🌟 samo ovde se pojavljuje dugme
            onClick = { pokemon ->
                // ovo je normalni klik
            },
            onDisownClick = { pokemon ->
                val currentUserId = userViewModel.currentUser.value?.uid ?: return@PokemonAdapter

                userViewModel.disownPokemon(currentUserId, pokemon.id) { success ->
                    if (success) {
                        currentPokemons = currentPokemons.filter { it.id != pokemon.id }
                        refreshPokemonList()
                    } else {
                        Toast.makeText(requireContext(), "Failed to disown Pokemon", Toast.LENGTH_SHORT).show()
                    }
                }

            }

        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        val currentUser = userViewModel.currentUser.value

        if (visitedUsername != null) {
            // Gledamo tuđi profil
            userViewModel.fetchUserByUsername(visitedUsername!!) { user ->
                if (user == null) return@fetchUserByUsername
                populateProfileUI(user)
                logoutBtn.visibility = View.GONE
            }

        } else if (currentUser != null) {
            // Gledamo svoj profil
            populateProfileUI(currentUser)
            logoutBtn.visibility = View.VISIBLE
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

        avatarImage.setOnClickListener {
            if (visitedUsername != null) return@setOnClickListener // samo svoj profil

            val avatarList = listOf(
                R.drawable.ic_avatar,
                R.drawable.ic_avatar,
                R.drawable.ic_avatar,
                R.drawable.ic_avatar,
                R.drawable.ic_avatar,
                R.drawable.ic_avatar,
                R.drawable.ic_avatar,
                R.drawable.ic_avatar,
                R.drawable.ic_avatar,
                R.drawable.ic_avatar
            )

            val dialogView = layoutInflater.inflate(R.layout.dialog_avatar_picker, null)
            val gridView = dialogView.findViewById<GridView>(R.id.gridAvatars)

            gridView.adapter = AvatarAdapter(requireContext(), avatarList) { selected, isCamera ->
                when {
                    selected != null -> { // drawable
                        avatarImage.setImageResource(selected)
                        userViewModel.updateAvatarDrawable(selected)
                    }
                    isCamera -> {
                        val avatarDir = File(requireContext().filesDir, "avatars")
                        if (!avatarDir.exists()) avatarDir.mkdir()

                        val file = File(
                            avatarDir,
                            "avatar_camera_${System.currentTimeMillis()}.png"
                        )
                        cameraFilePath = file.absolutePath

                        val uri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )

                        cameraPhotoUri = uri
                        takePictureLauncher.launch(uri)
                    }

                    else -> { // galerija
                        pickImageFromGallery()
                    }
                }
            }


            AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .show()
        }


        avatarImage.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val size = view.width.coerceAtMost(view.height)
                outline.setOval(0, 0, size, size)
            }
        }
        avatarImage.clipToOutline = true


        // Spinner za sortiranje
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sort_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sortSpinner.adapter = adapter
        }

        // Listeneri za sortiranje
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                refreshPokemonList()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        checkboxAlive.setOnCheckedChangeListener { _, _ ->
            refreshPokemonList()
        }

        btnSortToggle.setOnClickListener {
            isAscending = !isAscending
            refreshPokemonList()
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

    private fun populateProfileUI(user: AppUser) {
        usernameText.text = user.username
        levelText.text = "Level: ${user.level}"

        val xpNeeded = user.level * 100
        val xpPercent = ((user.xp.toFloat() / xpNeeded) * 100).coerceIn(0f, 100f).toInt()
        xpText.text = "XP: $xpPercent%"
        xpProgress.progress = xpPercent
        winsText.text = "Wins: ${user.wins}"
        loseText.text = "Loses: ${user.loses}"

        val avatarPath = user.localAvatarPath

        when {
            !avatarPath.isNullOrEmpty() -> {
                if (avatarPath.startsWith("drawable://")) {
                    val resId = avatarPath.removePrefix("drawable://").toInt()
                    avatarImage.setImageResource(resId)
                } else {
                    val bitmap = BitmapFactory.decodeFile(avatarPath)
                    avatarImage.setImageBitmap(bitmap)
                }
            }
            else -> {
                avatarImage.setImageResource(R.drawable.ic_pokemon_placeholder)
            }
        }


        userViewModel.fetchUserPokemons(user.uid) { pokemons ->
            currentPokemons = pokemons
            refreshPokemonList()
        }
    }

    private fun refreshPokemonList() {
        if (currentPokemons.isEmpty()) return

        // filtriranje po "Alive"
        var filtered = if (checkboxAlive.isChecked) {
            currentPokemons.filter { it.alive  }
        } else {
            currentPokemons
        }

        // sortiranje po spinner-u
        val selectedSort = sortSpinner.selectedItem.toString()
        filtered = when (selectedSort) {
            "Level" -> filtered.sortedBy { it.level }
            "HP" -> filtered.sortedBy { it.currenthp }
            "Name" -> filtered.sortedBy { it.name.lowercase() }
            else -> filtered
        }

        // invertovanje ako descending
        if (!isAscending) {
            filtered = filtered.reversed()
        }

        adapter.submitList(null)
        adapter.submitList(filtered.map { it.copy() })

        adapter.notifyDataSetChanged()
    }

    // --- Lokalno čuvanje galerije ---
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            avatarImage.setImageURI(it)
            saveAvatarLocally(it)?.let { path ->
                userViewModel.updateAvatarPath(path) // update u viewmodelu
            }
        }
    }

    private fun pickImageFromGallery() { pickImageLauncher.launch("image/*") }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraPhotoUri != null && cameraFilePath != null) {
            avatarImage.setImageURI(cameraPhotoUri)
            userViewModel.updateAvatarPath(cameraFilePath!!)
        }
    }

    private fun saveAvatarLocally(uri: Uri): String? {
        val context = requireContext()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val avatarDir = File(context.filesDir, "avatars")
            if (!avatarDir.exists()) avatarDir.mkdir()

            val userId = userViewModel.currentUser.value?.uid ?: "default"
            val file = File(avatarDir, "avatar_$userId.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun onResume() {
        super.onResume()
    }


}