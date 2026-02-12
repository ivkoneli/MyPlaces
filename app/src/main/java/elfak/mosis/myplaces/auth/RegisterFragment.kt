package elfak.mosis.myplaces.auth

import AvatarAdapter
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import elfak.mosis.myplaces.MainActivity
import elfak.mosis.myplaces.R
import elfak.mosis.myplaces.model.UserViewModel
import java.io.File
import java.io.FileOutputStream

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val authRepo = AuthRepository()
    private val userViewModel: UserViewModel by activityViewModels()

    private var selectedAvatarDrawable: Int? = null
    private var selectedAvatarUri: Uri? = null
    private var cameraFilePath: String? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val username = view.findViewById<EditText>(R.id.usernameInput)
        val email = view.findViewById<EditText>(R.id.emailInput)
        val pass = view.findViewById<EditText>(R.id.passwordInput)
        val registerBtn = view.findViewById<Button>(R.id.registerBtn)

        val avatarImage = view.findViewById<ImageView>(R.id.avatarImage)
        val changeAvatarText = view.findViewById<TextView>(R.id.changeAvatarText)
        val spinner = view.findViewById<Spinner>(R.id.countryCodeSpinner)
        val phoneInput = view.findViewById<EditText>(R.id.phoneInput)

        val countryCodes = listOf(
            "🇷🇸 +381",
            "🇭🇷 +385",
            "🇧🇦 +387",
            "🇲🇪 +382",
            "🇩🇪 +49",
            "🇺🇸 +1"
        )

        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            countryCodes
        )

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

        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val savedPath = saveAvatarLocally(it)
                if (savedPath != null) {
                    selectedAvatarUri = Uri.parse(savedPath)
                    selectedAvatarDrawable = null
                    avatarImage.setImageURI(Uri.parse(savedPath))
                }
            }
        }

        fun pickImageFromGallery() {
            pickImageLauncher.launch("image/*")
        }

        var cameraPhotoUri: Uri? = null

        val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraFilePath != null && cameraPhotoUri != null) {
                selectedAvatarUri = Uri.parse(cameraFilePath)
                selectedAvatarDrawable = null
                avatarImage.setImageURI(cameraPhotoUri) // samo za prikaz
            }

        }

        fun createTempImageUri(): Uri {
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
            return uri
        }



        fun showAvatarDialog() {
            val dialogView = layoutInflater.inflate(R.layout.dialog_avatar_picker, null)
            val gridView = dialogView.findViewById<GridView>(R.id.gridAvatars)

            gridView.adapter = AvatarAdapter(requireContext(), avatarList) { selected, isCamera ->
                when {
                    selected != null -> { // drawable
                        selectedAvatarDrawable = selected
                        selectedAvatarUri = null
                        avatarImage.setImageResource(selected)
                    }
                    isCamera -> { // kamera
                        val uri = createTempImageUri()
                        takePictureLauncher.launch(uri)
                    }
                    else -> { // galerija
                        pickImageFromGallery()
                    }
                }
            }


            AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .show()
        }

        avatarImage.setOnClickListener { showAvatarDialog() }
        changeAvatarText.setOnClickListener { showAvatarDialog() }


        // Call AuthRepo register with data from the fragment
        registerBtn.setOnClickListener {

            val fullPhone = spinner.selectedItem.toString().split(" ")[1] +
                    phoneInput.text.toString()

            authRepo.register(
                username.text.toString(),
                email.text.toString(),
                pass.text.toString(),
                fullPhone,
                selectedAvatarDrawable,
                selectedAvatarUri,
                onSuccess = { appUser ->
                    userViewModel.currentUser.value = appUser
                    findNavController().navigate(R.id.action_register_to_home)
                },
                onError = { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            )
        }



        // Navigate back to Login screen
        view.findViewById<TextView>(R.id.goLogin).setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }


    private fun saveAvatarLocally(uri: Uri): String? {
        val context = requireContext()
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val avatarDir = File(context.filesDir, "avatars")
            if (!avatarDir.exists()) avatarDir.mkdir()

            val fileName = "avatar_${System.currentTimeMillis()}.png"
            val file = File(avatarDir, fileName)

            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



}
