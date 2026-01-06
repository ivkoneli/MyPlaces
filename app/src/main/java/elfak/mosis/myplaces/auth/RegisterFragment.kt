package elfak.mosis.myplaces.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import elfak.mosis.myplaces.MainActivity
import elfak.mosis.myplaces.R
import elfak.mosis.myplaces.model.UserViewModel

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val authRepo = AuthRepository()
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val username = view.findViewById<EditText>(R.id.usernameInput)
        val email = view.findViewById<EditText>(R.id.emailInput)
        val pass = view.findViewById<EditText>(R.id.passwordInput)
        val registerBtn = view.findViewById<Button>(R.id.registerBtn)

        // Call AuthRepo register with data from the fragment
        registerBtn.setOnClickListener {
            // On success update the view model and navigate to home , onErr display error msg
            authRepo.register(
                username.text.toString(),
                email.text.toString(),
                pass.text.toString(),
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
}
